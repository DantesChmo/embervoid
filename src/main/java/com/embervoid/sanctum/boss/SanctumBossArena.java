package com.embervoid.sanctum.boss;

import com.embervoid.Embervoid;
import com.embervoid.boss.ArenaManager;
import com.embervoid.boss.ArenaSpec;
import com.embervoid.boss.BossFight;
import com.embervoid.realm.Realms;
import com.embervoid.sanctum.SanctumArena;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * The summon rule of the Ruined Sanctum's arena (PLAN 16.3, DESIGN §2): a player who steps inside the fog radius of
 * an arena whose Fallen Sister is not beaten yet starts her fight on the boss framework ({@link ArenaManager#start}),
 * at the arena's centre (where 16.1's placeholder marker stood). Each dungeon's boss is beaten once per world, by
 * whoever is in the fight; after that the dungeon stays cleared ({@link ClearedSanctums}) and nobody summons her
 * there again. There is no rematch.
 */
public final class SanctumBossArena {
	/** Progress id of the Fallen Sister (what "beaten" is recorded under for a player's first clear). */
	public static final Identifier BOSS_ID = Embervoid.id("fallen_sister");
	/** Her level: the middle of the band 8–12 (DESIGN §2). */
	public static final int LEVEL = 10;
	/** The fog wall around the arena centre; the room's walls are 9 blocks out. */
	public static final int RADIUS = 9;
	/** The arena floor's centre in the boss wing template (16.1: the marker stood one block above it). */
	public static final BlockPos TEMPLATE_CENTER = new BlockPos(10, 0, 19);
	/** How often players are checked for stepping in, in ticks. */
	private static final int CHECK_INTERVAL = 5;
	/** How far above the arena floor a player still steps in. */
	private static final int STEP_IN_HEIGHT = 10;

	static void register() {
		ServerTickEvents.END_WORLD_TICK.register(SanctumBossArena::tick);
	}

	public static ArenaSpec spec() {
		return new ArenaSpec(BOSS_ID, SanctumBossEntities.FALLEN_SISTER, LEVEL, RADIUS);
	}

	private static void tick(ServerWorld world) {
		if (world.getTime() % CHECK_INTERVAL != 0 || Realms.isRealm(world)) {
			return;
		}
		for (ServerPlayerEntity player : world.getPlayers()) {
			if (!player.isSpectator() && player.isAlive()) {
				tryStart(world, player);
			}
		}
	}

	/** Starts her fight if {@code player} stands inside an uncleared arena with no fight running; returns it, or null. */
	@Nullable
	public static BossFight tryStart(ServerWorld world, ServerPlayerEntity player) {
		StructurePiece piece = SanctumArena.arenaAt(world, player.getBlockPos());
		if (piece == null) {
			return null;
		}
		BlockPos center = center(piece);
		if (!stepsIn(center, player.getPos()) || ArenaManager.fightAt(world, center) != null || isCleared(world, center)) {
			return null;
		}
		return ArenaManager.start(world, spec(), center);
	}

	/**
	 * Whether a player at {@code pos} has stepped into the arena with floor centre {@code center}: within a block of
	 * the fog wall's radius from the centre column, from the floor up to {@value #STEP_IN_HEIGHT} blocks above it.
	 */
	public static boolean stepsIn(BlockPos center, Vec3d pos) {
		double dx = pos.x - (center.getX() + 0.5);
		double dz = pos.z - (center.getZ() + 0.5);
		double dy = pos.y - center.getY();
		return dx * dx + dz * dz <= (RADIUS - 1) * (RADIUS - 1) && dy >= 0 && dy <= STEP_IN_HEIGHT;
	}

	/** The arena floor centre of a boss wing piece, wherever and however it was turned. */
	public static BlockPos center(StructurePiece piece) {
		PoolStructurePiece pool = (PoolStructurePiece) piece;
		return center(pool.getPos(), pool.getRotation());
	}

	/** The arena floor centre of a boss wing placed at {@code piecePos} turned by {@code rotation}. */
	public static BlockPos center(BlockPos piecePos, BlockRotation rotation) {
		return piecePos.add(StructureTemplate.transformAround(TEMPLATE_CENTER, BlockMirror.NONE, rotation, BlockPos.ORIGIN));
	}

	public static boolean isCleared(ServerWorld world, BlockPos center) {
		return ClearedSanctums.get(world).isCleared(center);
	}

	/** Her death: this dungeon is cleared for good. */
	static void markCleared(ServerWorld world, BlockPos center) {
		if (ClearedSanctums.get(world).clear(center)) {
			Embervoid.LOGGER.info("Ruined Sanctum at {} cleared", center.toShortString());
		}
	}

	private SanctumBossArena() {}
}
