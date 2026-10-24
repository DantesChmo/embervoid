package com.embervoid.gametest;

import com.embervoid.block.setpiece.LitSetPieceBlock;
import com.embervoid.block.setpiece.SetPieceBlock;
import com.embervoid.block.setpiece.SetPieceBlockEntity;
import com.embervoid.block.setpiece.SetPieceCap;
import com.embervoid.block.setpiece.SetPieceConfig;
import com.embervoid.block.setpiece.SetPieceKind;
import com.embervoid.block.setpiece.SetPieces;
import com.embervoid.boss.ArenaManager;
import com.embervoid.boss.BossEntities;
import com.embervoid.boss.BossFight;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The set-piece framework (PLAN 20.1): the cap per chunk, triggers changing the synced state, arena events reaching
 * the pieces inside an arena and not those outside, the great brazier following its campfire, and every kind's
 * animation file holding the animations the kind names.
 */
public final class SetPieceTests implements FabricGameTest {
	/** Pieces beyond {@code setpieces.maxPerChunk} go when the chunk's pieces come alive; the lowest, northernmost stay. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "setpiece_cap", tickLimit = 60)
	public void chunkCapRemovesTheExtraPieces(TestContext context) {
		ServerWorld world = context.getWorld();
		int max = SetPieceConfig.get().maxPerChunk();
		context.assertTrue(max == 8, "default cap is " + max + ", expected 8");
		BlockPos origin = context.getAbsolutePos(new BlockPos(0, 1, 0));
		ChunkPos chunk = new ChunkPos(origin);
		List<BlockPos> placed = new ArrayList<>();
		BlockState bell = SetPieces.GREAT_BELL.getDefaultState();
		for (int i = 0; i < max + 3; i++) {
			BlockPos pos = new BlockPos(chunk.getStartX() + 2 + i, origin.getY() + 6 + (i % 3), chunk.getStartZ() + 2 + (i % 5));
			world.setBlockState(pos, bell);
			placed.add(pos);
		}
		context.assertTrue(SetPieceCap.piecesIn(world, chunk).size() >= max + 3, "the pieces were not placed");
		context.waitAndRun(3, () -> {
			List<BlockPos> left = SetPieceCap.piecesIn(world, chunk);
			List<BlockPos> expected = SetPieceCap.keep(placed, max);
			try {
				context.assertTrue(left.size() == max, "after the cap: " + left.size() + " pieces, expected " + max);
				context.assertTrue(left.containsAll(expected) && expected.containsAll(left), "the wrong pieces stayed: " + left);
				context.assertTrue(SetPieceCap.isFull(world, origin), "the chunk does not count as full");
			} finally {
				for (BlockPos pos : placed) {
					world.setBlockState(pos, Blocks.AIR.getDefaultState());
				}
			}
			context.complete();
		});
	}

	/** {@link SetPieces#trigger}: states, timed states, actions and modes change the block entity's synced data. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "setpiece_trigger", tickLimit = 60)
	public void triggersChangeTheSyncedState(TestContext context) {
		ServerWorld world = context.getWorld();
		BlockPos gate = context.getAbsolutePos(new BlockPos(1, 2, 1));
		BlockPos mill = context.getAbsolutePos(new BlockPos(3, 2, 1));
		BlockPos censer = context.getAbsolutePos(new BlockPos(5, 2, 1));
		world.setBlockState(gate, SetPieces.PORTCULLIS.getDefaultState().with(SetPieceBlock.FACING, Direction.EAST));
		world.setBlockState(mill, SetPieces.ASH_MILL.getDefaultState());
		world.setBlockState(censer, SetPieces.CENSER.getDefaultState());
		SetPieceBlockEntity gatePiece = SetPieces.at(world, gate);
		SetPieceBlockEntity millPiece = SetPieces.at(world, mill);
		SetPieceBlockEntity censerPiece = SetPieces.at(world, censer);
		context.assertTrue(gatePiece != null && millPiece != null && censerPiece != null, "no block entities");
		context.assertTrue(gatePiece.state().equals("open") && gatePiece.mode() == SetPieceKind.Mode.TRIGGERED, "portcullis starts " + gatePiece.state());
		context.assertTrue(world.getBlockState(gate).get(SetPieceBlock.FACING) == Direction.EAST, "facing lost");

		context.assertTrue(SetPieces.trigger(world, gate, "closed+slam"), "closed+slam not understood");
		context.assertTrue(gatePiece.state().equals("closed"), "portcullis state " + gatePiece.state());
		context.assertTrue(gatePiece.actionsTriggered() == 1, "slam not triggered");
		NbtCompound synced = gatePiece.toInitialChunkDataNbt();
		context.assertTrue("closed".equals(synced.getString("State")) && "triggered".equals(synced.getString("Mode")), "synced NBT " + synced);
		context.assertTrue(gatePiece.toUpdatePacket() != null, "no update packet");
		context.assertTrue(!SetPieces.trigger(world, gate, "sideways"), "an unknown event was accepted");
		context.assertTrue(!SetPieces.trigger(world, gate.up(3), "open"), "a trigger on air was accepted");

		context.assertTrue(SetPieces.trigger(world, mill, "always"), "mode not understood");
		context.assertTrue(millPiece.mode() == SetPieceKind.Mode.ALWAYS, "mode " + millPiece.mode());
		context.assertTrue(SetPieces.trigger(world, mill, "spinning+spin_up 5"), "timed state not understood");
		context.assertTrue(millPiece.state().equals("spinning"), "mill state " + millPiece.state());

		context.assertTrue(SetPieces.trigger(world, censer, "lit"), "lit not understood");
		BlockState litState = world.getBlockState(censer);
		context.assertTrue(litState.get(LitSetPieceBlock.LIT), "censer blockstate not lit");
		context.assertTrue(litState.getLuminance() > 0, "a lit censer gives no light");
		context.assertTrue(SetPieces.at(world, censer) != null && SetPieces.at(world, censer).state().equals("lit"), "the censer lost its state with the blockstate change");

		context.waitAndRun(10, () -> {
			context.assertTrue(millPiece.state().equals("still"), "mill did not fall back: " + millPiece.state());
			SetPieces.trigger(world, censer, "cold");
			context.assertTrue(!world.getBlockState(censer).get(LitSetPieceBlock.LIT), "censer still lit");
			context.complete();
		});
	}

	/** A fight's events reach the pieces inside its arena (radius + 8), not the ones beyond; a reset puts them back. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "setpiece_arena", tickLimit = 120)
	public void arenaEventsReachThePiecesInside(TestContext context) {
		ServerWorld world = context.getWorld();
		BossArena arena = new BossArena(context, null);
		int radius = 6;
		arena.step(() -> {
			arena.floor(4, 4, radius + 1);
			BlockPos inside = context.getAbsolutePos(new BlockPos(9, 2, 4)); // 5 blocks out: inside the fog wall
			BlockPos nearby = context.getAbsolutePos(new BlockPos(4, 3, 16)); // 12 blocks out: within the margin of 8
			BlockPos outside = context.getAbsolutePos(new BlockPos(4, 2, 24)); // 20 blocks out: beyond it
			world.setBlockState(inside, SetPieces.GREAT_BELL.getDefaultState());
			world.setBlockState(nearby, SetPieces.SLUICE_GRATE.getDefaultState());
			world.setBlockState(outside, SetPieces.GREAT_BELL.getDefaultState());
			SetPieceBlockEntity bell = SetPieces.at(world, inside);
			SetPieceBlockEntity grate = SetPieces.at(world, nearby);
			SetPieceBlockEntity far = SetPieces.at(world, outside);
			arena.join(4, 6);
			BossFight fight = arena.start(BossEntities.TEST_BOSS, 4, 4, radius, 10);
			context.assertTrue("summon".equals(bell.lastEvent()), "the bell inside heard " + bell.lastEvent());
			context.assertTrue(bell.actionsTriggered() == 1, "the bell did not toll at the summon");
			context.assertTrue("summon".equals(grate.lastEvent()), "the grate within the margin heard " + grate.lastEvent());
			context.assertTrue(far.lastEvent().isEmpty(), "the bell outside heard " + far.lastEvent());

			int heard = fight.fireArenaEvent("hazard flood");
			context.assertTrue(heard == 2, "hazard flood reached " + heard + " pieces");
			context.assertTrue(grate.state().equals("open"), "the grate did not open: " + grate.state());
			context.assertTrue(grate.actionsTriggered() == 1, "the grate did not lift");

			ArenaManager.reset(fight);
			context.assertTrue(grate.state().equals("closed"), "the grate did not close on the reset: " + grate.state());
			context.assertTrue("reset".equals(far.lastEvent()) == false, "the bell outside heard the reset");
			for (BlockPos pos : List.of(inside, nearby, outside)) {
				world.setBlockState(pos, Blocks.AIR.getDefaultState());
			}
			arena.finish();
		});
	}

	/** A great brazier over a campfire lights and goes out with it, and ignores the fight's phase events. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "setpiece_brazier", tickLimit = 60)
	public void greatBrazierFollowsItsCampfire(TestContext context) {
		ServerWorld world = context.getWorld();
		BlockPos fire = context.getAbsolutePos(new BlockPos(2, 2, 2));
		BlockPos brazier = fire.up();
		BlockPos lone = context.getAbsolutePos(new BlockPos(5, 2, 2));
		world.setBlockState(fire.down(), Blocks.STONE.getDefaultState());
		world.setBlockState(fire, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, false));
		world.setBlockState(brazier, SetPieces.GREAT_BRAZIER.getDefaultState());
		world.setBlockState(lone, SetPieces.GREAT_BRAZIER.getDefaultState());
		SetPieceBlockEntity piece = SetPieces.at(world, brazier);
		SetPieceBlockEntity lonePiece = SetPieces.at(world, lone);
		context.assertTrue(piece.state().equals("cold"), "starts " + piece.state());
		world.setBlockState(fire, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true));
		context.assertTrue(piece.state().equals("lit"), "did not light with the campfire: " + piece.state());
		context.assertTrue(world.getBlockState(brazier).get(LitSetPieceBlock.LIT), "blockstate not lit");
		world.setBlockState(fire, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, false));
		context.assertTrue(piece.state().equals("cold"), "did not go out with the campfire: " + piece.state());
		piece.onArenaEvent("phase 1");
		lonePiece.onArenaEvent("phase 1");
		context.assertTrue(piece.state().equals("cold"), "a phase lit the brazier over a campfire");
		context.assertTrue(lonePiece.state().equals("lit"), "a phase did not light the lone brazier");
		context.complete();
	}

	/** Every kind's animation file holds the loops and actions the kind names, and its model and textures exist. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "setpiece_files", tickLimit = 60)
	public void animationFilesMatchTheKinds(TestContext context) {
		for (SetPieceKind kind : SetPieces.kinds()) {
			String base = "/assets/embervoid/";
			JsonObject animations = json(base + "animations/block/setpiece/" + kind.name() + ".animation.json");
			context.assertTrue(animations != null, kind.name() + ": no animation file");
			JsonObject list = animations.getAsJsonObject("animations");
			for (String name : kind.animationNames()) {
				context.assertTrue(list.has(name), kind.name() + ": animation " + name + " missing (has " + list.keySet() + ")");
			}
			context.assertTrue(json(base + "geo/block/setpiece/" + kind.name() + ".geo.json") != null, kind.name() + ": no geo file");
			for (String texture : List.of("textures/block/setpiece/" + kind.name() + ".png",
					// No glowmask for a kind with nothing that glows (GeckoLib refuses an empty one; ClientAssetRulesTest).
					"textures/item/setpiece/" + kind.name() + ".png", "models/block/setpiece/" + kind.name() + ".json")) {
				try (InputStream in = SetPieceTests.class.getResourceAsStream(base + texture)) {
					context.assertTrue(in != null, kind.name() + ": missing " + texture);
				} catch (Exception e) {
					context.throwGameTestException(kind.name() + ": " + e);
				}
			}
		}
		context.complete();
	}

	private static JsonObject json(String resource) {
		try (InputStream in = SetPieceTests.class.getResourceAsStream(resource)) {
			if (in == null) {
				return null;
			}
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (Exception e) {
			return null;
		}
	}
}
