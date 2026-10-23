package com.embervoid.gametest;

import com.embervoid.hub.Hub;
import com.embervoid.hub.HubLayout;
import com.embervoid.hub.HubTravel;
import com.embervoid.hub.stele.CleftStele;
import com.embervoid.hub.stele.CleftStoneBlock;
import com.embervoid.hub.stele.HubStele;
import com.embervoid.hub.stele.SteleBlocks;
import com.embervoid.hub.stele.SteleFrame;
import com.embervoid.hub.stele.SteleItems;
import com.embervoid.hub.stele.SteleSounds;
import com.embervoid.hub.stele.SteleState;
import com.embervoid.hub.stele.SteleTravel;
import com.embervoid.realm.Realms;
import com.embervoid.realm.api.RealmTeleports;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.GameTestException;
import net.minecraft.test.TestContext;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The world's lit Cleft Stele and travel through it (PLAN 15.4, DESIGN §2): through a lit stele to the hub and back
 * out to stand in front of it, one lit stele per world, the fallback when it is broken, the Nether, no kindling in
 * realms or the hub, the hub's stele unbreakable, two players. The world has one lit stele and one hub, so every test
 * has its own batch.
 *
 * <p>Steles in the overworld stand along X in the test area: A at relative z = 1, B at z = 6, their cracks at x = 3,
 * on a stone floor; one comes out of them at z = 2 and z = 7 (the +Z side has room), facing south.
 */
public final class SteleTravelTests implements FabricGameTest {
	private static final double NEAR = 0.5;
	private static final int TICKS = 200;

	/**
	 * Kindle A, walk into its crack → the hub's landing (the way in sounds, the spot is remembered); the hub's crack
	 * does not take the player back at once, after the grace it does → in front of A. Then the same by right clicks.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "stele_round_trip", tickLimit = TICKS)
	public void roundTripThroughTheHub(TestContext context) {
		ServerWorld world = context.getWorld();
		ServerWorld hub = hub(context);
		floor(context);
		SteleFrame a = build(world, context.getAbsolutePos(new BlockPos(1, 1, 1)), Direction.Axis.X);
		ServerPlayerEntity player = TestPlayers.create(context, 3, 4);
		step(context, () -> {
			player.setStackInHand(Hand.MAIN_HAND, new ItemStack(SteleItems.CRYING_SOUL));
			kindle(context, player, a);
			context.assertTrue(player.getMainHandStack().isEmpty(), "the soul was not used up");
			Vec3d start = player.getPos();
			SoundLog.forget(hub, Vec3d.ofBottomCenter(HubLayout.LANDING), 4);
			SoundLog.forget(world, middle(a), 4);

			walkIn(player, a);
			assertAtLanding(context, current(player), "after walking into A");
			context.assertTrue(SoundLog.heard(hub, SteleSounds.ENTER, Vec3d.ofBottomCenter(HubLayout.LANDING), 2), "no sound on arrival");
			context.assertTrue(SoundLog.heard(world, SteleSounds.ENTER, middle(a), 2), "no sound at the stele left");
			HubTravel.Exit exit = HubTravel.lastOverworldExit(current(player));
			context.assertTrue(exit != null && exit.dimension() == World.OVERWORLD && exit.pos().distanceTo(start) < NEAR,
					"the way out was not remembered: " + exit);

			walkIn(current(player), HubStele.FRAME);
			assertAtLanding(context, current(player), "the hub's crack right after arriving");
		}, player);
		context.waitAndRun(SteleTravel.ARRIVAL_GRACE + 2, () -> step(context, () -> {
			SoundLog.forget(world, front(a), 4);
			walkIn(current(player), HubStele.FRAME);
			assertInFront(context, current(player), world, a, "after the hub's crack");
			context.assertTrue(SoundLog.heard(world, SteleSounds.LEAVE, front(a), 2), "no sound coming out");

			// A right click is deliberate: no grace.
			use(current(player), a.at(0, 2));
			assertAtLanding(context, current(player), "after a right click on A");
			use(current(player), HubStele.FRAME.at(4, 1));
			assertInFront(context, current(player), world, a, "after a right click on the hub's stele");
			context.assertTrue(isLit(SteleTravel.lit(world.getServer()), world, a), "A is no longer the lit stele");
			TestPlayers.complete(context, current(player));
		}, player));
	}

	/**
	 * One lit stele per world: kindling B puts A out (blocks cold, crack empty) and B leads to the hub; another player
	 * kindling A again puts B out for everyone, and the player who had used B is told, the one who kindled is not.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "stele_replace", tickLimit = TICKS)
	public void newSteleReplacesTheOld(TestContext context) {
		ServerWorld world = context.getWorld();
		floor(context);
		SteleFrame a = build(world, context.getAbsolutePos(new BlockPos(1, 1, 1)), Direction.Axis.X);
		SteleFrame b = build(world, context.getAbsolutePos(new BlockPos(1, 1, 6)), Direction.Axis.X);
		ChatRecorder chat = new ChatRecorder();
		ServerPlayerEntity first = TestPlayers.join(context, 3, 3, chat);
		ServerPlayerEntity second = TestPlayers.create(context, 3, 4);
		step(context, () -> {
			first.setStackInHand(Hand.MAIN_HAND, new ItemStack(SteleItems.CRYING_SOUL, 2));
			second.setStackInHand(Hand.MAIN_HAND, new ItemStack(SteleItems.CRYING_SOUL));
			kindle(context, first, a);

			SoundLog.forget(world, middle(a), 4);
			kindle(context, first, b);
			assertCold(context, world, a, "A after B was kindled");
			context.assertTrue(SoundLog.heard(world, SteleSounds.PUT_OUT, middle(a), 2), "no put-out sound at A");
			use(first, a.at(0, 0));
			context.assertTrue(current(first).getServerWorld() == world, "the cold A still led somewhere");
			first.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
			use(first, b.at(0, 0));
			assertAtLanding(context, current(first), "through B");
			use(current(first), HubStele.FRAME.at(0, 0));
			assertInFront(context, current(first), world, b, "out of the hub with B lit");

			// Another player's stele puts B out for everyone.
			int toldBefore = chat.count("embervoid.stele.replaced");
			kindle(context, second, a);
			assertCold(context, world, b, "B after another player kindled A");
			context.assertTrue(chat.count("embervoid.stele.replaced") == toldBefore + 1, "the user of B was not told");
			use(current(first), b.at(0, 0));
			context.assertTrue(current(first).getServerWorld() == world, "the cold B still led somewhere");
			use(current(first), a.at(4, 0));
			assertAtLanding(context, current(first), "through A again");
			use(current(first), HubStele.FRAME.at(0, 0));
			assertInFront(context, current(first), world, a, "out of the hub with A lit again");
			TestPlayers.complete(context, current(first), current(second));
		}, first, second);
	}

	/** Breaking a stone of the lit stele leaves none: the hub's way out is the spot the player left from. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "stele_broken", tickLimit = TICKS)
	public void brokenSteleFallsBackToTheLastExit(TestContext context) {
		ServerWorld world = context.getWorld();
		floor(context);
		SteleFrame a = build(world, context.getAbsolutePos(new BlockPos(1, 1, 1)), Direction.Axis.X);
		ServerPlayerEntity player = TestPlayers.create(context, 3, 4);
		ServerPlayerEntity breaker = TestPlayers.create(context, 6, 4);
		step(context, () -> {
			player.setStackInHand(Hand.MAIN_HAND, new ItemStack(SteleItems.CRYING_SOUL));
			kindle(context, player, a);
			Vec3d start = player.getPos();
			use(player, a.at(1, 1));
			assertAtLanding(context, current(player), "through A");

			BlockPos broken = a.at(4, 2);
			context.assertTrue(breaker.interactionManager.tryBreakBlock(broken), "A's stone did not break");
			context.assertTrue(SteleTravel.lit(world.getServer()) == null, "a broken stele is still the world's: "
					+ SteleTravel.lit(world.getServer()));
			world.setBlockState(broken, SteleBlocks.CLEFT_STONE.getDefaultState());
			assertCold(context, world, a, "A after a stone broke");

			use(current(player), HubStele.FRAME.at(3, 4));
			ServerPlayerEntity out = current(player);
			context.assertTrue(out.getServerWorld() == world && out.getPos().distanceTo(start) < NEAR,
					"out of the hub to " + out.getServerWorld().getRegistryKey().getValue() + " " + out.getPos()
							+ ", not where they left from " + start);
			TestPlayers.complete(context, out, breaker);
		}, player, breaker);
	}

	/** A stele works in the Nether: there to the hub, and back out in front of it. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "stele_nether", tickLimit = TICKS)
	public void steleInTheNether(TestContext context) {
		ServerWorld nether = Objects.requireNonNull(context.getWorld().getServer().getWorld(World.NETHER), "no Nether");
		BlockPos corner = context.getAbsolutePos(BlockPos.ORIGIN).withY(100);
		// A room of air on a stone floor, cut into whatever the Nether has there.
		for (BlockPos pos : BlockPos.iterate(corner.add(-1, -1, -1), corner.add(7, 7, 7))) {
			nether.setBlockState(pos, pos.getY() == corner.getY() - 1 ? Blocks.STONE.getDefaultState() : Blocks.AIR.getDefaultState());
		}
		SteleFrame a = build(nether, corner.add(1, 0, 1), Direction.Axis.X);
		ServerPlayerEntity player = TestPlayers.create(context, 3, 4);
		step(context, () -> {
			RealmTeleports.teleport(player, nether, Vec3d.ofBottomCenter(corner.add(3, 0, 4)), 0);
			ServerPlayerEntity there = current(player);
			there.setStackInHand(Hand.MAIN_HAND, new ItemStack(SteleItems.CRYING_SOUL));
			kindle(context, there, a);
			context.assertTrue(isLit(SteleTravel.lit(nether.getServer()), nether, a), "the Nether stele is not the world's: "
					+ SteleTravel.lit(nether.getServer()));

			walkIn(there, a);
			assertAtLanding(context, current(player), "through the Nether stele");
			HubTravel.Exit exit = HubTravel.lastOverworldExit(current(player));
			context.assertTrue(exit != null && exit.dimension() == World.NETHER, "the way out is not the Nether: " + exit);
			use(current(player), HubStele.FRAME.at(0, 5));
			assertInFront(context, current(player), nether, a, "out of the hub to the Nether stele");

			// Put out, so no later test finds it lit.
			nether.setBlockState(a.at(0, 0), Blocks.AIR.getDefaultState());
			context.assertTrue(SteleTravel.lit(nether.getServer()) == null, "the broken Nether stele is still lit");
			TestPlayers.complete(context, current(player));
		}, player);
	}

	/**
	 * In a realm and in the hub a soul kindles nothing (kept, the frame cold, the world's stele unchanged), and the
	 * debug command refuses too.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "stele_refused", tickLimit = TICKS)
	public void noKindlingInRealmsOrTheHub(TestContext context) {
		ServerWorld realm = Objects.requireNonNull(context.getWorld().getServer().getWorld(Realms.ASHEN_REACH.world()), "no realm");
		ServerWorld hub = hub(context);
		BlockPos landing = BlockPos.ofFloored(RealmTeleports.landingSpot(realm));
		SteleFrame inRealm = build(realm, landing.add(4, 2, 4), Direction.Axis.Z);
		SteleFrame inHub = build(hub, new BlockPos(0, HubLayout.FLOOR_Y + 20, HubLayout.ISLAND_RADIUS + 8), Direction.Axis.X);
		ServerPlayerEntity player = TestPlayers.create(context, 3, 4);
		step(context, () -> {
			SteleState.Lit before = SteleTravel.lit(realm.getServer());
			RealmTeleports.teleport(player, realm, Vec3d.ofBottomCenter(landing), 0);
			refused(context, current(player), inRealm, before, "in a realm");
			Hub.sendToLanding(current(player));
			refused(context, current(player), inHub, before, "in the hub");
			for (SteleFrame frame : List.of(inRealm, inHub)) {
				for (BlockPos pos : frame.stones()) {
					(frame == inHub ? hub : realm).setBlockState(pos, Blocks.AIR.getDefaultState());
				}
			}
			TestPlayers.complete(context, current(player));
		}, player);
	}

	private static void refused(TestContext context, ServerPlayerEntity player, SteleFrame frame, @Nullable SteleState.Lit before,
			String where) {
		ServerWorld world = player.getServerWorld();
		player.setStackInHand(Hand.MAIN_HAND, new ItemStack(SteleItems.CRYING_SOUL));
		ActionResult result = use(player, frame.at(0, 0));
		context.assertTrue(!result.isAccepted(), where + ": the soul was used (" + result + ")");
		context.assertTrue(player.getMainHandStack().getCount() == 1, where + ": the soul was taken");
		assertCold(context, world, frame, where);
		BlockPos pos = frame.at(0, 0);
		int kindled = player.getServer().getCommandManager().executeWithPrefix(player.getCommandSource().withLevel(2),
				"embervoid stele kindle " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
		context.assertTrue(kindled == 0, where + ": the command kindled it");
		assertCold(context, world, frame, where + " after the command");
		context.assertTrue(Objects.equals(SteleTravel.lit(player.getServer()), before), where + ": the world's stele changed to "
				+ SteleTravel.lit(player.getServer()));
	}

	/**
	 * The hub's stele is lit from the start and never the world's; nobody breaks it, not even in creative; a stone taken
	 * by a command puts it out only until the next tick.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "stele_hub", tickLimit = TICKS)
	public void hubSteleCannotBeBroken(TestContext context) {
		ServerWorld hub = hub(context);
		ServerPlayerEntity player = TestPlayers.create(context, 3, 4);
		step(context, () -> {
			assertHubSteleLit(context, hub, "from the start");
			context.assertTrue(SteleTravel.lit(hub.getServer()) == null || SteleTravel.lit(hub.getServer()).dimension() != Hub.KEY,
					"the hub's stele counts as the world's");
			Hub.sendToLanding(player);
			ServerPlayerEntity inHub = current(player);
			context.assertTrue(!inHub.interactionManager.tryBreakBlock(HubStele.FRAME.at(1, 0)), "a survival player broke a stone");
			inHub.changeGameMode(GameMode.CREATIVE);
			context.assertTrue(!inHub.interactionManager.tryBreakBlock(HubStele.FRAME.at(3, 2)), "a creative player broke a stone");
			context.assertTrue(!inHub.interactionManager.tryBreakBlock(HubStele.FRAME.at(SteleFrame.CRACK, 1)),
					"a creative player broke the crack");
			assertHubSteleLit(context, hub, "after the tries");

			hub.setBlockState(HubStele.FRAME.at(4, 5), Blocks.AIR.getDefaultState());
			context.assertTrue(!CleftStele.isLit(hub, HubStele.FRAME), "taking a stone did not put it out");
		}, player);
		context.waitAndRun(2, () -> step(context, () -> {
			assertHubSteleLit(context, hub, "a tick after a stone was taken");
			TestPlayers.complete(context, current(player));
		}, player));
	}

	/**
	 * Two players use one stele: the grace after a trip is each player's own, both come out in front of it, by walking
	 * in and by a right click.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "stele_two_players", tickLimit = TICKS)
	public void twoPlayersUseOneStele(TestContext context) {
		ServerWorld world = context.getWorld();
		floor(context);
		SteleFrame a = build(world, context.getAbsolutePos(new BlockPos(1, 1, 1)), Direction.Axis.X);
		ServerPlayerEntity[] p = {TestPlayers.create(context, 2, 4), TestPlayers.create(context, 5, 4)};
		step(context, () -> {
			p[0].setStackInHand(Hand.MAIN_HAND, new ItemStack(SteleItems.CRYING_SOUL));
			kindle(context, p[0], a);
			walkIn(p[0], a);
			assertAtLanding(context, current(p[0]), "the first player");
			walkIn(p[1], a);
			assertAtLanding(context, current(p[1]), "the second player, right after the first");
			use(current(p[1]), HubStele.FRAME.at(1, 3));
			assertInFront(context, current(p[1]), world, a, "the second player out by a right click");
			walkIn(current(p[0]), HubStele.FRAME);
			assertAtLanding(context, current(p[0]), "the first player in the hub's crack in their grace");
		}, p);
		context.waitAndRun(SteleTravel.ARRIVAL_GRACE + 2, () -> step(context, () -> {
			walkIn(current(p[0]), HubStele.FRAME);
			assertInFront(context, current(p[0]), world, a, "the first player out by the crack");
			TestPlayers.complete(context, current(p[0]), current(p[1]));
		}, p));
	}

	/** Uses the soul in the player's hand on a stone of {@code frame}; it must be lit and the world's stele after. */
	private static void kindle(TestContext context, ServerPlayerEntity player, SteleFrame frame) {
		ServerWorld world = player.getServerWorld();
		ActionResult result = use(player, frame.at(1, 0));
		context.assertTrue(result.isAccepted(), "the soul was not used on " + frame + ": " + result);
		context.assertTrue(CleftStele.isLit(world, frame), "the stele did not light: " + frame);
		context.assertTrue(isLit(SteleTravel.lit(world.getServer()), world, frame), "not the world's stele: "
				+ SteleTravel.lit(world.getServer()));
	}

	private static boolean isLit(@Nullable SteleState.Lit lit, ServerWorld world, SteleFrame frame) {
		return lit != null && lit.dimension() == world.getRegistryKey() && lit.frame().equals(frame);
	}

	private static void assertCold(TestContext context, ServerWorld world, SteleFrame frame, String when) {
		for (BlockPos stone : frame.stones()) {
			BlockState state = world.getBlockState(stone);
			context.assertTrue(state.isOf(SteleBlocks.CLEFT_STONE) && !state.get(CleftStoneBlock.KINDLED),
					when + ": stone at " + stone.toShortString() + " is " + state);
		}
		for (BlockPos crack : frame.crack()) {
			context.assertTrue(world.getBlockState(crack).isAir(), when + ": crack at " + crack.toShortString() + " is "
					+ world.getBlockState(crack));
		}
	}

	private static void assertHubSteleLit(TestContext context, ServerWorld hub, String when) {
		context.assertTrue(CleftStele.isLit(hub, HubStele.FRAME), when + ": the hub's crack is not whole");
		for (BlockPos stone : HubStele.FRAME.stones()) {
			BlockState state = hub.getBlockState(stone);
			context.assertTrue(state.isOf(SteleBlocks.CLEFT_STONE) && state.get(CleftStoneBlock.KINDLED),
					when + ": the hub's stone at " + stone.toShortString() + " is " + state);
		}
	}

	private static void assertAtLanding(TestContext context, ServerPlayerEntity player, String when) {
		context.assertTrue(Hub.isHub(player.getWorld()), when + ": in " + player.getServerWorld().getRegistryKey().getValue()
				+ ", not the hub");
		context.assertTrue(player.getPos().distanceTo(Vec3d.ofBottomCenter(HubLayout.LANDING)) < NEAR, when + ": at "
				+ player.getBlockPos().toShortString() + ", not the hub's landing");
	}

	/** Standing on the ground just in front of the crack, on the +Z side, facing away (south). */
	private static void assertInFront(TestContext context, ServerPlayerEntity player, ServerWorld world, SteleFrame frame, String when) {
		context.assertTrue(player.getServerWorld() == world, when + ": in " + player.getServerWorld().getRegistryKey().getValue());
		context.assertTrue(player.getPos().distanceTo(front(frame)) < NEAR, when + ": at " + player.getPos() + ", not in front of "
				+ frame.at(SteleFrame.CRACK, 0).toShortString());
		context.assertTrue(Math.abs(player.getYaw() - Direction.SOUTH.asRotation()) < 1, when + ": facing " + player.getYaw());
	}

	private static Vec3d front(SteleFrame frame) {
		return Vec3d.ofBottomCenter(frame.at(SteleFrame.CRACK, 0).south());
	}

	private static Vec3d middle(SteleFrame frame) {
		return Vec3d.ofCenter(frame.at(SteleFrame.CRACK, SteleFrame.HEIGHT / 2));
	}

	/** Stone under the whole test area. */
	private static void floor(TestContext context) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				context.setBlockState(x, 0, z, Blocks.STONE);
			}
		}
	}

	/** A cold stele: 24 stones, the crack column cleared. */
	private static SteleFrame build(ServerWorld world, BlockPos origin, Direction.Axis axis) {
		SteleFrame frame = new SteleFrame(origin, axis);
		for (BlockPos stone : frame.stones()) {
			world.setBlockState(stone, SteleBlocks.CLEFT_STONE.getDefaultState(), Block.NOTIFY_ALL);
		}
		for (BlockPos crack : frame.crack()) {
			world.setBlockState(crack, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
		}
		return frame;
	}

	/** The player stepping into the crack of {@code frame} (its block feels them, as every tick they stand in it). */
	private static void walkIn(ServerPlayerEntity player, SteleFrame frame) {
		ServerWorld world = player.getServerWorld();
		BlockPos crack = frame.at(SteleFrame.CRACK, 0);
		world.getBlockState(crack).onEntityCollision(world, crack, player);
	}

	/** A right click on {@code pos} with the main hand, the way the server handles one. */
	private static ActionResult use(ServerPlayerEntity player, BlockPos pos) {
		BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
		return player.interactionManager.interactBlock(player, player.getServerWorld(), player.getMainHandStack(),
				Hand.MAIN_HAND, hit);
	}

	private static ServerWorld hub(TestContext context) {
		ServerWorld hub = Hub.world(context.getWorld().getServer());
		if (hub == null) {
			throw new GameTestException("no hub dimension in the test world");
		}
		return hub;
	}

	/** The player entity now on the server for this player (a dimension change can replace it). */
	private static ServerPlayerEntity current(ServerPlayerEntity player) {
		ServerPlayerEntity now = player.getServer().getPlayerManager().getPlayer(player.getUuid());
		return now == null ? player : now;
	}

	/** Runs a test's checks; on a failure the players leave the server before the runner sees it. */
	private static void step(TestContext context, Runnable checks, ServerPlayerEntity... players) {
		try {
			checks.run();
		} catch (RuntimeException e) {
			for (ServerPlayerEntity player : players) {
				ServerPlayerEntity now = current(player);
				if (now.getServer().getPlayerManager().getPlayer(now.getUuid()) == now) {
					now.getServer().getPlayerManager().remove(now);
				}
			}
			throw e;
		}
	}

	/** A connection that keeps the translation keys of the chat lines sent to its player. */
	private static final class ChatRecorder extends ClientConnection {
		private final List<String> keys = new ArrayList<>();

		ChatRecorder() {
			super(NetworkSide.SERVERBOUND);
		}

		@Override
		public void send(Packet<?> packet) {
			send(packet, null);
		}

		@Override
		public void send(Packet<?> packet, @Nullable PacketCallbacks callbacks) {
			if (packet instanceof GameMessageS2CPacket message && !message.overlay()
					&& message.content().getContent() instanceof TranslatableTextContent text) {
				keys.add(text.getKey());
			}
		}

		int count(String key) {
			return (int) keys.stream().filter(key::equals).count();
		}
	}
}
