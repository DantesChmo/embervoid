package com.embervoid.reach.gear;

import com.embervoid.anim.AttackAnimations;
import com.embervoid.anim.WeaponKind;
import com.embervoid.fx.CombatFx;
import com.embervoid.fx.SwingArc;
import com.embervoid.fx.particle.EmberParticles;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Arm;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.IntConsumer;

/**
 * Every weapon's signature in the air (docs/art-direction.md §4): the swing trail of each weapon kind on every swing,
 * what a kill with the Scythe lets go (soul wisps into the player), the Huntsman's bolts trailing embers, and a small
 * scheduler for effects that unfold over a few ticks (the Scythe's ash wave rolling forward, the Rapier's dash).
 * Nothing here changes a fight; the hits are in the items.
 */
public final class WeaponFx {
	/** Colours of the trails (0xRRGGBB): the Scythe's ash, the Rapier's amber, the Sanctum's cold steel, the Mace's ember. */
	public static final int ASH = 0xD9D2C6;
	public static final int AMBER = 0xF0B040;
	public static final int COLD = 0xBFE6FF;
	public static final int EMBER = CombatFx.EMBER;
	/** Soul wisps a Scythe kill frees, plus one per {@value #SOULS_PER_HEALTH} max health of the dead. */
	private static final int SOULS_BASE = 3;
	private static final float SOULS_PER_HEALTH = 12;
	private static final int SOULS_MAX = 12;
	private static final List<Timed> TIMED = new ArrayList<>();
	private static final List<PersistentProjectileEntity> BOLTS = new ArrayList<>();

	/** An effect unfolding tick by tick: {@code step} gets 0 … {@code ticks} - 1. */
	private record Timed(ServerWorld world, int ticks, IntConsumer step, int[] at) {
	}

	/** Runs {@code step} once a tick for {@code ticks} ticks, starting next tick. */
	public static void unfold(ServerWorld world, int ticks, IntConsumer step) {
		TIMED.add(new Timed(world, ticks, step, new int[]{0}));
	}

	/**
	 * A swing that started {@code clip} ({@link AttackAnimations#onSwing}, null for none): its trail, along the path the
	 * clip swings the weapon, mirrored for a left-handed player as the clip is.
	 */
	public static void onSwing(ServerPlayerEntity player, @Nullable WeaponKind.Clip clip) {
		SwingArc arc = clip == null ? null : swingArc(clip);
		if (arc != null) {
			CombatFx.trail(player, player.getMainArm() == Arm.LEFT ? arc.mirrored() : arc);
		}
	}

	/**
	 * The trail of a swing clip (tools/player_anim_art.py; angles 0 ahead, 90 to the right): the Scythe's sweep right
	 * to left and low, the Longsword's forehand from high right to low left and its backhand back up, the Rapier's
	 * thrust, the Mace's overhead; null for the whip (its lash is drawn as a rope).
	 */
	@Nullable
	static SwingArc swingArc(WeaponKind.Clip clip) {
		return switch (clip.name()) {
			case "scythe_sweep" -> new SwingArc(85, -85, -12, 3.4f, 1.15f, 7, ASH);
			case "sword_swing_1" -> new SwingArc(75, -75, 40, 2.7f, 1.35f, 6, COLD);
			case "sword_swing_2" -> new SwingArc(-75, 75, 40, 2.7f, 1.3f, 6, COLD);
			case "rapier_thrust" -> new SwingArc(-4, 4, 0, 3.4f, 1.35f, 4, AMBER);
			case "mace_overhead" -> SwingArc.overhead(2.5f, 1.5f, 8, EMBER);
			default -> null;
		};
	}

	/** A special attack that went through: its own trail, the way its clip turns (the Harvest and Wide Cleave go right to left). */
	public static void onSpecial(ServerPlayerEntity player, ItemStack stack) {
		WeaponKind kind = AttackAnimations.kindOf(stack);
		if (kind == null) {
			return;
		}
		SwingArc arc = switch (kind) {
			case SCYTHE -> new SwingArc(180, -180, -8, 3.6f, 1.2f, 9, ASH);
			case SWORD -> new SwingArc(100, -100, -10, 3.2f, 1.3f, 7, COLD);
			case MACE -> SwingArc.overhead(2.6f, 1.6f, 9, EMBER);
			default -> null;
		};
		if (arc != null) {
			CombatFx.trail(player, AttackAnimations.armHolding(player, stack) == Arm.LEFT ? arc.mirrored() : arc);
		}
	}

	/** A Scythe kill: the dead one's soul leaves it in wisps that fly into the reaper. */
	private static void onDeath(LivingEntity dead, DamageSource source) {
		if (!(source.getAttacker() instanceof ServerPlayerEntity player) || !(dead.getWorld() instanceof ServerWorld world)
				|| !(player.getMainHandStack().getItem() instanceof AshenScytheItem)) {
			return;
		}
		int count = Math.min(SOULS_MAX, SOULS_BASE + Math.round(dead.getMaxHealth() / SOULS_PER_HEALTH));
		Vec3d from = dead.getPos().add(0, dead.getHeight() * 0.6, 0);
		EmberParticles.souls(world, from, player.getEyePos().add(0, -0.4, 0), count);
		EmberParticles.spawn(world, EmberParticles.ASH_FLAKE, from, 12, 0.4, 0.04);
		world.playSound(null, dead.getX(), dead.getY(), dead.getZ(), GearSounds.REAP, SoundCategory.PLAYERS, 0.9f, 0.9f);
	}

	/** A bolt of the Huntsman's Crossbow just shot: it trails embers until it lands. */
	public static void trackBolt(PersistentProjectileEntity bolt) {
		BOLTS.add(bolt);
	}

	private static void tick(ServerWorld world) {
		for (Iterator<Timed> it = TIMED.iterator(); it.hasNext(); ) {
			Timed timed = it.next();
			if (timed.world() != world) {
				continue;
			}
			timed.step().accept(timed.at()[0]++);
			if (timed.at()[0] >= timed.ticks()) {
				it.remove();
			}
		}
		for (Iterator<PersistentProjectileEntity> it = BOLTS.iterator(); it.hasNext(); ) {
			PersistentProjectileEntity bolt = it.next();
			if (bolt.getWorld() != world) {
				continue;
			}
			if (bolt.isRemoved() || bolt.age > 100) {
				it.remove();
				if (!bolt.isOnGround() && bolt.age <= 100) {
					// Gone in the air: it went into a mob.
					EmberParticles.burst(world, bolt.getPos(), 0.6f, EMBER);
					EmberParticles.throwAll(world, EmberParticles.CINDER_SPARK, bolt.getPos(), 8, bolt.getVelocity().multiply(0.1), 0.15);
				}
				continue;
			}
			if (bolt.isOnGround() || bolt.getVelocity().lengthSquared() < 0.01) {
				it.remove();
				EmberParticles.spawn(world, EmberParticles.SOOT, bolt.getPos(), 2, 0.05, 0.01);
				continue;
			}
			Vec3d back = bolt.getVelocity().multiply(-0.05);
			EmberParticles.throwAll(world, EmberParticles.EMBER, bolt.getPos(), 2, back, 0.03);
			EmberParticles.throwAll(world, EmberParticles.CINDER_SPARK, bolt.getPos(), 1, back, 0.05);
		}
	}

	static void register() {
		ServerTickEvents.END_WORLD_TICK.register(WeaponFx::tick);
		ServerLivingEntityEvents.AFTER_DEATH.register(WeaponFx::onDeath);
	}

	private WeaponFx() {}
}
