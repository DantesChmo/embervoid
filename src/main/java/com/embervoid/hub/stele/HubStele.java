package com.embervoid.hub.stele;

import com.embervoid.hub.Hub;
import com.embervoid.hub.HubLayout;
import com.embervoid.hub.HubState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.ServerTask;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The hub's own Cleft Stele: built lit into the hub template at {@link HubLayout#STELE_CRACK_BOTTOM},
 * the hub's way out ({@link SteleTravel#leaveHub}). Always lit and never the world's stele: nobody can break it, not
 * even in creative, and if a stone goes some other way (a command) it is lit again right after. A hub placed before this
 * stele existed gets it on the next server start.
 */
public final class HubStele {
	public static final SteleFrame FRAME = new SteleFrame(
			HubLayout.STELE_CRACK_BOTTOM.offset(HubLayout.STELE_AXIS, -SteleFrame.CRACK), HubLayout.STELE_AXIS);

	static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			ServerWorld hub = Hub.world(server);
			if (hub != null && HubState.get(hub).placed()) {
				ensureLit(hub);
			}
		});
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> !isPart(world, pos));
		// After the block change that put it out is done, not inside it.
		CleftStele.PUT_OUT.register((world, lostStone) -> {
			if (isPart(world, lostStone)) {
				world.getServer().send(new ServerTask(world.getServer().getTicks(), () -> ensureLit(world)));
			}
		});
	}

	/** Whether {@code pos} is one of the hub stele's blocks, crack included. */
	public static boolean isPart(World world, BlockPos pos) {
		return Hub.isHub(world) && HubLayout.isSteleSpot(pos);
	}

	/** Lights whatever of the hub's stele is missing or cold. Returns whether anything had to be set. */
	public static boolean ensureLit(ServerWorld hub) {
		boolean changed = false;
		BlockState stone = SteleBlocks.CLEFT_STONE.getDefaultState().with(CleftStoneBlock.KINDLED, true);
		for (BlockPos pos : FRAME.stones()) {
			if (!hub.getBlockState(pos).equals(stone)) {
				hub.setBlockState(pos, stone, Block.NOTIFY_ALL);
				changed = true;
			}
		}
		BlockState crack = SteleBlocks.STELE_CRACK.getDefaultState().with(SteleCrackBlock.AXIS, FRAME.axis());
		for (BlockPos pos : FRAME.crack()) {
			if (!hub.getBlockState(pos).equals(crack)) {
				hub.setBlockState(pos, crack, Block.NOTIFY_ALL);
				changed = true;
			}
		}
		return changed;
	}

	private HubStele() {}
}
