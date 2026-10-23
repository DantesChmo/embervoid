package com.embervoid.hub.stele;

import com.embervoid.Embervoid;
import com.embervoid.hub.HubTravel;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The Cleft Stele (PLAN 15.3, DESIGN §2): a slab of Cleft Stone ({@link SteleFrame}) that a Crying Soul kindles. This
 * class kindles it and puts it out; which stele is the world's lit one and where the crack leads is {@link SteleTravel}
 * (PLAN 15.4), which hooks {@link #KINDLED}, {@link #PUT_OUT} and {@link #onEnter}. The hub's own stele: {@link HubStele}.
 */
public final class CleftStele {
	/** Fired on the server after a stele is kindled; {@code player} is null when no player did it. */
	public static final Event<Kindled> KINDLED = EventFactory.createArrayBacked(Kindled.class,
			listeners -> (world, frame, player) -> {
				for (Kindled listener : listeners) {
					listener.onKindled(world, frame, player);
				}
			});

	/** Fired on the server after a lit stele goes out because one of its stones is gone. */
	public static final Event<PutOut> PUT_OUT = EventFactory.createArrayBacked(PutOut.class,
			listeners -> (world, lostStone) -> {
				for (PutOut listener : listeners) {
					listener.onPutOut(world, lostStone);
				}
			});

	@FunctionalInterface
	public interface Kindled {
		void onKindled(ServerWorld world, SteleFrame frame, @Nullable ServerPlayerEntity player);
	}

	@FunctionalInterface
	public interface PutOut {
		void onPutOut(ServerWorld world, BlockPos lostStone);
	}

	/** More than one stele's worth of blocks: the put-out never wanders off through a whole wall of kindled stone. */
	private static final int PUT_OUT_LIMIT = 64;

	public static void register() {
		SteleBlocks.register();
		SteleItems.register();
		SteleTravel.register();
		HubStele.register();
		SteleCommands.register();
		Embervoid.LOGGER.debug("Registering the Cleft Stele");
	}

	/** A player stands in the crack of a lit stele (every tick they do): through it, if it is a working one. */
	public static void onEnter(ServerPlayerEntity player, BlockPos crack) {
		SteleTravel.walkIn(player, crack);
	}

	/**
	 * Whether a stele can be kindled in {@code world}: the overworld, the Nether, the End (any world but the mod's own);
	 * never in a realm, the hub or an arena (DESIGN §2 "Where"). Works on both sides.
	 */
	public static boolean canKindleIn(World world) {
		return HubTravel.isOverworld(world.getRegistryKey());
	}

	/** What a block counts as for {@link SteleFrame}. */
	public static SteleFrame.Cell cell(BlockState state) {
		if (state.isOf(SteleBlocks.CLEFT_STONE)) {
			return SteleFrame.Cell.STONE;
		}
		if (state.isOf(SteleBlocks.STELE_CRACK) || state.isAir() || state.isReplaceable() && state.getFluidState().isEmpty()) {
			return SteleFrame.Cell.OPEN;
		}
		return SteleFrame.Cell.OTHER;
	}

	/** A cold stele that {@code pos} belongs to, the one a Crying Soul used on {@code pos} would kindle. */
	public static Optional<SteleFrame> coldFrame(BlockView world, BlockPos pos) {
		if (!world.getBlockState(pos).isOf(SteleBlocks.CLEFT_STONE)) {
			return Optional.empty();
		}
		return SteleFrame.find(at -> cell(world.getBlockState(at)), pos).filter(frame -> !isLit(world, frame));
	}

	/** Whether the crack of {@code frame} is filled. */
	public static boolean isLit(BlockView world, SteleFrame frame) {
		return frame.crack().stream().allMatch(pos -> world.getBlockState(pos).isOf(SteleBlocks.STELE_CRACK));
	}

	/** Turns the stones kindled and fills the crack. The frame must be valid ({@link #coldFrame}). */
	public static void kindle(ServerWorld world, SteleFrame frame, @Nullable ServerPlayerEntity player) {
		for (BlockPos stone : frame.stones()) {
			world.setBlockState(stone, world.getBlockState(stone).with(CleftStoneBlock.KINDLED, true));
		}
		BlockState crack = SteleBlocks.STELE_CRACK.getDefaultState().with(SteleCrackBlock.AXIS, frame.axis());
		for (BlockPos pos : frame.crack()) {
			world.setBlockState(pos, crack);
		}
		Vec3d middle = Vec3d.ofCenter(frame.at(SteleFrame.CRACK, SteleFrame.HEIGHT / 2)).subtract(0, 0.5, 0);
		world.playSound(null, middle.x, middle.y, middle.z, SteleSounds.KINDLE, SoundCategory.BLOCKS, 1.5f, 0.8f);
		boolean alongX = frame.axis() == Direction.Axis.X;
		world.spawnParticles(ParticleTypes.FLAME, middle.x, middle.y, middle.z, 40, alongX ? 0.15 : 0.05, 1.4,
				alongX ? 0.05 : 0.15, 0.02);
		world.spawnParticles(ParticleTypes.CLOUD, middle.x, middle.y, middle.z, 20, alongX ? 0.2 : 0.3, 1.4,
				alongX ? 0.3 : 0.2, 0.01);
		KINDLED.invoker().onKindled(world, frame, player);
	}

	/**
	 * Puts out a whole stele that is still standing (another one was kindled): its kindled stones go cold, its crack
	 * empty, except the blocks in {@code spare} (those it shares with the new one). Only stele blocks are touched, so a
	 * frame that is no longer there costs nothing. Fires no {@link #PUT_OUT}: no stone was lost.
	 */
	public static void extinguish(ServerWorld world, SteleFrame frame, Collection<BlockPos> spare) {
		boolean changed = false;
		for (BlockPos pos : frame.crack()) {
			if (!spare.contains(pos) && world.getBlockState(pos).isOf(SteleBlocks.STELE_CRACK)) {
				world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
				changed = true;
			}
		}
		for (BlockPos pos : frame.stones()) {
			BlockState state = world.getBlockState(pos);
			if (!spare.contains(pos) && state.isOf(SteleBlocks.CLEFT_STONE) && state.get(CleftStoneBlock.KINDLED)) {
				world.setBlockState(pos, state.with(CleftStoneBlock.KINDLED, false), Block.NOTIFY_ALL);
				changed = true;
			}
		}
		if (changed) {
			Vec3d middle = Vec3d.ofCenter(frame.at(SteleFrame.CRACK, SteleFrame.HEIGHT / 2));
			world.playSound(null, middle.x, middle.y, middle.z, SteleSounds.PUT_OUT, SoundCategory.BLOCKS, 1.0f, 0.9f);
		}
	}

	/**
	 * A kindled stone at {@code lostStone} is gone: the kindled stones and crack blocks joined to it go cold and empty.
	 */
	static void putOut(ServerWorld world, BlockPos lostStone) {
		List<BlockPos> found = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> open = new ArrayDeque<>();
		seen.add(lostStone);
		open.add(lostStone);
		while (!open.isEmpty() && found.size() < PUT_OUT_LIMIT) {
			BlockPos pos = open.poll();
			for (Direction side : Direction.values()) {
				BlockPos next = pos.offset(side);
				if (seen.add(next) && isLitPart(world.getBlockState(next))) {
					found.add(next);
					open.add(next);
				}
			}
		}
		if (found.isEmpty()) {
			return;
		}
		// The crack first: nothing should stand in a crack whose stones are already cold.
		for (BlockPos pos : found) {
			if (world.getBlockState(pos).isOf(SteleBlocks.STELE_CRACK)) {
				world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
			}
		}
		for (BlockPos pos : found) {
			BlockState state = world.getBlockState(pos);
			if (state.isOf(SteleBlocks.CLEFT_STONE)) {
				world.setBlockState(pos, state.with(CleftStoneBlock.KINDLED, false), Block.NOTIFY_ALL);
			}
		}
		world.playSound(null, lostStone, SteleSounds.PUT_OUT, SoundCategory.BLOCKS, 1.0f, 0.9f);
		PUT_OUT.invoker().onPutOut(world, lostStone);
	}

	private static boolean isLitPart(BlockState state) {
		return state.isOf(SteleBlocks.STELE_CRACK)
				|| state.isOf(SteleBlocks.CLEFT_STONE) && state.get(CleftStoneBlock.KINDLED);
	}

	/**
	 * Uses the soul on {@code pos}: kindles the cold stele there, if any and if steles burn in this world. Returns
	 * whether it did.
	 */
	static boolean tryKindle(ServerWorld world, BlockPos pos, @Nullable PlayerEntity player) {
		if (!canKindleIn(world)) {
			return false;
		}
		Optional<SteleFrame> frame = coldFrame(world, pos);
		frame.ifPresent(f -> kindle(world, f, player instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null));
		return frame.isPresent();
	}

	private CleftStele() {}
}
