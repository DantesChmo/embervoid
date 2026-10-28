package com.embervoid.sanctum;

import com.embervoid.Embervoid;
import com.embervoid.hub.stele.SteleBlocks;
import com.embervoid.mixin.SinglePoolElementAccessor;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.pool.SinglePoolElement;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

/**
 * The boss wing of a Ruined Sanctum (DESIGN §2), one jigsaw piece behind the gate hall: a shrine room, a
 * fog wall ({@link FogWallBlock}) and the arena with a cold Cleft Stele at its far end. The stele's stones cannot be
 * mined outside creative while they stand in an arena piece, so every dungeon keeps a stele for its Crying Soul.
 * The Fallen Sister is summoned at the arena's centre ({@code sanctum.boss.SanctumBossArena}).
 */
public final class SanctumArena {
	/** The overworld structure; the id stays from the Ember Sanctum so {@code /locate} and old maps still find it. */
	public static final RegistryKey<Structure> STRUCTURE = RegistryKey.of(RegistryKeys.STRUCTURE, Embervoid.id("ember_sanctum"));
	public static final Identifier TEMPLATE = Embervoid.id("sanctum/arena");
	/** Template of the old core (Maiden, obelisks): no pool leads to it; Sanctums already planned in 0.4.x worlds place it. */
	public static final Identifier OLD_CORE = Embervoid.id("sanctum/core");

	static void register() {
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			if (!state.isOf(SteleBlocks.CLEFT_STONE) || !(world instanceof ServerWorld server) || !isProtected(server, player, pos)) {
				return true;
			}
			player.sendMessage(Text.translatable("embervoid.sanctum.stele_bound"), true);
			return false;
		});
	}

	/** Whether {@code player} may not break the block at {@code pos}: it is in a Sanctum arena and they are not in creative. */
	private static boolean isProtected(ServerWorld world, PlayerEntity player, BlockPos pos) {
		// The abilities follow the game mode; unlike isCreative() they also hold for GameTest's mock players.
		return !player.getAbilities().creativeMode && arenaAt(world, pos) != null;
	}

	/** The arena piece of a Ruined Sanctum whose box holds {@code pos}, or null. */
	@Nullable
	public static StructurePiece arenaAt(ServerWorld world, BlockPos pos) {
		StructureStart start = world.getStructureAccessor().getStructureContaining(pos, STRUCTURE);
		for (StructurePiece piece : start.getChildren()) {
			if (piece.getBoundingBox().contains(pos) && isPiece(piece, TEMPLATE)) {
				return piece;
			}
		}
		return null;
	}

	public static boolean isPiece(StructurePiece piece, Identifier template) {
		return piece instanceof PoolStructurePiece pool && pool.getPoolElement() instanceof SinglePoolElement single
				&& ((SinglePoolElementAccessor) single).embervoid$location().left().filter(template::equals).isPresent();
	}

	private SanctumArena() {}
}
