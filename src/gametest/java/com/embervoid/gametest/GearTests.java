package com.embervoid.gametest;

import com.embervoid.Embervoid;
import com.embervoid.reach.gear.Bleeding;
import com.embervoid.reach.gear.GearArmor;
import com.embervoid.reach.gear.GearConfig;
import com.embervoid.reach.gear.GearItems;
import com.embervoid.reach.gear.GearLootTables;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;

/** Ashen Reach gear (PLAN milestone 11): recipes, loot tables and item behaviour. */
public final class GearTests implements FabricGameTest {
	/** Fresh players cannot be hurt for their first 60 ticks. */
	static final int JOIN_INVULNERABLE_TICKS = 65;

	/** The fixed chest table ids load (other tracks' structures point at them) and the 11.1 recipes exist. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void materialRecipesAndChestTablesLoad(TestContext context) {
		MinecraftServer server = context.getWorld().getServer();
		for (Identifier id : GearLootTables.all()) {
			context.assertTrue(server.getLootManager().getLootTable(id) != LootTable.EMPTY, "missing chest table " + id);
		}
		assertRecipe(context, "smoldering_ingot", GearItems.SMOLDERING_INGOT);
		assertRecipe(context, "ash_of_rematch", GearItems.ASH_OF_REMATCH);
		assertRecipe(context, "grave_iron_ingot_from_smelting_raw_grave_iron", GearItems.GRAVE_IRON_INGOT);
		assertRecipe(context, "grave_iron_ingot_from_blasting_raw_grave_iron", GearItems.GRAVE_IRON_INGOT);
		context.complete();
	}

	/** Three hits make Bleeding III; it deals 1.5 per second straight through full diamond armor. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void bleedingStacksAndIgnoresArmor(TestContext context) {
		HuskEntity husk = context.spawnMob(EntityType.HUSK, 2, 2, 2);
		husk.setAiDisabled(true);
		for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			husk.equipStack(slot, new ItemStack(switch (slot) {
				case HEAD -> Items.DIAMOND_HELMET;
				case CHEST -> Items.DIAMOND_CHESTPLATE;
				case LEGS -> Items.DIAMOND_LEGGINGS;
				default -> Items.DIAMOND_BOOTS;
			}));
		}
		for (int hit = 0; hit < 4; hit++) {
			context.assertTrue(Bleeding.apply(husk, 1, null), "husk did not bleed");
		}
		context.assertTrue(Bleeding.level(husk) == 3, "expected Bleeding III, got " + Bleeding.level(husk));
		float start = husk.getHealth();
		// 40 ticks hold exactly two damage ticks, whatever the husk's age.
		context.runAtTick(context.getTick() + 40, () -> {
			float lost = start - husk.getHealth();
			context.assertTrue(Math.abs(lost - 3.0f) < 0.01f, "expected 3.0 bleed damage through armor, got " + lost);
			context.complete();
		});
	}

	/** Every new stack refreshes the duration; immune entity types (tag {@code embervoid:bleed_immune}) never bleed. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void bleedingRefreshesAndRespectsImmunity(TestContext context) {
		HuskEntity husk = context.spawnMob(EntityType.HUSK, 2, 2, 2);
		husk.setAiDisabled(true);
		Bleeding.apply(husk, 1, null);
		int full = GearConfig.get().bleeding().durationTicks();
		context.runAtTick(context.getTick() + 30, () -> {
			context.assertTrue(husk.getStatusEffect(Bleeding.BLEEDING).getDuration() < full, "duration did not run down");
			Bleeding.apply(husk, 1, null);
			context.assertTrue(husk.getStatusEffect(Bleeding.BLEEDING).getDuration() == full, "second hit did not refresh");
			context.assertTrue(Bleeding.level(husk) == 2, "second hit did not raise the level");

			for (RegistryEntry<EntityType<?>> type : Registries.ENTITY_TYPE.iterateEntries(Bleeding.IMMUNE)) {
				if (type.value().create(context.getWorld()) instanceof LivingEntity immune) {
					context.assertFalse(Bleeding.apply(immune, 1, null), type.value() + " is tagged immune but bled");
				}
			}
			context.complete();
		});
	}

	/** Smithing a diamond piece with the template and an ingot gives the Smoldering piece, enchantments kept. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void smolderingSmithingKeepsEnchantments(TestContext context) {
		assertRecipe(context, "smoldering_template", GearItems.SMOLDERING_TEMPLATE);
		ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
		helmet.addEnchantment(Enchantments.PROTECTION, 3);
		SimpleInventory table = new SimpleInventory(new ItemStack(GearItems.SMOLDERING_TEMPLATE), helmet,
				new ItemStack(GearItems.SMOLDERING_INGOT));
		ServerWorld world = context.getWorld();
		ItemStack result = world.getRecipeManager().getFirstMatch(RecipeType.SMITHING, table, world)
				.map(recipe -> recipe.craft(table, world.getRegistryManager())).orElse(ItemStack.EMPTY);
		context.assertTrue(result.isOf(GearArmor.SMOLDERING_PLATE.get(0)), "smithing gave " + result);
		context.assertTrue(EnchantmentHelper.getLevel(Enchantments.PROTECTION, result) == 3, "enchantment lost");
		context.complete();
	}

	/** Each piece takes 8 % off fire damage; the full set stops burning ticks but not lava. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void smolderingPlateResistsFire(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		for (int i = 0; i < 3; i++) {
			player.equipStack(GearArmor.SLOTS.get(i), new ItemStack(GearArmor.SMOLDERING_PLATE.get(i)));
		}
		// Fresh players cannot be hurt for their first 60 ticks (join invulnerability).
		context.waitAndRun(JOIN_INVULNERABLE_TICKS, () -> withPlayers(context, () -> {
			player.damage(player.getDamageSources().onFire(), 10);
			float lost = player.getMaxHealth() - player.getHealth();
			context.assertTrue(Math.abs(lost - 7.6f) < 0.01f, "3 pieces: expected 7.6 burning damage, got " + lost);
			player.setHealth(player.getMaxHealth());
			player.equipStack(GearArmor.SLOTS.get(3), new ItemStack(GearArmor.SMOLDERING_PLATE.get(3)));
		}, player));
		// Past the hurt cooldown of the first hit.
		context.waitAndRun(JOIN_INVULNERABLE_TICKS + 20, () -> withPlayers(context, () -> {
			player.damage(player.getDamageSources().onFire(), 10);
			context.assertTrue(player.getHealth() == player.getMaxHealth(), "full set still burns: " + player.getHealth());
			player.damage(player.getDamageSources().lava(), 4);
			context.assertTrue(player.getHealth() < player.getMaxHealth(), "full set made lava harmless");
			TestPlayers.complete(context, player);
		}, player));
	}

	/** Runs a test step; if it fails, the players leave the server first so they do not leak into other tests. */
	static void withPlayers(TestContext context, Runnable step, ServerPlayerEntity... players) {
		try {
			step.run();
		} catch (RuntimeException e) {
			for (ServerPlayerEntity player : players) {
				if (player.getServer().getPlayerManager().getPlayer(player.getUuid()) == player) {
					player.getServer().getPlayerManager().remove(player);
				}
			}
			throw e;
		}
	}

	static void assertRecipe(TestContext context, String name, Item output) {
		MinecraftServer server = context.getWorld().getServer();
		Recipe<?> recipe = server.getRecipeManager().get(Embervoid.id(name)).orElse(null);
		context.assertTrue(recipe != null, "missing recipe " + name);
		context.assertTrue(recipe.getOutput(server.getRegistryManager()).isOf(output), "recipe " + name + " makes the wrong item");
	}
}
