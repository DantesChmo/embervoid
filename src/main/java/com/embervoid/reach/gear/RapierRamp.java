package com.embervoid.reach.gear;

import com.embervoid.fx.particle.EmberParticles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * The Royal Ash Rapier's ramp (DESIGN §6.1): full-charge hits in a row on the same target, each within
 * {@code gear.weapons.rampWindowTicks} of the last, add {@code rampPerHit} each to the next hit, up to {@code rampMax}.
 * A swing that hits nothing ({@link GearReach#onSwing}), a hit taken ({@link GearEffects#afterDamage}) or another
 * target starts it over. A hit at less than full charge neither builds nor breaks it. Server side, not saved.
 */
public final class RapierRamp {
	private record State(UUID target, int hits, long lastTick) {}

	private static final Map<PlayerEntity, State> STATES = new WeakHashMap<>();

	/** Damage multiplier of the player's next hit on {@code target}. */
	public static float multiplier(PlayerEntity player, Entity target) {
		GearConfig.Weapons config = GearConfig.get().weapons();
		return WeaponRules.rampMultiplier(hitsOn(player, target), config.rampPerHit(), config.rampMax());
	}

	/** Full-charge hits in a row the player has on {@code target} that still count. */
	public static int hitsOn(PlayerEntity player, Entity target) {
		State state = STATES.get(player);
		if (state == null || !state.target.equals(target.getUuid())
				|| !WeaponRules.keepsRamp(player.getWorld().getTime() - state.lastTick, GearConfig.get().weapons().rampWindowTicks())) {
			return 0;
		}
		return state.hits;
	}

	/** A landed Rapier hit at {@code charge}: a full one builds the ramp on its target. */
	static void onHit(PlayerEntity player, Entity target, float charge) {
		if (!WeaponRules.fullyCharged(charge)) {
			return;
		}
		STATES.put(player, new State(target.getUuid(), hitsOn(player, target) + 1, player.getWorld().getTime()));
	}

	/** A miss or a hit taken: the ramp is gone (a puff of soot off the blade says so, if there was one). */
	public static void reset(PlayerEntity player) {
		State state = STATES.remove(player);
		if (state != null && state.hits > 0 && player.getWorld() instanceof ServerWorld world) {
			Vec3d hand = player.getEyePos().add(player.getRotationVector().multiply(0.5)).add(0, -0.35, 0);
			EmberParticles.spawn(world, EmberParticles.SOOT, hand, 2 + Math.min(state.hits, 4), 0.15, 0.02);
		}
	}

	static boolean holdsRapier(PlayerEntity player) {
		return player.getMainHandStack().getItem() instanceof RoyalAshRapierItem;
	}

	private RapierRamp() {}
}
