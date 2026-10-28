package com.embervoid.gametest;

import com.embervoid.entity.mob.HollowEntity;
import com.embervoid.entity.mob.MobEntities;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.GameTestException;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * The Hollow's fight (DESIGN §3.2): its swing lands only after the wind-up and misses a
 * player who stepped out of reach during it; the raised guard takes front melee down to 20 % and deflects front arrows,
 * hits from behind go through; an axe breaks the guard and staggers it for 60 ticks.
 */
public final class HollowTests implements FabricGameTest {
	private static final float GUARDED_FACTOR = 0.2f;
	private static final int STAGGER_TICKS = 60;

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "hollow", tickLimit = 200)
	public void swingLandsAfterTheWindup(TestContext context) {
		BossArena arena = new BossArena(context, null);
		int windup = windupTicks();
		arena.step(() -> {
			arena.floor(4, 4, 4);
			HollowEntity hollow = hollow(context, 4, 3);
			ServerPlayerEntity player = arena.join(4, 4);
			arena.after(BossArena.JOIN_INVULNERABLE_TICKS, () -> {
				hollow.setTarget(player);
				context.assertTrue(startSwing(hollow), "the swing did not start");
				float health = player.getHealth();
				arena.after(windup - 1, () -> {
					context.assertTrue(player.getHealth() == health, "hit before the wind-up was over");
					arena.after(3, () -> {
						context.assertTrue(player.getHealth() < health, "the swing never landed");
						hollow.discard();
						arena.finish();
					});
				});
			});
		});
	}

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "hollow", tickLimit = 200)
	public void steppingAwayDodgesTheSwing(TestContext context) {
		BossArena arena = new BossArena(context, null);
		int windup = windupTicks();
		arena.step(() -> {
			arena.floor(4, 4, 4);
			HollowEntity hollow = hollow(context, 4, 2);
			ServerPlayerEntity player = arena.join(4, 3);
			arena.after(BossArena.JOIN_INVULNERABLE_TICKS, () -> {
				hollow.setTarget(player);
				context.assertTrue(startSwing(hollow), "the swing did not start");
				float health = player.getHealth();
				arena.after(windup / 2, () -> {
					// Five blocks back: outside its reach even with the grace for a target backing off.
					Vec3d away = context.getAbsolute(Vec3d.ofBottomCenter(new BlockPos(4, 1, 7)));
					player.teleport(context.getWorld(), away.x, away.y, away.z, 0, 0);
					arena.after(windup, () -> {
						context.assertTrue(player.getHealth() == health, "the swing hit a player who stepped away");
						hollow.discard();
						arena.finish();
					});
				});
			});
		});
	}

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "hollow", tickLimit = 300)
	public void guardBlocksFrontAndAnAxeBreaksIt(TestContext context) {
		BossArena arena = new BossArena(context, null);
		arena.step(() -> {
			arena.floor(4, 4, 4);
			HollowEntity hollow = hollow(context, 4, 3);
			ServerPlayerEntity player = arena.join(4, 5);
			hollow.setTarget(player);
			// The guard goes up at random (1 in 10 per tick) while a target is close.
			arena.await(hollow::isGuarding, 200, "the guard", () -> {
				face(hollow, 0);
				float armor = hollow.getArmor();
				float toughness = (float) hollow.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
				float amount = 10;

				// Front sword hit: 20 % of it, then armor.
				player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
				float before = hollow.getHealth();
				hollow.damage(context.getWorld().getDamageSources().playerAttack(player), amount);
				float expected = DamageUtil.getDamageLeft(amount * GUARDED_FACTOR, armor, toughness);
				context.assertTrue(Math.abs(before - hollow.getHealth() - expected) < 0.01f,
						"guarded front hit took " + (before - hollow.getHealth()) + ", expected " + expected);
				context.assertTrue(hollow.isGuarding(), "a sword hit dropped the guard");

				Vec3d front = hollow.getPos().add(0, 1, 2);
				ArrowEntity arrow = new ArrowEntity(context.getWorld(), front.x, front.y, front.z);
				arrow.setOwner(player);
				before = hollow.getHealth();
				boolean hurt = hollow.damage(context.getWorld().getDamageSources().arrow(arrow, player), amount);
				context.assertTrue(!hurt && hollow.getHealth() == before, "a front arrow got through the guard");
				arrow.discard();

				face(hollow, 180);
				hollow.timeUntilRegen = 0;
				before = hollow.getHealth();
				hollow.damage(context.getWorld().getDamageSources().playerAttack(player), amount);
				expected = DamageUtil.getDamageLeft(amount, armor, toughness);
				context.assertTrue(Math.abs(before - hollow.getHealth() - expected) < 0.01f,
						"hit from behind took " + (before - hollow.getHealth()) + ", expected " + expected);

				face(hollow, 0);
				hollow.timeUntilRegen = 0;
				player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
				hollow.damage(context.getWorld().getDamageSources().playerAttack(player), amount);
				context.assertTrue(hollow.isStaggered() && !hollow.isGuarding(), "the axe did not break the guard");
				arena.after(STAGGER_TICKS - 5, () -> {
					context.assertTrue(hollow.isStaggered(), "the stagger ended early");
					arena.after(10, () -> {
						context.assertTrue(!hollow.isStaggered(), "still staggered after " + STAGGER_TICKS + " ticks");
						hollow.discard();
						arena.finish();
					});
				});
			});
		});
	}

	/** A Hollow with an iron sword, no goals (it stands still), facing south (towards +z). */
	private static HollowEntity hollow(TestContext context, int x, int z) {
		HollowEntity hollow = context.spawnMob(MobEntities.HOLLOW, new BlockPos(x, 1, z));
		hollow.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
		face(hollow, 0);
		return hollow;
	}

	private static void face(HollowEntity hollow, float yaw) {
		hollow.setYaw(yaw);
		hollow.setHeadYaw(yaw);
		hollow.setBodyYaw(yaw);
	}

	/** The attack goal's entry point; package-private in the entity. */
	private static boolean startSwing(HollowEntity hollow) {
		try {
			Method method = HollowEntity.class.getDeclaredMethod("startSwing");
			method.setAccessible(true);
			return (boolean) method.invoke(hollow);
		} catch (ReflectiveOperationException e) {
			throw new GameTestException("HollowEntity.startSwing: " + e);
		}
	}

	private static int windupTicks() {
		try {
			Field field = HollowEntity.class.getDeclaredField("WINDUP_TICKS");
			field.setAccessible(true);
			return field.getInt(null);
		} catch (ReflectiveOperationException e) {
			throw new GameTestException("HollowEntity.WINDUP_TICKS: " + e);
		}
	}
}
