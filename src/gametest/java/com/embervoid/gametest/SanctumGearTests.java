package com.embervoid.gametest;

import com.embervoid.entity.mob.HollowEntity;
import com.embervoid.entity.mob.MobEntities;
import com.embervoid.player.StaminaManager;
import com.embervoid.reach.gear.GearWeaponItem;
import com.embervoid.reach.gear.GearWeapons;
import com.embervoid.sanctum.gear.SanctumGearConfig;
import com.embervoid.sanctum.gear.SanctumGearSounds;
import com.embervoid.sanctum.gear.SanctumWeapons;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * The Sanctum weapons (PLAN L1, DESIGN §10.10): both special attacks through the real right-click (stamina, cooldown,
 * who they hit, damage through {@code damage()}, never through a wall, their own sound), the mace breaking a Hollow's guard, the item
 * numbers and the outer-hall loot.
 */
public final class SanctumGearTests implements FabricGameTest {
	/** Wide Cleave: the arc in front takes 1.25× the hit; beside, behind, out of range and behind a wall nothing. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "sanctum_gear", tickLimit = 100)
	public void wideCleaveHitsTheArcInFront(TestContext context) {
		BossArena arena = new BossArena(context, null);
		arena.step(() -> {
			arena.floor(4, 4, 4);
			ServerPlayerEntity player = arena.join(4, 3);
			// Yaw 0 looks south (+z).
			face(player);
			Item sword = SanctumWeapons.SANCTUM_LONGSWORD;
			hold(player, sword);
			HuskEntity front = husk(context, 4, 5);
			HuskEntity diagonal = husk(context, 5, 5);
			HuskEntity beside = husk(context, 6, 3);
			HuskEntity behind = husk(context, 4, 1);
			HuskEntity far = husk(context, 4, 7);
			SanctumGearConfig config = SanctumGearConfig.get();
			float max = StaminaManager.getMax(player);

			ActionResult used = sword.use(context.getWorld(), player, Hand.MAIN_HAND).getResult();
			context.assertTrue(used.isAccepted(), "Wide Cleave was refused with full stamina: " + used);
			context.assertTrue(StaminaManager.get(player) == max - config.cleaveStamina(),
					"stamina after Wide Cleave: " + StaminaManager.get(player));
			context.assertTrue(SoundLog.heard(context.getWorld(), SanctumGearSounds.WIDE_CLEAVE, player.getPos(), 1),
					"Wide Cleave played no " + SanctumGearSounds.WIDE_CLEAVE.getId());
			float expected = 6.5f * config.cleaveMultiplier();
			hitFor(context, front, expected, "the mob in front");
			hitFor(context, diagonal, expected, "the mob 27° off the look");
			for (HuskEntity missed : List.of(beside, behind, far)) {
				context.assertTrue(missed.getHealth() == missed.getMaxHealth(), "Wide Cleave hit a mob outside the arc at "
						+ missed.getBlockPos().subtract(context.getAbsolutePos(BlockPos.ORIGIN)));
			}
			cooldown(context, player, sword, config.cleaveCooldown());

			// A wall between the player and the mob in front stops the cleave.
			context.setBlockState(4, 1, 4, Blocks.STONE);
			context.setBlockState(4, 2, 4, Blocks.STONE);
			front.setHealth(front.getMaxHealth());
			front.timeUntilRegen = 0;
			StaminaManager.set(player, max);
			sword.use(context.getWorld(), player, Hand.MAIN_HAND);
			context.assertTrue(front.getHealth() == front.getMaxHealth(), "Wide Cleave hit through a wall");

			// With no stamina it fails and starts no cooldown.
			player.getItemCooldownManager().remove(sword);
			StaminaManager.set(player, 0);
			ActionResult refused = sword.use(context.getWorld(), player, Hand.MAIN_HAND).getResult();
			context.assertTrue(!refused.isAccepted() && !player.getItemCooldownManager().isCoolingDown(sword),
					"Wide Cleave ran with no stamina: " + refused);
			for (HuskEntity husk : List.of(front, diagonal, beside, behind, far)) {
				husk.discard();
			}
			clearWall(context, 4, 4);
			arena.finish();
		});
	}

	/** Ground Slam: everything in the ring takes 0.75× the hit, is knocked away and slowed; not out of it or walled off. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "sanctum_gear", tickLimit = 100)
	public void groundSlamHitsTheRing(TestContext context) {
		BossArena arena = new BossArena(context, null);
		arena.step(() -> {
			arena.floor(4, 4, 4);
			ServerPlayerEntity player = arena.join(4, 3);
			face(player);
			Item mace = SanctumWeapons.ASHBEARERS_MACE;
			hold(player, mace);
			HuskEntity front = husk(context, 4, 5);
			HuskEntity behind = husk(context, 4, 1);
			HuskEntity beside = husk(context, 6, 3);
			HuskEntity far = husk(context, 4, 8);
			HuskEntity walled = husk(context, 2, 3);
			context.setBlockState(3, 1, 3, Blocks.STONE);
			context.setBlockState(3, 2, 3, Blocks.STONE);
			SanctumGearConfig config = SanctumGearConfig.get();
			float max = StaminaManager.getMax(player);

			ActionResult used = mace.use(context.getWorld(), player, Hand.MAIN_HAND).getResult();
			context.assertTrue(used.isAccepted(), "Ground Slam was refused with full stamina: " + used);
			context.assertTrue(StaminaManager.get(player) == max - config.slamStamina(),
					"stamina after Ground Slam: " + StaminaManager.get(player));
			context.assertTrue(SoundLog.heard(context.getWorld(), SanctumGearSounds.GROUND_SLAM, player.getPos(), 1),
					"Ground Slam played no " + SanctumGearSounds.GROUND_SLAM.getId());
			float expected = 8 * config.slamMultiplier();
			for (HuskEntity hit : List.of(front, behind, beside)) {
				hitFor(context, hit, expected, "a mob in the ring");
				StatusEffectInstance slow = hit.getStatusEffect(StatusEffects.SLOWNESS);
				context.assertTrue(slow != null && slow.getAmplifier() == config.slamSlownessAmplifier()
						&& slow.getDuration() == config.slamSlownessTicks(), "Ground Slam slowness: " + slow);
			}
			context.assertTrue(front.getVelocity().z > 0.1 && behind.getVelocity().z < -0.1 && beside.getVelocity().x > 0.1,
					"Ground Slam did not knock away: " + front.getVelocity() + " / " + behind.getVelocity() + " / " + beside.getVelocity());
			context.assertTrue(far.getHealth() == far.getMaxHealth(), "Ground Slam hit a mob 5 blocks away");
			context.assertTrue(walled.getHealth() == walled.getMaxHealth() && !walled.hasStatusEffect(StatusEffects.SLOWNESS),
					"Ground Slam hit through a wall");
			cooldown(context, player, mace, config.slamCooldown());
			for (HuskEntity husk : List.of(front, behind, beside, far, walled)) {
				husk.discard();
			}
			clearWall(context, 3, 3);
			arena.finish();
		});
	}

	/** The mace breaks a raised Hollow guard from the front and staggers it, like an axe; the longsword does not. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "sanctum_gear", tickLimit = 300)
	public void maceBreaksAHollowGuard(TestContext context) {
		BossArena arena = new BossArena(context, null);
		arena.step(() -> {
			arena.floor(4, 4, 4);
			HollowEntity hollow = context.spawnMob(MobEntities.HOLLOW, new BlockPos(4, 1, 3));
			hollow.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
			faceSouth(hollow);
			ServerPlayerEntity player = arena.join(4, 5);
			hollow.setTarget(player);
			// The guard goes up at random (1 in 10 per tick) while a target is close.
			arena.await(hollow::isGuarding, 200, "the guard", () -> {
				faceSouth(hollow);
				player.setStackInHand(Hand.MAIN_HAND, new ItemStack(SanctumWeapons.SANCTUM_LONGSWORD));
				hollow.damage(context.getWorld().getDamageSources().playerAttack(player), 5);
				context.assertTrue(hollow.isGuarding(), "a longsword hit broke the guard");
				hollow.timeUntilRegen = 0;
				player.setStackInHand(Hand.MAIN_HAND, new ItemStack(SanctumWeapons.ASHBEARERS_MACE));
				hollow.damage(context.getWorld().getDamageSources().playerAttack(player), 5);
				context.assertTrue(hollow.isStaggered() && !hollow.isGuarding(), "the mace did not break the guard");
				hollow.discard();
				arena.finish();
			});
		});
	}

	/**
	 * The weapons' numbers (damage, speed, durability, iron repair, the realm weapons' sword enchantments) and the
	 * outer-hall chest: each about 6 % of chests, never both in one.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "sanctum_gear", tickLimit = 200)
	public void weaponNumbersAndOuterHallLoot(TestContext context) {
		record Numbers(Item item, double damage, double speed) {}
		for (Numbers n : List.of(new Numbers(SanctumWeapons.SANCTUM_LONGSWORD, 6.5, 1.6), new Numbers(SanctumWeapons.ASHBEARERS_MACE, 8, 0.9))) {
			ItemStack stack = new ItemStack(n.item());
			context.assertTrue(Math.abs(GearTests.modifier(stack, EntityAttributes.GENERIC_ATTACK_DAMAGE) + 1 - n.damage()) < 1e-4
					&& Math.abs(GearTests.modifier(stack, EntityAttributes.GENERIC_ATTACK_SPEED) + 4 - n.speed()) < 1e-4, n + " modifiers");
			context.assertTrue(stack.getMaxDamage() == 600, n.item() + " durability " + stack.getMaxDamage());
			context.assertTrue(n.item().canRepair(stack, new ItemStack(Items.IRON_INGOT)), n.item() + " not repaired by iron");
			context.assertTrue(n.item() instanceof GearWeaponItem weapon && weapon.ability() != null, n.item() + " has no special");
			List<Enchantment> offered = EnchantmentHelper.getPossibleEntries(30, stack, false).stream().map(e -> e.enchantment).toList();
			for (Enchantment enchantment : GearWeapons.SWORD_ENCHANTMENTS) {
				context.assertTrue(enchantment.isAcceptableItem(stack) && offered.contains(enchantment),
						enchantment.getTranslationKey() + " not on " + n.item());
			}
		}
		context.assertTrue(HollowEntity.breaksGuard(new ItemStack(SanctumWeapons.ASHBEARERS_MACE))
				&& !HollowEntity.breaksGuard(new ItemStack(SanctumWeapons.SANCTUM_LONGSWORD)), "breaks_guard tag");

		LootContextParameterSet chest = new LootContextParameterSet.Builder(context.getWorld())
				.add(LootContextParameters.ORIGIN, context.getAbsolute(new Vec3d(1, 1, 1)))
				.build(LootContextTypes.CHEST);
		LootTable table = context.getWorld().getServer().getLootManager().getLootTable(com.embervoid.Embervoid.id("chests/sanctum_outer_hall"));
		int rolls = 4000;
		int swords = 0;
		int maces = 0;
		for (int i = 0; i < rolls; i++) {
			List<ItemStack> loot = table.generateLoot(chest);
			boolean sword = loot.stream().anyMatch(s -> s.isOf(SanctumWeapons.SANCTUM_LONGSWORD));
			boolean mace = loot.stream().anyMatch(s -> s.isOf(SanctumWeapons.ASHBEARERS_MACE));
			context.assertFalse(sword && mace, "both Sanctum weapons in one chest");
			swords += sword ? 1 : 0;
			maces += mace ? 1 : 0;
		}
		// Expected 240 each (6 %); ±60 is four standard deviations.
		context.assertTrue(swords >= 180 && swords <= 300 && maces >= 180 && maces <= 300,
				"outer hall: longsword in " + swords + ", mace in " + maces + " of " + rolls + " chests, expected ~240");
		context.complete();
	}

	/**
	 * Removes a two-high test wall: test areas of later batches overlap this one, and a mob placed into a leftover wall
	 * suffocates.
	 */
	private static void clearWall(TestContext context, int x, int z) {
		context.setBlockState(x, 1, z, Blocks.AIR);
		context.setBlockState(x, 2, z, Blocks.AIR);
	}

	private static void face(ServerPlayerEntity player) {
		player.setYaw(0);
		player.setHeadYaw(0);
		player.setPitch(0);
	}

	private static void faceSouth(HollowEntity hollow) {
		hollow.setYaw(0);
		hollow.setHeadYaw(0);
		hollow.setBodyYaw(0);
	}

	/** Puts the weapon in the main hand and lets a tick apply its attributes. */
	private static void hold(ServerPlayerEntity player, Item item) {
		player.setStackInHand(Hand.MAIN_HAND, new ItemStack(item));
		player.playerTick();
	}

	/** A husk with its AI off, standing at relative {@code (x, 1, z)}. */
	private static HuskEntity husk(TestContext context, int x, int z) {
		HuskEntity husk = context.spawnMob(EntityType.HUSK, new BlockPos(x, 1, z));
		husk.setAiDisabled(true);
		return husk;
	}

	/** The husk lost {@code amount} after its armor, as one player attack through {@code damage()} would take. */
	private static void hitFor(TestContext context, HuskEntity husk, float amount, String what) {
		float expected = DamageUtil.getDamageLeft(amount, husk.getArmor(),
				(float) husk.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS));
		float lost = husk.getMaxHealth() - husk.getHealth();
		context.assertTrue(Math.abs(lost - expected) < 0.01f, what + " lost " + lost + ", expected " + expected);
	}

	/** The item cools down for exactly {@code ticks}: still cooling one tick before, free after. */
	private static void cooldown(TestContext context, ServerPlayerEntity player, Item item, int ticks) {
		for (int i = 0; i < ticks - 1; i++) {
			player.getItemCooldownManager().update();
		}
		context.assertTrue(player.getItemCooldownManager().isCoolingDown(item), item + " cooled down before " + ticks + " ticks");
		player.getItemCooldownManager().update();
		context.assertTrue(!player.getItemCooldownManager().isCoolingDown(item), item + " still cooling after " + ticks + " ticks");
	}
}
