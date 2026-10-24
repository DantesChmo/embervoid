package com.embervoid.realm;

import com.embervoid.boss.ArenaBoss;
import com.embervoid.mob.MobLevels;
import com.embervoid.realm.worldgen.RealmBiomeSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;

/**
 * Mob level bands per zone (DESIGN §5.4): a hostile mob that appears in a realm gets a level from the band of the
 * zone it stands in (the final zone's band in its sector), through {@link MobLevels}' provider hook. Arena bosses get
 * theirs from {@code BossFight}.
 */
public final class RealmMobLevels {
	public static void register() {
		MobLevels.registerProvider((mob, world) -> {
			if (mob instanceof ArenaBoss) {
				return OptionalInt.empty();
			}
			RealmDefinition.LevelBand band = bandAt(world, mob.getBlockPos());
			return band == null ? OptionalInt.empty() : OptionalInt.of(band.min() + world.random.nextInt(band.max() - band.min() + 1));
		});
	}

	/** The level band at a position, or null outside realms. */
	@Nullable
	public static RealmDefinition.LevelBand bandAt(ServerWorld world, BlockPos pos) {
		RealmDefinition realm = Realms.byWorld(world.getRegistryKey());
		if (realm == null) {
			return null;
		}
		return realm.levelsOf(zoneAt(world, pos));
	}

	/**
	 * Zone index at a position (or {@link RealmLayout#LANDING} / {@link RealmLayout#ABYSS} / {@link RealmLayout#FINAL});
	 * landing outside realms.
	 */
	public static int zoneAt(ServerWorld world, BlockPos pos) {
		if (world.getChunkManager().getChunkGenerator().getBiomeSource() instanceof RealmBiomeSource source) {
			return source.zoneAt(pos.getX(), pos.getZ(), world.getChunkManager().getNoiseConfig().getMultiNoiseSampler());
		}
		return RealmLayout.LANDING;
	}

	private RealmMobLevels() {}
}
