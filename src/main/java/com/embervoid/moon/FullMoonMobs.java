package com.embervoid.moon;

import com.embervoid.boss.ArenaBoss;
import com.embervoid.mob.MobLevels;
import com.embervoid.moon.api.MoonNight;
import com.embervoid.moon.api.MoonNights;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.OptionalInt;
import java.util.UUID;

/**
 * Full moon mob modifiers (DESIGN §3.3, PLAN 8.4) for overworld hostiles (mobs that get a level): those that spawn
 * on a full moon get {@code moon.fullMoon.levelBonus} extra levels (kept after the night); while the full moon is on,
 * all loaded ones have their follow range multiplied by {@code moon.fullMoon.followRange} (a transient modifier,
 * removed when the night ends) and do not lose a player target that stays within it ({@code TrackTargetGoalMixin}).
 */
public final class FullMoonMobs {
	private static final UUID FOLLOW_RANGE_ID = UUID.fromString("3c1f6a52-7d0e-4b8a-9e51-2f4d8b6a1c07");

	static void register() {
		MobLevels.registerProvider((mob, world) -> {
			if (mob instanceof ArenaBoss || !isFullMoonHere(world)) {
				return OptionalInt.empty();
			}
			return OptionalInt.of(MobLevels.defaultLevel(mob, world) + MoonConfig.get().fullMoon().levelBonus());
		});
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (affected(entity, world)) {
				applyFollowRange((MobEntity) entity, isFullMoonHere(world));
			}
		});
		MoonNights.NIGHT_STARTED.register((server, night) -> {
			if (night == MoonNight.FULL_MOON) {
				updateLoaded(server, true);
			}
		});
		MoonNights.NIGHT_ENDED.register((server, night, reason) -> {
			if (night == MoonNight.FULL_MOON) {
				updateLoaded(server, false);
			}
		});
	}

	/**
	 * Whether a mob keeps {@code target} although its targeting goal would drop it: a full moon is on, the mob is an
	 * overworld hostile and the target is a player it may attack, within its follow range.
	 */
	public static boolean keepsTarget(MobEntity mob, LivingEntity target, double followRange) {
		if (!(target instanceof PlayerEntity) || !target.isAlive() || !mob.canTarget(target)
				|| !(mob.getWorld() instanceof ServerWorld world) || !affected(mob, world) || !isFullMoonHere(world)) {
			return false;
		}
		return mob.squaredDistanceTo(target) <= followRange * followRange;
	}

	private static boolean isFullMoonHere(ServerWorld world) {
		return world.getRegistryKey() == World.OVERWORLD && MoonNights.current(world.getServer()) == MoonNight.FULL_MOON;
	}

	private static boolean affected(Entity entity, ServerWorld world) {
		return world.getRegistryKey() == World.OVERWORLD && MobLevels.canHaveLevel(entity) && !(entity instanceof ArenaBoss);
	}

	private static void updateLoaded(MinecraftServer server, boolean on) {
		ServerWorld overworld = server.getOverworld();
		for (Entity entity : overworld.iterateEntities()) {
			if (affected(entity, overworld)) {
				applyFollowRange((MobEntity) entity, on);
			}
		}
	}

	private static void applyFollowRange(MobEntity mob, boolean on) {
		EntityAttributeInstance range = mob.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE);
		if (range == null) {
			return;
		}
		range.removeModifier(FOLLOW_RANGE_ID);
		double factor = MoonConfig.get().fullMoon().followRange();
		if (on && factor != 1) {
			range.addTemporaryModifier(new EntityAttributeModifier(FOLLOW_RANGE_ID, "Embervoid full moon", factor - 1,
					EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
		}
	}

	private FullMoonMobs() {}
}
