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
 * The Cleft Stele: a ritual circle of Cleft Stone, or the older slab ({@link SteleFrame}), that a Crying Soul
 * kindles. This class kindles it and puts it out; which stele is the world's lit one and where the crack leads is
 * {@link SteleTravel}, which hooks {@link #KINDLED}, {@link #PUT_OUT} and {@link #onEnter}. The hub's own stele: {@link HubStele}.
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
	 * never in a realm, the hub or an arena. Works on both sides.
	 */
	public static boolean canKindleIn(World world) {
		return HubTravel.isOverworld(world.getRegistryKey());
	}

	public static SteleFrame.Cell cell(BlockState state) {
		if (state.isOf(SteleBlocks.CLEFT_STONE)) {
			return SteleFrame.Cell.STONE;
		}
		if (isPassage(state) || state.isAir() || state.isReplaceable() && state.getFluidState().isEmpty()) {
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

	/** Whether the passage of {@code frame} (its crack or rift) is filled. */
	public static boolean isLit(BlockView world, SteleFrame frame) {
		Block passage = passage(frame).getBlock();
		return frame.crack().stream().allMatch(pos -> world.getBlockState(pos).isOf(passage));
	}

	/** The block a lit {@code frame}'s passage is made of: a slab's crack along its axis, a circle's rift. */
	public static BlockState passage(SteleFrame frame) {
		return frame.shape() == SteleFrame.Shape.CIRCLE ? SteleBlocks.STELE_RIFT.getDefaultState()
				: SteleBlocks.STELE_CRACK.getDefaultState().with(SteleCrackBlock.AXIS, frame.axis());
	}

	/** Whether {@code state} is a lit passage: a crack or a rift. */
	public static boolean isPassage(BlockState state) {
		return state.isOf(SteleBlocks.STELE_CRACK) || state.isOf(SteleBlocks.STELE_RIFT);
	}

	/** Turns the stones kindled and fills the passage. The frame must be valid ({@link #coldFrame}). */
	public static void kindle(ServerWorld world, SteleFrame frame, @Nullable ServerPlayerEntity player) {
		for (BlockPos stone : frame.stones()) {
			world.setBlockState(stone, world.getBlockState(stone).with(CleftStoneBlock.KINDLED, true));
		}
		BlockState passage = passage(frame);
		for (BlockPos pos : frame.crack()) {
			world.setBlockState(pos, passage);
		}
		Vec3d middle = frame.middle().subtract(0, 0.5, 0);
		world.playSound(null, middle.x, middle.y, middle.z, SteleSounds.KINDLE, SoundCategory.BLOCKS, 1.5f, 0.8f);
		double spreadX = frame.shape() == SteleFrame.Shape.CIRCLE ? 0.25 : frame.axis() == Direction.Axis.X ? 0.15 : 0.05;
		double spreadZ = frame.shape() == SteleFrame.Shape.CIRCLE ? 0.25 : frame.axis() == Direction.Axis.X ? 0.05 : 0.15;
		double height = frame.crack().size() / 4.0;
		world.spawnParticles(ParticleTypes.FLAME, middle.x, middle.y, middle.z, 40, spreadX, height, spreadZ, 0.02);
		world.spawnParticles(ParticleTypes.CLOUD, middle.x, middle.y, middle.z, 20, spreadX + 0.1, height, spreadZ + 0.1, 0.01);
		if (frame.shape() == SteleFrame.Shape.CIRCLE) {
			// Each pillar flares as the circle catches.
			for (BlockPos stone : frame.stones()) {
				world.spawnParticles(ParticleTypes.SMALL_FLAME, stone.getX() + 0.5, stone.getY() + 0.5, stone.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.01);
			}
		}
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
			if (!spare.contains(pos) && isPassage(world.getBlockState(pos))) {
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
			Vec3d middle = frame.middle();
			world.playSound(null, middle.x, middle.y, middle.z, SteleSounds.PUT_OUT, SoundCategory.BLOCKS, 1.0f, 0.9f);
		}
	}

	/**
	 * A kindled stone at {@code lostStone} is gone: the kindled stones and passage blocks joined to it go cold and
	 * empty, and so does a lit circle the stone stood in (its pillars stand apart, joined only through the circle).
	 */
	static void putOut(ServerWorld world, BlockPos lostStone) {
		List<BlockPos> found = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> open = new ArrayDeque<>();
		seen.add(lostStone);
		open.add(lostStone);
		for (SteleFrame circle : SteleFrame.circlesWith(lostStone)) {
			if (world.getBlockState(circle.crack().get(0)).isOf(SteleBlocks.STELE_RIFT)) {
				for (BlockPos pos : concat(circle.stones(), circle.crack())) {
					if (seen.add(pos) && isLitPart(world.getBlockState(pos))) {
						found.add(pos);
						open.add(pos);
					}
				}
			}
		}
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
		// The passage first: nothing should stand in a crack whose stones are already cold.
		for (BlockPos pos : found) {
			if (isPassage(world.getBlockState(pos))) {
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

	private static List<BlockPos> concat(List<BlockPos> a, List<BlockPos> b) {
		List<BlockPos> all = new ArrayList<>(a);
		all.addAll(b);
		return all;
	}

	private static boolean isLitPart(BlockState state) {
		return isPassage(state)
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
