package com.embervoid.gametest;

import com.embervoid.hub.Hub;
import com.embervoid.hub.HubLayout;
import com.embervoid.hub.HubState;
import com.embervoid.item.HealingFlaskItem;
import com.embervoid.player.PlayerDataManager;
import com.embervoid.progress.ProgressManager;
import com.embervoid.sanctum.SanctumBlocks;
import com.embervoid.sanctum.SanctumEntities;
import com.embervoid.sanctum.VoidObeliskBlock.Glow;
import com.embervoid.sanctum.VoidObeliskBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.GameTestException;
import net.minecraft.test.TestContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.function.BooleanSupplier;

/**
 * The hub dimension on the test server, which has the datapack dimensions ({@code TestServerDimensionsMixin}).
 * Every test touches the one shared hub, so each has its own batch (tests of one batch run at the same time).
 */
public final class HubTests implements FabricGameTest {
	/** Upper bound for the hub's chunks and their entities to load after force-loading them. */
	private static final int LOAD_LIMIT = 400;
	/** The hub template's chunks: it spans -24..24 on both axes. */
	private static final int CHUNK_MIN = -2;
	private static final int CHUNK_MAX = 1;

	/**
	 * The server placed the hub when it started; placing it again does nothing, not even repairing it. The shrine, the
	 * obelisk ring with its realms, the lit hub stele and exactly one Ashen Maiden are there.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "hub_placed", tickLimit = LOAD_LIMIT + 100)
	public void hubIsPlacedOnceWithMaidenAndRing(TestContext context) {
		ServerWorld hub = hub(context);
		context.assertTrue(HubState.get(hub).placed(), "the hub was not placed on server start");
		context.assertTrue(!Hub.ensurePlaced(context.getWorld().getServer()), "the hub was placed a second time");
		String wrong = Hub.verify(hub);
		context.assertTrue(wrong == null, "hub not whole: " + wrong);

		// A missing block stays missing: nothing places or repairs the hub again.
		BlockPos corner = new BlockPos(HubLayout.CORE_HALF, HubLayout.FLOOR_Y + 7, HubLayout.CORE_HALF);
		BlockState lantern = hub.getBlockState(corner);
		context.assertTrue(!lantern.isAir(), "no lantern on the corner pillar at " + corner.toShortString());
		hub.setBlockState(corner, Blocks.AIR.getDefaultState());
		Hub.ensurePlaced(context.getWorld().getServer());
		boolean repaired = !hub.getBlockState(corner).isAir();
		hub.setBlockState(corner, lantern);
		context.assertTrue(!repaired, "the hub was placed again over itself");

		int obelisks = 0;
		for (int x = CHUNK_MIN; x <= CHUNK_MAX; x++) {
			for (int z = CHUNK_MIN; z <= CHUNK_MAX; z++) {
				for (BlockEntity entity : hub.getChunk(x, z).getBlockEntities().values()) {
					if (entity.getCachedState().isOf(SanctumBlocks.VOID_OBELISK)) {
						obelisks++;
					}
				}
			}
		}
		context.assertTrue(obelisks == HubLayout.OBELISKS.size(), obelisks + " Void Obelisks in the hub");

		setChunksForced(hub, true);
		await(context, () -> hub.shouldTickEntity(HubLayout.MAIDEN), LOAD_LIMIT, "the hub's entities to load", () ->
				// A few more ticks for every entity section of the island to come in.
				context.waitAndRun(20, () -> {
					int maidens = hub.getEntitiesByType(SanctumEntities.ASHEN_MAIDEN, Hub.area(), maiden -> true).size();
					setChunksForced(hub, false);
					context.assertTrue(maidens == 1, maidens + " Ashen Maidens in the hub");
					context.complete();
				}));
	}

	/**
	 * Two players in the hub see the same obelisks in their own states (dark, lit, kindled from their own progress).
	 * Realm 1 lights for both on their first visit; the Realm 2 obelisk refuses the player who has not
	 * unlocked it, and the Realm 1 obelisk takes a player into the realm.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "hub_obelisks", tickLimit = 200)
	public void obeliskStatesArePerPlayer(TestContext context) {
		ServerWorld hub = hub(context);
		ServerPlayerEntity lit = inHub(context, 2, 2);
		ServerPlayerEntity dark = inHub(context, 4, 2);
		step(context, () -> {
			VoidObeliskBlockEntity first = obelisk(hub, HubLayout.OBELISKS.get(0));
			context.assertTrue(first.getRealm().equals(ProgressManager.REALM_1), "first obelisk leads to " + first.getRealm());
			HubLayout.Obelisk second = HubLayout.OBELISKS.get(1);
			ProgressManager.unlockRealm(lit, second.realm());
			for (HubLayout.Obelisk obelisk : HubLayout.OBELISKS) {
				boolean realm1 = obelisk.realm().equals(ProgressManager.REALM_1);
				Glow expected = realm1 || obelisk == second ? Glow.LIT : Glow.DARK;
				context.assertTrue(glow(lit, obelisk) == expected, "unlocking player sees " + obelisk.realm() + " " + glow(lit, obelisk));
				context.assertTrue(glow(dark, obelisk) == (realm1 ? Glow.LIT : Glow.DARK), "other player sees " + obelisk.realm()
						+ " " + glow(dark, obelisk));
			}
			ProgressManager.completeRealm(lit, ProgressManager.REALM_1);
			context.assertTrue(glow(lit, HubLayout.OBELISKS.get(0)) == Glow.KINDLED, "completed realm not kindled");
			context.assertTrue(glow(dark, HubLayout.OBELISKS.get(0)) == Glow.LIT, "the other player's obelisk changed");

			use(dark, hub, second.pos());
			context.assertTrue(dark.getServerWorld() == hub, "a dark obelisk took the player away");
			use(lit, hub, HubLayout.OBELISKS.get(0).pos());
			context.assertTrue(lit.getServerWorld() != hub, "the lit obelisk kept the player in the hub");
		}, lit, dark);
		TestPlayers.complete(context, lit, dark);
	}

	/**
	 * Outside creative nothing is built or broken: no block placed on the floor, the floor stays, no water poured. A
	 * creative player can still build.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "hub_protected", tickLimit = 200)
	public void buildingIsRefused(TestContext context) {
		ServerWorld hub = hub(context);
		ServerPlayerEntity player = inHub(context, 2, 2);
		step(context, () -> {
			BlockPos floor = HubLayout.LANDING.down();
			BlockState floorState = hub.getBlockState(floor);
			context.assertTrue(!floorState.isAir(), "no floor at the landing");

			player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.STONE, 4));
			ActionResult placed = use(player, hub, floor);
			context.assertTrue(!placed.isAccepted() && hub.getBlockState(HubLayout.LANDING).isAir(),
					"a block was placed in the hub: " + placed + ", " + hub.getBlockState(HubLayout.LANDING));

			context.assertTrue(!player.interactionManager.tryBreakBlock(floor), "the floor broke");
			context.assertTrue(hub.getBlockState(floor) == floorState, "the floor is now " + hub.getBlockState(floor));

			player.setPitch(90);
			player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
			ActionResult poured = player.interactionManager.interactItem(player, hub, player.getMainHandStack(), Hand.MAIN_HAND);
			context.assertTrue(!poured.isAccepted() && player.getMainHandStack().isOf(Items.WATER_BUCKET), "water was poured: " + poured);
			context.assertTrue(hub.getFluidState(HubLayout.LANDING).isEmpty(), "water at the landing");

			player.changeGameMode(GameMode.CREATIVE);
			player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.STONE));
			use(player, hub, floor);
			boolean built = hub.getBlockState(HubLayout.LANDING).isOf(Blocks.STONE);
			hub.setBlockState(HubLayout.LANDING, Blocks.AIR.getDefaultState());
			context.assertTrue(built, "a creative player could not build");
		}, player);
		TestPlayers.complete(context, player);
	}

	/**
	 * Resting at the hub's shrine with a block in hand is a rest, not building: full health and flasks, and no respawn
	 * point (the hub is not a realm, and a shrine is not a bed).
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "hub_shrine", tickLimit = 200)
	public void shrineRefillsWithoutRespawnPoint(TestContext context) {
		ServerWorld hub = hub(context);
		ServerPlayerEntity player = inHub(context, 2, 2);
		step(context, () -> {
			context.assertTrue(player.getSpawnPointPosition() == null, "the test player already has a respawn point");
			player.setHealth(player.getMaxHealth() / 2);
			PlayerDataManager.get(player).flaskCharges = 0;
			player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.STONE, 4));

			use(player, hub, HubLayout.SHRINE);
			context.assertTrue(player.getHealth() == player.getMaxHealth(), "not healed: " + player.getHealth());
			context.assertTrue(HealingFlaskItem.charges(player) == HealingFlaskItem.maxCharges(player),
					"flasks not refilled: " + HealingFlaskItem.charges(player));
			context.assertTrue(player.getSpawnPointPosition() == null, "the shrine set a respawn point at "
					+ player.getSpawnPointPosition());
			context.assertTrue(hub.getBlockState(HubLayout.SHRINE.up()).isAir(), "a block was placed on the shrine");
		}, player);
		TestPlayers.complete(context, player);
	}

	private static ServerWorld hub(TestContext context) {
		ServerWorld hub = Hub.world(context.getWorld().getServer());
		if (hub == null) {
			throw new GameTestException("no hub dimension in the test world");
		}
		return hub;
	}

	/** A survival test player, moved to the hub's landing. */
	private static ServerPlayerEntity inHub(TestContext context, int x, int z) {
		ServerPlayerEntity player = TestPlayers.create(context, x, z);
		if (!Hub.sendToLanding(player)) {
			TestPlayers.complete(context, player);
			throw new GameTestException("could not send a player to the hub");
		}
		return player;
	}

	/** Runs a test's checks; on a failure the players leave the server before the runner sees it. */
	private static void step(TestContext context, Runnable checks, ServerPlayerEntity... players) {
		try {
			checks.run();
		} catch (RuntimeException e) {
			for (ServerPlayerEntity player : players) {
				if (player.getServer().getPlayerManager().getPlayer(player.getUuid()) == player) {
					player.getServer().getPlayerManager().remove(player);
				}
			}
			throw e;
		}
	}

	/** A right click on the top of a block with whatever the player holds in the main hand. */
	private static ActionResult use(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
		BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos).add(0, 0.5, 0), Direction.UP, pos, false);
		return player.interactionManager.interactBlock(player, world, player.getMainHandStack(), Hand.MAIN_HAND, hit);
	}

	private static VoidObeliskBlockEntity obelisk(ServerWorld hub, HubLayout.Obelisk obelisk) {
		if (!(hub.getBlockEntity(obelisk.pos()) instanceof VoidObeliskBlockEntity entity)) {
			throw new GameTestException("no obelisk at " + obelisk.pos().toShortString());
		}
		return entity;
	}

	private static Glow glow(ServerPlayerEntity player, HubLayout.Obelisk obelisk) {
		VoidObeliskBlockEntity entity = (VoidObeliskBlockEntity) player.getServerWorld().getBlockEntity(obelisk.pos());
		return Glow.of(ProgressManager.get(player), entity.getRealm());
	}

	private static void setChunksForced(ServerWorld world, boolean forced) {
		for (int x = CHUNK_MIN; x <= CHUNK_MAX; x++) {
			for (int z = CHUNK_MIN; z <= CHUNK_MAX; z++) {
				world.setChunkForced(x, z, forced);
			}
		}
	}

	/** Runs {@code then} once {@code condition} holds, checking every tick; fails after {@code limit} ticks. */
	private static void await(TestContext context, BooleanSupplier condition, int limit, String what, Runnable then) {
		if (condition.getAsBoolean()) {
			then.run();
		} else if (limit <= 0) {
			throw new GameTestException("timed out waiting for " + what);
		} else {
			context.waitAndRun(1, () -> await(context, condition, limit - 1, what, then));
		}
	}
}
