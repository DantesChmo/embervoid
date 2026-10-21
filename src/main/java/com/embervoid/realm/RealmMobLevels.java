package com.embervoid.realm;

import com.embervoid.boss.ArenaBoss;
import com.embervoid.realm.worldgen.RealmBiomeSource;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Mob level bands per zone (DESIGN §5.4): a hostile mob that appears in a realm gets a level from the band of the
 * zone it stands in.
 *
 * <p>The level itself is stored by the mobs track's level API. Until that is on {@code main}, {@link #setLevel} and
 * {@link #hasLevel} are stubs, so this only decides levels; {@code /embervoid realm where} shows them.
 */
public final class RealmMobLevels {
	public static void register() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof Monster && !(entity instanceof ArenaBoss) && !hasLevel(entity)) {
				RealmDefinition.LevelBand band = bandAt(world, entity.getBlockPos());
				if (band != null) {
					setLevel(entity, band.min() + world.random.nextInt(band.max() - band.min() + 1));
				}
			}
		});
	}

	/** The level band at a position, or null outside realms. */
	@Nullable
	public static RealmDefinition.LevelBand bandAt(ServerWorld world, BlockPos pos) {
		RealmDefinition realm = Realms.byWorld(world.getRegistryKey());
		if (realm == null) {
			return null;
		}
		if (realm.depths().equals(world.getRegistryKey())) {
			return realm.depthsLevels();
		}
		int zone = zoneAt(world, pos);
		return zone >= 0 && zone < realm.zoneLevels().size() ? realm.zoneLevels().get(zone) : realm.landingLevels();
	}

	/** Zone index at a position (or {@link RealmLayout#LANDING} / {@link RealmLayout#ABYSS}); landing outside realms. */
	public static int zoneAt(ServerWorld world, BlockPos pos) {
		if (world.getChunkManager().getChunkGenerator().getBiomeSource() instanceof RealmBiomeSource source) {
			return source.zoneAt(pos.getX(), pos.getZ(), world.getChunkManager().getNoiseConfig().getMultiNoiseSampler());
		}
		return RealmLayout.LANDING;
	}

	// --- Stub of the mobs track's level API; replace with calls into it once it is on main. ---

	private static boolean hasLevel(Entity entity) {
		return false;
	}

	private static void setLevel(Entity entity, int level) {
	}

	private RealmMobLevels() {}
}
