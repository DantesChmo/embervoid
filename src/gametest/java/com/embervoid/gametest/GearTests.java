package com.embervoid.gametest;

import com.embervoid.Embervoid;
import com.embervoid.item.HealingFlaskItem;
import com.embervoid.reach.gear.AshBombEntity;
import com.embervoid.reach.gear.Bleeding;
import com.embervoid.reach.gear.GearAbilities;
import com.embervoid.reach.gear.GearArmor;
import com.embervoid.reach.gear.GearConfig;
import com.embervoid.reach.gear.GearConsumables;
import com.embervoid.reach.gear.GearDamage;
import com.embervoid.reach.gear.GearItems;
import com.embervoid.reach.gear.GearLootTables;
import com.embervoid.reach.gear.GearReach;
import com.embervoid.reach.gear.GearWeapons;
import com.embervoid.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

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

	/**
	 * Set bonuses follow the equipment: full Reaper's Garb +10 % speed, full Necropolis Warden +0.3 knockback
	 * resistance and −1.25 % speed per piece, and the bonus leaves with a piece. Huntsman's Leathers: +15 % projectile
	 * damage from the wearer; the Warden set takes 15 % less while blocking.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void setBonusesFollowEquipment(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		double baseSpeed = player.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
		withPlayers(context, () -> {
			equip(player, GearArmor.REAPERS_GARB);
			// Test players have no connection to tick them; a real player's tick notices equipment changes.
			player.playerTick();
			double speed = player.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
			context.assertTrue(Math.abs(speed - baseSpeed * 1.1) < 1e-6, "Reaper set speed " + speed + ", base " + baseSpeed);
			player.equipStack(EquipmentSlot.FEET, ItemStack.EMPTY);
			player.playerTick();
			speed = player.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
			context.assertTrue(Math.abs(speed - baseSpeed) < 1e-6, "speed bonus stayed without the boots: " + speed);

			equip(player, GearArmor.NECROPOLIS_WARDEN);
			player.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.SHIELD));
			player.setCurrentHand(Hand.OFF_HAND);
			// A shield blocks after 5 ticks of use.
			for (int tick = 0; tick < 6; tick++) {
				player.playerTick();
			}
			speed = player.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
			context.assertTrue(Math.abs(speed - baseSpeed * 0.95) < 1e-6, "Warden set speed " + speed);
			double knockback = player.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
			context.assertTrue(Math.abs(knockback - 0.3) < 1e-6, "Warden set knockback resistance " + knockback);
			context.assertTrue(player.isBlocking(), "player is not blocking with the shield");
			float blocked = GearDamage.modifyIncoming(player, player.getDamageSources().generic(), 10);
			context.assertTrue(Math.abs(blocked - 8.5f) < 1e-4, "Warden set while blocking: " + blocked);

			HuskEntity husk = context.spawnMob(EntityType.HUSK, 4, 2, 4);
			husk.setAiDisabled(true);
			ArrowEntity arrow = new ArrowEntity(context.getWorld(), player);
			float plain = GearDamage.modifyIncoming(husk, context.getWorld().getDamageSources().arrow(arrow, player), 10);
			context.assertTrue(plain == 10, "arrow damage changed without the Huntsman set: " + plain);
			equip(player, GearArmor.HUNTSMANS_LEATHERS);
			float boosted = GearDamage.modifyIncoming(husk, context.getWorld().getDamageSources().arrow(arrow, player), 10);
			context.assertTrue(Math.abs(boosted - 11.5f) < 1e-4, "Huntsman set arrow damage: " + boosted);
		}, player);
		TestPlayers.complete(context, player);
	}

	/**
	 * Scarecrow's Mask halves the distance mobs notice the wearer at; Drowned Sabatons give Depth Strider III; Huntsman's
	 * Greaves take 4 off fall damage (nothing up to 7 blocks) and give Speed after a kill.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void uniquePiecesWork(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		context.assertTrue(player.getAttackDistanceScalingFactor(null) == 1.0, "scaling without the mask");
		player.equipStack(EquipmentSlot.HEAD, new ItemStack(GearArmor.SCARECROW_MASK));
		context.assertTrue(player.getAttackDistanceScalingFactor(null) == 0.5, "mask: " + player.getAttackDistanceScalingFactor(null));
		context.assertTrue(GearArmor.hiddenFromAmbush(player), "mask does not hide from ambushes");
		player.equipStack(EquipmentSlot.FEET, new ItemStack(GearArmor.DROWNED_SABATONS));
		context.assertTrue(EnchantmentHelper.getDepthStrider(player) == 3, "sabatons depth strider " + EnchantmentHelper.getDepthStrider(player));
		player.equipStack(EquipmentSlot.LEGS, new ItemStack(GearArmor.HUNTSMAN_GREAVES_ITEM));

		context.waitAndRun(JOIN_INVULNERABLE_TICKS, () -> withPlayers(context, () -> {
			player.damage(player.getDamageSources().fall(), 4);
			context.assertTrue(player.getHealth() == player.getMaxHealth(), "a 7-block fall hurt: " + player.getHealth());
			player.damage(player.getDamageSources().fall(), 7);
			float lost = player.getMaxHealth() - player.getHealth();
			// Leggings armor applies on top: the fall loses 4, then 5 armor points.
			context.assertTrue(lost > 0 && lost < 3.01f, "a 10-block fall: lost " + lost + ", expected at most 3");

			HuskEntity husk = context.spawnMob(EntityType.HUSK, 4, 2, 4);
			husk.damage(context.getWorld().getDamageSources().playerAttack(player), 10000);
			context.assertTrue(player.hasStatusEffect(StatusEffects.SPEED), "no speed after a kill");
			TestPlayers.complete(context, player);
		}, player));
	}

	/** Furnace Heart Cuirass: dropping below 30 % health sets off a fire nova that hurts mobs around, once per cooldown. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void furnaceHeartNova(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		player.equipStack(EquipmentSlot.CHEST, new ItemStack(GearArmor.FURNACE_HEART_CUIRASS));
		HuskEntity husk = context.spawnMob(EntityType.HUSK, 4, 2, 2);
		husk.setAiDisabled(true);
		context.waitAndRun(JOIN_INVULNERABLE_TICKS, () -> withPlayers(context, () -> {
			player.damage(player.getDamageSources().magic(), 4);
			context.assertTrue(husk.getHealth() == husk.getMaxHealth(), "nova went off above 30 % health");
		}, player));
		// Past the hurt cooldown of the first hit.
		context.waitAndRun(JOIN_INVULNERABLE_TICKS + 20, () -> withPlayers(context, () -> {
			player.damage(player.getDamageSources().magic(), 11);
			context.assertTrue(husk.getHealth() < husk.getMaxHealth(), "no nova below 30 % health");
			context.assertTrue(player.getItemCooldownManager().isCoolingDown(GearArmor.FURNACE_HEART_CUIRASS), "no cooldown");
			TestPlayers.complete(context, player);
		}, player));
	}

	/** Weapon recipes exist and the weapons carry their DESIGN damage and speed. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void weaponRecipesAndNumbers(TestContext context) {
		assertRecipe(context, "ashen_scythe", GearWeapons.ASHEN_SCYTHE);
		assertRecipe(context, "wardens_scourge", GearWeapons.WARDENS_SCOURGE);
		assertRecipe(context, "royal_ash_rapier", GearWeapons.ROYAL_ASH_RAPIER);
		float[][] numbers = {{8, 1.1f}, {8, 0.9f}, {6, 2.0f}};
		for (int i = 0; i < GearWeapons.MELEE.size(); i++) {
			ItemStack stack = new ItemStack(GearWeapons.MELEE.get(i));
			double damage = 1 + modifier(stack, EntityAttributes.GENERIC_ATTACK_DAMAGE);
			double speed = 4 + modifier(stack, EntityAttributes.GENERIC_ATTACK_SPEED);
			context.assertTrue(Math.abs(damage - numbers[i][0]) < 1e-4 && Math.abs(speed - numbers[i][1]) < 1e-4,
					stack.getItem() + ": damage " + damage + ", speed " + speed);
		}
		context.complete();
	}

	/**
	 * The Scourge hits a mob 5 blocks away on a swing (server raycast) and makes it bleed; the Rapier refuses a target
	 * beyond 2.5 blocks.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 100)
	public void weaponReach(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		withPlayers(context, () -> {
			HuskEntity far = husk(context, 2, 7);
			HuskEntity mid = husk(context, 4, 6);
			HuskEntity near = husk(context, 0, 4);
			hold(player, GearWeapons.WARDENS_SCOURGE);
			GearReach.onSwing(player, Hand.MAIN_HAND);
			context.assertTrue(far.getHealth() < far.getMaxHealth(), "the Scourge did not reach 5 blocks");
			context.assertTrue(Bleeding.level(far) == 1, "the Scourge's hit did not bleed");

			hold(player, GearWeapons.ROYAL_ASH_RAPIER);
			context.assertTrue(GearReach.beforeAttackForTest(player, mid) == ActionResult.FAIL, "the Rapier hit beyond 2.5 blocks");
			context.assertTrue(GearReach.beforeAttackForTest(player, near) == ActionResult.PASS, "the Rapier missed at 2 blocks");
			player.attack(near);
			context.assertTrue(near.isOnFire() && Bleeding.level(near) == 1, "the Rapier's hit did not burn and bleed");
		}, player);
		TestPlayers.complete(context, player);
	}

	/** A fully charged Scythe hit sweeps the mob beside the target and sends an ash wave to a mob further ahead. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 100)
	public void scytheSweepAndAshWave(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		withPlayers(context, () -> {
			HuskEntity target = husk(context, 2, 4);
			HuskEntity beside = husk(context, 4, 5);
			HuskEntity ahead = husk(context, 2, 7);
			hold(player, GearWeapons.ASHEN_SCYTHE);
			for (int tick = 0; tick < 30; tick++) {
				player.playerTick();
			}
			GearReach.beforeAttackForTest(player, target);
			player.attack(target);
			context.assertTrue(target.getHealth() < target.getMaxHealth(), "the target was not hit");
			context.assertTrue(beside.getHealth() < beside.getMaxHealth(), "the sweep missed the mob beside the target");
			context.assertTrue(ahead.getHealth() < ahead.getMaxHealth(), "the ash wave missed the mob 5 blocks ahead");
		}, player);
		TestPlayers.complete(context, player);
	}

	/** Harvest hits all around, Lash pulls a mob 5 blocks away closer, Lunge dashes and bleeds its target twice. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 100)
	public void weaponSpecialAttacks(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		withPlayers(context, () -> {
			ServerWorld world = context.getWorld();
			HuskEntity left = husk(context, 0, 2);
			HuskEntity behind = husk(context, 2, 0);
			hold(player, GearWeapons.ASHEN_SCYTHE);
			GearAbilities.HARVEST.perform(world, player, player.getMainHandStack());
			context.assertTrue(left.getHealth() < left.getMaxHealth() && behind.getHealth() < behind.getMaxHealth(),
					"Harvest missed a mob beside or behind the player");

			HuskEntity far = husk(context, 2, 7);
			hold(player, GearWeapons.WARDENS_SCOURGE);
			GearAbilities.LASH.perform(world, player, player.getMainHandStack());
			context.assertTrue(far.getVelocity().z < -0.5, "Lash did not pull: velocity " + far.getVelocity());

			hold(player, GearWeapons.ROYAL_ASH_RAPIER);
			GearAbilities.LUNGE.perform(world, player, player.getMainHandStack());
			context.assertTrue(player.getVelocity().z > 1, "Lunge did not dash: velocity " + player.getVelocity());
			context.assertTrue(Bleeding.level(far) == 2 && far.isOnFire(), "Lunge: bleeding " + Bleeding.level(far));
		}, player);
		TestPlayers.complete(context, player);
	}

	/** Huntsman's Crossbow: 2 s charge, and its bolts deal 1.5× with Punch III and pierce one mob. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 100)
	public void huntsmansCrossbowBolts(TestContext context) {
		assertRecipe(context, "huntsmans_crossbow", GearWeapons.HUNTSMANS_CROSSBOW);
		ItemStack crossbow = new ItemStack(GearWeapons.HUNTSMANS_CROSSBOW);
		context.assertTrue(CrossbowItem.getPullTime(crossbow) == 40, "charge " + CrossbowItem.getPullTime(crossbow));
		context.assertTrue(CrossbowItem.getPullTime(new ItemStack(Items.CROSSBOW)) == 25, "vanilla crossbow charge changed");

		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		withPlayers(context, () -> {
			NbtList projectiles = new NbtList();
			projectiles.add(new ItemStack(Items.ARROW).writeNbt(new NbtCompound()));
			crossbow.getOrCreateNbt().put("ChargedProjectiles", projectiles);
			CrossbowItem.setCharged(crossbow, true);
			player.setStackInHand(Hand.MAIN_HAND, crossbow);
			CrossbowItem.shootAll(context.getWorld(), player, Hand.MAIN_HAND, crossbow, 3.15f, 0);
			List<PersistentProjectileEntity> bolts = context.getWorld().getEntitiesByClass(PersistentProjectileEntity.class,
					player.getBoundingBox().expand(4), bolt -> bolt.getOwner() == player);
			context.assertTrue(bolts.size() == 1, "expected one bolt, got " + bolts.size());
			PersistentProjectileEntity bolt = bolts.get(0);
			context.assertTrue(Math.abs(bolt.getDamage() - 3.0) < 1e-6, "bolt damage " + bolt.getDamage() + ", expected 3.0");
			context.assertTrue(bolt.getPunch() == 3 && bolt.getPierceLevel() == 1,
					"punch " + bolt.getPunch() + ", pierce " + bolt.getPierceLevel());
			bolt.discard();
		}, player);
		TestPlayers.complete(context, player);
	}

	/** The Ash Bomb's burst burns and scorches mobs within 2 blocks, not further. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 100)
	public void ashBombBurst(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		withPlayers(context, () -> {
			HuskEntity near = husk(context, 5, 5);
			HuskEntity far = husk(context, 5, 9);
			AshBombEntity bomb = new AshBombEntity(context.getWorld(), player);
			Vec3d at = near.getPos().add(0.5, 0, 0);
			bomb.setPosition(at.x, at.y, at.z);
			bomb.burst();
			context.assertTrue(near.getHealth() < near.getMaxHealth() && near.isOnFire(), "the bomb missed the mob beside it");
			context.assertTrue(far.getHealth() == far.getMaxHealth() && !far.isOnFire(), "the bomb hit a mob 4 blocks away");
		}, player);
		TestPlayers.complete(context, player);
	}

	/**
	 * Resin makes a weapon's hits ignite; Amber Balm cures Bleeding and Poison; oil lights a lantern in the offhand
	 * (Night Vision); a Whetstone repairs a quarter of a realm weapon; Cinder Shards burn like two coal.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 100)
	public void consumablesWork(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		ServerWorld world = context.getWorld();
		withPlayers(context, () -> {
			player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			player.setStackInHand(Hand.OFF_HAND, new ItemStack(GearConsumables.SMOLDERING_RESIN));
			player.getOffHandStack().use(world, player, Hand.OFF_HAND);
			context.assertTrue(player.getOffHandStack().isEmpty(), "resin not used up");
			HuskEntity husk = husk(context, 2, 4);
			player.attack(husk);
			context.assertTrue(husk.isOnFire(), "a resin-coated sword did not ignite");

			Bleeding.apply(player, 2, null);
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 200));
			ItemStack balm = new ItemStack(GearConsumables.AMBER_BALM);
			ItemStack left = balm.finishUsing(world, player);
			context.assertTrue(!player.hasStatusEffect(Bleeding.BLEEDING) && !player.hasStatusEffect(StatusEffects.POISON)
					&& player.hasStatusEffect(StatusEffects.REGENERATION), "balm did not cure and regenerate");
			context.assertTrue(left.isOf(Items.GLASS_BOTTLE), "balm left " + left);

			player.setStackInHand(Hand.OFF_HAND, new ItemStack(ModBlocks.EMBER_LANTERN));
			player.setStackInHand(Hand.MAIN_HAND, new ItemStack(GearConsumables.AMBER_LANTERN_OIL));
			player.getMainHandStack().use(world, player, Hand.MAIN_HAND);
			context.assertTrue(GearConsumables.carriesLitLantern(player), "the lantern did not light");
			GearConsumables.tickLanternForTest(player);
			context.assertTrue(player.hasStatusEffect(StatusEffects.NIGHT_VISION), "a lit lantern gave no night vision");

			ItemStack scythe = new ItemStack(GearWeapons.ASHEN_SCYTHE);
			scythe.setDamage(1000);
			player.setStackInHand(Hand.MAIN_HAND, scythe);
			player.setStackInHand(Hand.OFF_HAND, new ItemStack(GearConsumables.WHETSTONE));
			player.getOffHandStack().use(world, player, Hand.OFF_HAND);
			context.assertTrue(scythe.getDamage() == 550, "whetstone left damage " + scythe.getDamage() + ", expected 550");

			Integer fuel = FuelRegistry.INSTANCE.get(GearItems.CINDER_SHARD);
			context.assertTrue(fuel != null && fuel == 3200, "cinder fuel " + fuel);
			assertRecipe(context, "ash_bomb", GearConsumables.ASH_BOMB);
			assertRecipe(context, "whetstone", GearConsumables.WHETSTONE);
		}, player);
		TestPlayers.complete(context, player);
	}

	/** The Flask Ember raises flask healing by 15 % once; a second one is refused. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 100)
	public void flaskEmberOnce(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		withPlayers(context, () -> {
			float before = HealingFlaskItem.healAmount(player);
			player.setStackInHand(Hand.MAIN_HAND, new ItemStack(GearItems.FLASK_EMBER, 2));
			player.getMainHandStack().use(context.getWorld(), player, Hand.MAIN_HAND);
			float after = HealingFlaskItem.healAmount(player);
			context.assertTrue(Math.abs(after - before * 1.15f) < 1e-4, "heal " + before + " -> " + after);
			context.assertTrue(player.getMainHandStack().getCount() == 1, "the ember was not used up");
			player.getMainHandStack().use(context.getWorld(), player, Hand.MAIN_HAND);
			context.assertTrue(player.getMainHandStack().getCount() == 1, "a second ember was used");
			context.assertTrue(Math.abs(HealingFlaskItem.healAmount(player) - after) < 1e-4, "a second ember changed healing");
		}, player);
		TestPlayers.complete(context, player);
	}

	/**
	 * Chest tables: the arena chest always holds a template and an Ash of Rematch; a treasure chest holds its zone's
	 * weapon about 4 % of the time; a common chest holds its zone's material.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void chestLoot(TestContext context) {
		ServerWorld world = context.getWorld();
		LootContextParameterSet chest = new LootContextParameterSet.Builder(world)
				.add(LootContextParameters.ORIGIN, context.getAbsolute(new Vec3d(1, 1, 1)))
				.build(LootContextTypes.CHEST);
		Map<String, Item> weapons = Map.of("fields", GearWeapons.ASHEN_SCYTHE, "woods", GearWeapons.HUNTSMANS_CROSSBOW,
				"necropolis", GearWeapons.WARDENS_SCOURGE);
		Map<String, Item> materials = Map.of("fields", GearItems.CINDER_SHARD, "woods", GearItems.ROYAL_AMBER,
				"necropolis", GearItems.RAW_GRAVE_IRON);
		for (String zone : GearLootTables.ZONES) {
			List<ItemStack> arena = world.getServer().getLootManager().getLootTable(GearLootTables.arena(zone)).generateLoot(chest);
			context.assertTrue(arena.stream().anyMatch(stack -> stack.isOf(GearItems.SMOLDERING_TEMPLATE))
					&& arena.stream().anyMatch(stack -> stack.isOf(GearItems.ASH_OF_REMATCH)), zone + " arena chest: " + arena);
			int weapon = count(world, GearLootTables.treasure(zone), chest, weapons.get(zone), 3000);
			context.assertTrue(weapon >= 80 && weapon <= 170, zone + " treasure: weapon in " + weapon + " of 3000, expected ~120");
			context.assertTrue(count(world, GearLootTables.common(zone), chest, materials.get(zone), 100) > 0,
					zone + " common chests never hold " + materials.get(zone));
		}
		int rapier = count(world, GearLootTables.CITADEL_TREASURE, chest, GearWeapons.ROYAL_ASH_RAPIER, 3000);
		context.assertTrue(rapier >= 80 && rapier <= 170, "Citadel treasure: Rapier in " + rapier + " of 3000");
		context.complete();
	}

	/**
	 * Realm loot on mobs: a Grave Warden killed by a player drops a Necropolis Warden piece ~15 % and the Drowned
	 * Sabatons ~4 % of the time, and neither without a player; a Cinder Husk drops a Reaper's Garb piece ~3 %.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void mobRealmLoot(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		withPlayers(context, () -> {
			ServerWorld world = context.getWorld();
			LivingEntity warden = (LivingEntity) Registries.ENTITY_TYPE.get(Embervoid.id("grave_warden")).create(world);
			LivingEntity husk = (LivingEntity) Registries.ENTITY_TYPE.get(Embervoid.id("cinder_husk")).create(world);
			context.assertTrue(warden != null && husk != null, "realm mobs are not registered");
			int set = 0;
			int sabatons = 0;
			int reaper = 0;
			int unearned = 0;
			for (int kill = 0; kill < 3000; kill++) {
				for (ItemStack stack : roll(world, warden, player)) {
					set += GearArmor.NECROPOLIS_WARDEN.contains(stack.getItem()) ? 1 : 0;
					sabatons += stack.isOf(GearArmor.DROWNED_SABATONS) ? 1 : 0;
				}
				for (ItemStack stack : roll(world, husk, player)) {
					reaper += GearArmor.REAPERS_GARB.contains(stack.getItem()) ? 1 : 0;
				}
				for (ItemStack stack : roll(world, warden, null)) {
					unearned += GearArmor.NECROPOLIS_WARDEN.contains(stack.getItem()) || stack.isOf(GearArmor.DROWNED_SABATONS) ? 1 : 0;
				}
			}
			context.assertTrue(set >= 370 && set <= 530, "Grave Warden set pieces " + set + " of 3000, expected ~450");
			context.assertTrue(sabatons >= 75 && sabatons <= 170, "Grave Warden sabatons " + sabatons + " of 3000, expected ~120");
			context.assertTrue(reaper >= 55 && reaper <= 130, "Cinder Husk Reaper pieces " + reaper + " of 3000, expected ~90");
			context.assertTrue(unearned == 0, unearned + " set pieces without a player kill");
		}, player);
		TestPlayers.complete(context, player);
	}

	static int count(ServerWorld world, Identifier table, LootContextParameterSet params, Item item, int rolls) {
		LootTable loot = world.getServer().getLootManager().getLootTable(table);
		int found = 0;
		for (int i = 0; i < rolls; i++) {
			for (ItemStack stack : loot.generateLoot(params)) {
				found += stack.isOf(item) ? 1 : 0;
			}
		}
		return found;
	}

	/** One roll of a mob's loot table, killed by {@code player} (or by nothing). */
	static List<ItemStack> roll(ServerWorld world, LivingEntity mob, @Nullable ServerPlayerEntity player) {
		LootContextParameterSet.Builder params = new LootContextParameterSet.Builder(world)
				.add(LootContextParameters.THIS_ENTITY, mob)
				.add(LootContextParameters.ORIGIN, player == null ? Vec3d.ZERO : player.getPos())
				.add(LootContextParameters.DAMAGE_SOURCE, player == null ? world.getDamageSources().generic()
						: world.getDamageSources().playerAttack(player));
		if (player != null) {
			params.add(LootContextParameters.KILLER_ENTITY, player)
					.add(LootContextParameters.DIRECT_KILLER_ENTITY, player)
					.add(LootContextParameters.LAST_DAMAGE_PLAYER, player);
		}
		return world.getServer().getLootManager().getLootTable(mob.getLootTable()).generateLoot(params.build(LootContextTypes.ENTITY));
	}

	/** A husk with no AI standing on a stone block at relative {@code (x, 1, z)}. */
	static HuskEntity husk(TestContext context, int x, int z) {
		context.setBlockState(x, 0, z, Blocks.STONE);
		HuskEntity husk = context.spawnMob(EntityType.HUSK, x, 1, z);
		husk.setAiDisabled(true);
		return husk;
	}

	/** Puts the item in the main hand and lets a tick apply its attributes, as a real player's tick would. */
	static void hold(ServerPlayerEntity player, Item item) {
		player.setStackInHand(Hand.MAIN_HAND, new ItemStack(item));
		player.playerTick();
	}

	static double modifier(ItemStack stack, EntityAttribute attribute) {
		return stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(attribute).stream()
				.mapToDouble(EntityAttributeModifier::getValue).sum();
	}

	static void equip(LivingEntity entity, List<Item> set) {
		for (int i = 0; i < GearArmor.SLOTS.size(); i++) {
			entity.equipStack(GearArmor.SLOTS.get(i), new ItemStack(set.get(i)));
		}
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
