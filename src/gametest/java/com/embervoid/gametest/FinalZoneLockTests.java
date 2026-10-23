package com.embervoid.gametest;

import com.embervoid.gametest.mixin.EntityAccessor;
import com.embervoid.progress.ProgressManager;
import com.embervoid.reach.world.BorderWallFeature;
import com.embervoid.realm.EdgeLayout;
import com.embervoid.realm.FinalZoneLock;
import com.embervoid.realm.FinalZoneRegion;
import com.embervoid.realm.RealmBlocks;
import com.embervoid.realm.RealmSounds;
import com.embervoid.realm.Realms;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.EndermiteEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.test.GameTest;
import net.minecraft.test.GameTestException;
import net.minecraft.test.TestContext;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The final zone's lock (PLAN 21.2) in the real Ashen Reach, on its 60° border, one way in per test: walking, an ender
 * pearl, chorus fruit, elytra, riptide, a boat and a horse, a bridge over the wall and a tunnel under it. Each is
 * stopped for a player without clearance; a cleared player and a creative one pass, and two players with different
 * clearance at one spot inside are told apart. Every test has its own stretch of the border.
 *
 * <p>The players join for real (a mock test player is always creative) on a connection that keeps what they are sent.
 */
public final class FinalZoneLockTests implements FabricGameTest {
	private static final String BATCH = "final_zone_lock";
	private static final int LIMIT = 1200;

	/** Walking in: put back where they stood outside, told so, with the sealed-gate sound. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = BATCH, tickLimit = LIMIT)
	public void walkingInIsPushedBack(TestContext context) {
		Spot spot = new Spot(context, 140);
		Recorder recorder = new Recorder();
		ServerPlayerEntity player = spot.join(recorder, -2, 0);
		context.waitAndRun(3, () -> guard(context, spot, () -> {
			spot.put(player, 1.5, 0);
			context.assertTrue(spot.inside(player), "the step did not reach the final zone");
		}, player));
		context.waitAndRun(6, () -> guard(context, spot, () -> {
			context.assertTrue(!spot.inside(player), "still in the final zone at " + player.getBlockPos().toShortString());
			context.assertTrue(player.getPos().distanceTo(spot.at(-2, 0)) < 0.5,
					"not back where they stood: " + player.getPos() + " vs " + spot.at(-2, 0));
			context.assertTrue(recorder.messages.contains("embervoid.realm.final.locked"), "no message: " + recorder.messages);
			context.assertTrue(recorder.sounds.contains(RealmSounds.GATE_SEALED.getId()), "no sound: " + recorder.sounds);
			finish(context, spot, player);
		}, player));
	}

	/**
	 * An ender pearl thrown across the border by a player without clearance drops as an item where it crossed; a
	 * cleared player's pearl flies on in.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = BATCH, tickLimit = LIMIT)
	public void aPearlIntoTheFinalZoneDrops(TestContext context) {
		Spot spot = new Spot(context, 165);
		ServerPlayerEntity locked = spot.join(new Recorder(), -3, -3);
		ServerPlayerEntity cleared = spot.join(new Recorder(), -3, 3);
		clear(cleared);
		// Pearls only fly in chunks that tick entities: wait for the forced chunks to get there.
		spot.whenTicking(LIMIT / 2, () -> guard(context, spot, () -> {
			EnderPearlEntity lockedPearl = spot.throwPearl(locked, -3);
			EnderPearlEntity clearedPearl = spot.throwPearl(cleared, 3);
			boolean[] clearedPearlIn = {false};
			StringBuilder flight = new StringBuilder();
			for (int t = 1; t < 30; t++) {
				int tick = t;
				context.waitAndRun(t, () -> {
					if (!clearedPearl.isRemoved() && spot.region.contains(clearedPearl.getX(), clearedPearl.getZ())) {
						clearedPearlIn[0] = true;
					}
					if (tick < 10) {
						double[] at = spot.coordinates(clearedPearl);
						flight.append(String.format(" %d:%.1f/%.1f%s", tick, at[1], clearedPearl.getY() - spot.floor,
								clearedPearl.isRemoved() ? "x" : ""));
					}
				});
			}
			context.waitAndRun(30, () -> guard(context, spot, () -> {
				context.assertTrue(lockedPearl.isRemoved(), "the locked-out player's pearl is still flying at " + lockedPearl.getPos());
				List<ItemEntity> dropped = spot.realm.getEntitiesByClass(ItemEntity.class, spot.box(12),
						item -> item.getStack().isOf(Items.ENDER_PEARL));
				context.assertTrue(!dropped.isEmpty(), "no pearl dropped");
				for (ItemEntity item : dropped) {
					context.assertTrue(spot.region.borderDistance(item.getX(), item.getZ()) < 2.5,
							"the pearl dropped away from the border at " + item.getPos());
				}
				context.assertTrue(!spot.inside(locked), "the locked-out player got in");
				context.assertTrue(clearedPearlIn[0], "the cleared player's pearl never flew in (tick:across/height)" + flight);
				clearedPearl.discard();
				finish(context, spot, locked, cleared);
			}, locked, cleared));
		}, locked, cleared));
	}

	/** Chorus fruit eaten beside the border: often a jump, never into the final zone. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = BATCH, tickLimit = LIMIT)
	public void chorusFruitNeverLandsInside(TestContext context) {
		Spot spot = new Spot(context, 190);
		ServerPlayerEntity player = spot.join(new Recorder(), -1.5, 0);
		context.waitAndRun(3, () -> guard(context, spot, () -> {
			int moved = 0;
			for (int i = 0; i < 40; i++) {
				spot.put(player, -1.5, 0);
				Items.CHORUS_FRUIT.finishUsing(new ItemStack(Items.CHORUS_FRUIT), spot.realm, player);
				context.assertTrue(!spot.inside(player), "chorus fruit took the player in, to " + player.getBlockPos().toShortString());
				moved += player.getPos().distanceTo(spot.at(-1.5, 0)) > 1 ? 1 : 0;
			}
			context.assertTrue(moved >= 10, "chorus fruit moved the player only " + moved + " times in 40");
			finish(context, spot, player);
		}, player));
	}

	/** Gliding in on elytra: the glide stops and the player is back outside. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = BATCH, tickLimit = LIMIT)
	public void elytraGlidingInStops(TestContext context) {
		Spot spot = new Spot(context, 215);
		ServerPlayerEntity player = spot.join(new Recorder(), -3, 0);
		context.waitAndRun(2, () -> guard(context, spot, () -> {
			player.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
			spot.put(player, -3, 6);
			player.startFallFlying();
		}, player));
		context.waitAndRun(5, () -> guard(context, spot, () -> {
			context.assertTrue(player.isFallFlying(), "not gliding outside");
			spot.put(player, 2, 6);
			context.assertTrue(spot.inside(player), "the glide did not reach the final zone");
		}, player));
		context.waitAndRun(8, () -> guard(context, spot, () -> {
			context.assertTrue(!player.isFallFlying(), "still gliding");
			context.assertTrue(!spot.inside(player), "still in the final zone at " + player.getBlockPos().toShortString());
			finish(context, spot, player);
		}, player));
	}

	/** Riptide in the rain or water: refused when aimed into the final zone, thrown when aimed away. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = BATCH, tickLimit = LIMIT)
	public void riptideIntoTheFinalZoneIsRefused(TestContext context) {
		Spot spot = new Spot(context, 240);
		ServerPlayerEntity player = spot.join(new Recorder(), -3, 0);
		context.waitAndRun(3, () -> guard(context, spot, () -> {
			BlockPos feet = BlockPos.ofFloored(spot.at(-3, 0));
			spot.realm.setBlockState(feet, Blocks.WATER.getDefaultState());
			spot.put(player, -3, 0);
			((EntityAccessor) player).embervoid$updateWaterState();
			context.assertTrue(player.isTouchingWaterOrRain(), "the player is not in the water");
			ItemStack trident = new ItemStack(Items.TRIDENT);
			trident.addEnchantment(Enchantments.RIPTIDE, 3);
			int charged = Items.TRIDENT.getMaxUseTime(trident) - 20;

			spot.face(player, 1);
			player.setVelocity(Vec3d.ZERO);
			Items.TRIDENT.onStoppedUsing(trident, spot.realm, player, charged);
			context.assertTrue(!player.isUsingRiptide() && player.getVelocity().lengthSquared() < 1e-6,
					"riptide thrown into the final zone: " + player.getVelocity());

			spot.face(player, -1);
			boolean refused = FinalZoneLock.refusesRiptide(player, trident);
			Items.TRIDENT.onStoppedUsing(trident, spot.realm, player, charged);
			context.assertTrue(player.isUsingRiptide(), "riptide aimed away was refused too (by the lock: " + refused
					+ ", in water: " + player.isTouchingWaterOrRain() + ", look " + player.getRotationVec(1) + ", velocity "
					+ player.getVelocity() + ")");
			spot.realm.setBlockState(feet, Blocks.AIR.getDefaultState());
			finish(context, spot, player);
		}, player));
	}

	/** A boat and a horse ridden in: both go back out, their riders still on them. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = BATCH, tickLimit = LIMIT)
	public void aBoatOrHorseGoesBackOutWithItsRider(TestContext context) {
		Spot spot = new Spot(context, 265);
		ServerPlayerEntity sailor = spot.join(new Recorder(), -3, -4);
		ServerPlayerEntity rider = spot.join(new Recorder(), -3, 4);
		Entity[] mounts = new Entity[2];
		context.waitAndRun(3, () -> guard(context, spot, () -> {
			BoatEntity boat = new BoatEntity(spot.realm, 0, 0, 0);
			HorseEntity horse = EntityType.HORSE.create(spot.realm);
			horse.setTame(true);
			horse.saddle(SoundCategory.NEUTRAL);
			horse.setAiDisabled(true);
			mounts[0] = boat;
			mounts[1] = horse;
			spot.put(boat, -3, 0, -4);
			spot.put(horse, -3, 0, 4);
			spot.realm.spawnEntity(boat);
			spot.realm.spawnEntity(horse);
			context.assertTrue(sailor.startRiding(boat, true) && rider.startRiding(horse, true), "could not mount");
		}, sailor, rider));
		context.waitAndRun(6, () -> guard(context, spot, () -> {
			spot.put(mounts[0], 2, 0, -4);
			spot.put(mounts[1], 2, 0, 4);
			context.assertTrue(spot.inside(mounts[0]) && spot.inside(mounts[1]), "the mounts did not reach the final zone");
		}, sailor, rider));
		context.waitAndRun(10, () -> guard(context, spot, () -> {
			ServerPlayerEntity[] players = {sailor, rider};
			for (int i = 0; i < 2; i++) {
				Entity mount = mounts[i];
				context.assertTrue(!spot.inside(mount), mount.getType().getUntranslatedName() + " still inside at " + mount.getBlockPos().toShortString());
				context.assertTrue(players[i].getVehicle() == mount, "the rider fell off the " + mount.getType().getUntranslatedName());
				context.assertTrue(!spot.inside(players[i]), "the " + mount.getType().getUntranslatedName() + "'s rider is inside");
			}
			sailor.stopRiding();
			rider.stopRiding();
			mounts[0].discard();
			mounts[1].discard();
			finish(context, spot, sailor, rider);
		}, sailor, rider));
	}

	/** A bridge built high over the border wall: the far end is still the final zone, so back onto the bridge. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = BATCH, tickLimit = LIMIT)
	public void aBridgeOverTheWallLeadsNowhere(TestContext context) {
		Spot spot = new Spot(context, 290);
		int up = 14;
		for (double across = -5; across <= 5; across += 0.5) {
			for (double along = -1; along <= 1; along += 0.5) {
				spot.realm.setBlockState(BlockPos.ofFloored(spot.at(across, up - 1, along)), Blocks.STONE.getDefaultState());
			}
		}
		ServerPlayerEntity player = spot.join(new Recorder(), -3, up);
		context.waitAndRun(3, () -> guard(context, spot, () -> spot.put(player, 3, up), player));
		context.waitAndRun(6, () -> guard(context, spot, () -> {
			context.assertTrue(!spot.inside(player), "crossed the bridge into the final zone");
			context.assertTrue(Math.abs(player.getY() - spot.at(-3, up).y) < 0.01, "not back on the bridge: y " + player.getY());
			finish(context, spot, player);
		}, player));
	}

	/** A tunnel dug under the border wall: the far end is still the final zone, so back into the tunnel. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = BATCH, tickLimit = LIMIT)
	public void aTunnelUnderTheWallLeadsNowhere(TestContext context) {
		Spot spot = new Spot(context, 315);
		int down = -10;
		for (double across = -6; across <= 6; across += 0.5) {
			for (double along = -2; along <= 2; along += 0.5) {
				for (int dy = down - 2; dy <= down + 3; dy++) {
					boolean hole = Math.abs(along) < 0.6 && Math.abs(across) < 5 && dy >= down && dy <= down + 1;
					spot.realm.setBlockState(BlockPos.ofFloored(spot.at(across, dy, along)),
							hole ? Blocks.AIR.getDefaultState() : Blocks.STONE.getDefaultState());
				}
			}
		}
		ServerPlayerEntity player = spot.join(new Recorder(), -3, down);
		context.waitAndRun(3, () -> guard(context, spot, () -> spot.put(player, 3, down), player));
		context.waitAndRun(6, () -> guard(context, spot, () -> {
			context.assertTrue(!spot.inside(player), "came out of the tunnel in the final zone");
			context.assertTrue(Math.abs(player.getY() - spot.at(-3, down).y) < 0.01, "not back in the tunnel: y " + player.getY());
			finish(context, spot, player);
		}, player));
	}

	/**
	 * A player who has beaten the three zone bosses walks through the Citadel Gate's fog wall, which stays solid for
	 * one who has not, and goes on into the final zone.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = BATCH, tickLimit = LIMIT)
	public void aClearedPlayerPassesTheGateAndTheBorder(TestContext context) {
		Spot spot = new Spot(context, 340);
		ServerPlayerEntity cleared = spot.join(new Recorder(), -3, -2);
		ServerPlayerEntity locked = spot.join(new Recorder(), -3, 2);
		clear(cleared);
		BlockPos gate = BlockPos.ofFloored(spot.at(-6, 0, 6));
		spot.realm.setBlockState(gate, RealmBlocks.CITADEL_GATE.getDefaultState());
		BlockState state = spot.realm.getBlockState(gate);
		context.assertTrue(state.getCollisionShape(spot.realm, gate, ShapeContext.of(cleared)).isEmpty(),
				"the gate is solid for a cleared player");
		context.assertTrue(!state.getCollisionShape(spot.realm, gate, ShapeContext.of(locked)).isEmpty(),
				"the gate is open for a player without clearance");
		context.waitAndRun(3, () -> guard(context, spot, () -> spot.put(cleared, 3, 0, -2), cleared, locked));
		context.waitAndRun(8, () -> guard(context, spot, () -> {
			context.assertTrue(spot.inside(cleared) && cleared.getPos().distanceTo(spot.at(3, 0, -2)) < 0.01,
					"the cleared player was moved, to " + cleared.getPos());
			spot.realm.setBlockState(gate, Blocks.AIR.getDefaultState());
			finish(context, spot, cleared, locked);
		}, cleared, locked));
	}

	/** A creative player without clearance passes. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = BATCH, tickLimit = LIMIT)
	public void aCreativePlayerPasses(TestContext context) {
		Spot spot = new Spot(context, 365);
		ServerPlayerEntity player = spot.join(new Recorder(), -3, 0);
		player.changeGameMode(GameMode.CREATIVE);
		context.waitAndRun(3, () -> guard(context, spot, () -> spot.put(player, 3, 0), player));
		context.waitAndRun(8, () -> guard(context, spot, () -> {
			context.assertTrue(spot.inside(player) && player.getPos().distanceTo(spot.at(3, 0)) < 0.01,
					"the creative player was moved, to " + player.getPos());
			finish(context, spot, player);
		}, player));
	}

	/**
	 * Two players put at one spot inside (no step from outside to go back to): the one with clearance stays, the other
	 * goes out across the nearest border, {@link FinalZoneLock#EXIT_MARGIN} past it.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = BATCH, tickLimit = LIMIT)
	public void twoPlayersAtOneSpotAreToldApart(TestContext context) {
		Spot spot = new Spot(context, 390);
		ServerPlayerEntity cleared = spot.join(new Recorder(), 3, 0);
		ServerPlayerEntity locked = spot.join(new Recorder(), 3, 0);
		clear(cleared);
		context.waitAndRun(6, () -> guard(context, spot, () -> {
			context.assertTrue(spot.inside(cleared) && cleared.getPos().distanceTo(spot.at(3, 0)) < 0.01,
					"the cleared player was moved, to " + cleared.getPos());
			context.assertTrue(!spot.inside(locked), "the player without clearance is still inside");
			double[] at = spot.coordinates(locked);
			context.assertTrue(Math.abs(at[1] + FinalZoneLock.EXIT_MARGIN) < 0.01 && Math.abs(at[0]) < 0.01,
					"not just across the border: along " + at[0] + ", across " + at[1]);
			finish(context, spot, cleared, locked);
		}, cleared, locked));
	}

	/**
	 * The burnt city wall stands on both borders as generated (PLAN 21.2): on the border line, every stretch the layout
	 * puts up is there, the gaps are open.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = BATCH, tickLimit = LIMIT)
	public void theBorderWallStandsOnBothBorders(TestContext context) {
		ServerWorld realm = context.getWorld().getServer().getWorld(Realms.ASHEN_REACH.world());
		context.assertTrue(realm != null, "the Ashen Reach dimension is missing from the test world");
		double[] borders = BorderWallFeature.borders(Realms.ASHEN_REACH);
		int expected = 0;
		int standing = 0;
		int gapsOpen = 0;
		int gaps = 0;
		for (int border = 0; border < borders.length; border++) {
			for (double along = 520.5; along < 600; along += 1) {
				BlockPos column = BlockPos.ofFloored(Math.cos(borders[border]) * along, 0, Math.sin(borders[border]) * along);
				realm.getChunk(column.getX() >> 4, column.getZ() >> 4);
				boolean wall = BorderWallFeature.standsAt(realm, column.getX(), column.getZ());
				if (EdgeLayout.borderWallHeight(realm.getSeed(), border, along) > 0 || EdgeLayout.isBorderTower(along)) {
					expected++;
					standing += wall ? 1 : 0;
				} else {
					gaps++;
					gapsOpen += wall ? 0 : 1;
				}
			}
		}
		context.assertTrue(expected > 100 && standing >= expected - 2, "the wall stands at " + standing + " of " + expected + " columns");
		context.assertTrue(gaps > 5 && gapsOpen >= gaps - 2, "only " + gapsOpen + " of " + gaps + " gap columns open");
		context.complete();
	}

	private static void clear(ServerPlayerEntity player) {
		for (Identifier boss : Realms.ASHEN_REACH.zoneBosses()) {
			ProgressManager.markBossBeaten(player, boss);
		}
	}

	/** Runs a step; on a failure the players leave and the spot's chunks are let go before the test fails. */
	private static void guard(TestContext context, Spot spot, Runnable step, ServerPlayerEntity... players) {
		try {
			step.run();
		} catch (RuntimeException | AssertionError e) {
			for (ServerPlayerEntity player : players) {
				player.stopRiding();
				if (player.getServer().getPlayerManager().getPlayer(player.getUuid()) == player) {
					player.getServer().getPlayerManager().remove(player);
				}
			}
			spot.release();
			throw e;
		}
	}

	/** Lets the spot go, then the players, and completes the test. */
	private static void finish(TestContext context, Spot spot, ServerPlayerEntity... players) {
		spot.release();
		TestPlayers.complete(context, players);
	}

	/**
	 * A stretch of the final zone's 60° border {@code along} blocks out from the landing, its chunks loaded and
	 * forced: a stone floor across it a little above the ground, open air above. Across is positive into the final zone.
	 */
	private static final class Spot {
		/** How many spots hold each forced chunk: neighbouring spots share chunks, the last one lets a chunk go. */
		private static final Map<ChunkPos, Integer> FORCED = new HashMap<>();

		final ServerWorld realm;
		final FinalZoneRegion region;
		final TestContext context;
		final double angle;
		final double along;
		final int floor;
		private final List<ChunkPos> forced = new ArrayList<>();

		Spot(TestContext context, double along) {
			this.context = context;
			this.realm = context.getWorld().getServer().getWorld(Realms.ASHEN_REACH.world());
			context.assertTrue(realm != null, "the Ashen Reach dimension is missing from the test world");
			this.region = FinalZoneLock.region(Realms.ASHEN_REACH);
			this.angle = Realms.ASHEN_REACH.sectors().finalStart();
			this.along = along;
			int top = realm.getBottomY();
			for (int dx = -10; dx <= 10; dx++) {
				for (int dz = -10; dz <= 10; dz++) {
					BlockPos column = BlockPos.ofFloored(at(dx, 0, dz).x, 0, at(dx, 0, dz).z);
					ChunkPos chunk = new ChunkPos(column);
					if (!forced.contains(chunk)) {
						realm.getChunk(chunk.x, chunk.z);
						if (FORCED.merge(chunk, 1, Integer::sum) == 1) {
							realm.setChunkForced(chunk.x, chunk.z, true);
						}
						forced.add(chunk);
					}
					top = Math.max(top, realm.getTopY(Heightmap.Type.MOTION_BLOCKING, column.getX(), column.getZ()));
				}
			}
			this.floor = top + 1;
			Vec3d middle = at(0, 0);
			BlockPos centre = BlockPos.ofFloored(middle.x, floor, middle.z);
			for (BlockPos pos : BlockPos.iterate(centre.add(-8, 0, -8), centre.add(8, 0, 8))) {
				realm.setBlockState(pos, Blocks.STONE.getDefaultState());
			}
			for (BlockPos pos : BlockPos.iterate(centre.add(-8, 1, -8), centre.add(8, 20, 8))) {
				realm.setBlockState(pos, Blocks.AIR.getDefaultState());
			}
			realm.getEntitiesByClass(ItemEntity.class, box(16), item -> true).forEach(Entity::discard);
		}

		/** The point {@code across} blocks off the border, {@code up} blocks above the floor's top. */
		Vec3d at(double across, double up) {
			return at(across, up, 0);
		}

		/** As {@link #at(double, double)}, {@code shift} blocks further out along the border. */
		Vec3d at(double across, double up, double shift) {
			double cos = Math.cos(angle);
			double sin = Math.sin(angle);
			double a = along + shift;
			return new Vec3d(cos * a - sin * across, floor + 1 + up, sin * a + cos * across);
		}

		/** {along from this spot, across} of an entity. */
		double[] coordinates(Entity entity) {
			double cos = Math.cos(angle);
			double sin = Math.sin(angle);
			return new double[]{entity.getX() * cos + entity.getZ() * sin - along, -entity.getX() * sin + entity.getZ() * cos};
		}

		boolean inside(Entity entity) {
			return region.contains(entity.getX(), entity.getZ());
		}

		Box box(double radius) {
			Vec3d middle = at(0, 0);
			return new Box(middle.add(-radius, -radius, -radius), middle.add(radius, radius, radius));
		}

		/** A survival player joined and standing {@code across} blocks off the border, {@code up} above the floor. */
		ServerPlayerEntity join(ClientConnection connection, double across, double up) {
			ServerPlayerEntity player = TestPlayers.join(context, 1, 1, connection);
			Vec3d pos = at(across, up);
			player.teleport(realm, pos.x, pos.y, pos.z, 0, 0);
			return player;
		}

		void put(ServerPlayerEntity player, double across, double up) {
			put(player, across, up, 0);
		}

		void put(ServerPlayerEntity player, double across, double up, double shift) {
			Vec3d pos = at(across, up, shift);
			player.networkHandler.requestTeleport(pos.x, pos.y, pos.z, player.getYaw(), player.getPitch());
		}

		void put(Entity entity, double across, double up, double shift) {
			Vec3d pos = at(across, up, shift);
			entity.refreshPositionAndAngles(pos.x, pos.y, pos.z, entity.getYaw(), entity.getPitch());
			if (entity.getWorld() == realm && !entity.getPassengerList().isEmpty()) {
				entity.requestTeleport(pos.x, pos.y, pos.z);
			}
		}

		/** Turns the player to look across the border: into the final zone ({@code sign} 1) or away from it (-1). */
		void face(ServerPlayerEntity player, int sign) {
			double dx = -Math.sin(angle) * sign;
			double dz = Math.cos(angle) * sign;
			player.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
			player.setPitch(0);
		}

		/** A pearl thrown by {@code owner} from just outside, flying straight across the border. */
		EnderPearlEntity throwPearl(ServerPlayerEntity owner, double shift) {
			EnderPearlEntity pearl = new EnderPearlEntity(realm, owner);
			pearl.setItem(new ItemStack(Items.ENDER_PEARL));
			Vec3d from = at(-2.5, 2, shift);
			pearl.refreshPositionAndAngles(from.x, from.y, from.z, 0, 0);
			pearl.setVelocity(-Math.sin(angle) * 0.8, 0.15, Math.cos(angle) * 0.8);
			realm.spawnEntity(pearl);
			return pearl;
		}

		/** Runs {@code then} once every chunk of the spot ticks entities (forcing takes a few ticks to get there). */
		void whenTicking(int limit, Runnable then) {
			boolean ticking = forced.stream().allMatch(chunk -> realm.shouldTickEntity(chunk.getStartPos()));
			if (ticking) {
				then.run();
			} else if (limit <= 0) {
				release();
				throw new GameTestException("the spot's chunks never ticked entities");
			} else {
				context.waitAndRun(1, () -> whenTicking(limit - 1, then));
			}
		}

		/** Lets the forced chunks go and clears what a test left behind. */
		void release() {
			realm.getEntitiesByClass(ItemEntity.class, box(16), item -> true).forEach(Entity::discard);
			realm.getEntitiesByClass(EndermiteEntity.class, box(48), mite -> true).forEach(Entity::discard);
			for (ChunkPos chunk : forced) {
				if (FORCED.merge(chunk, -1, Integer::sum) == 0) {
					FORCED.remove(chunk);
					realm.setChunkForced(chunk.x, chunk.z, false);
				}
			}
			forced.clear();
		}
	}

	/** A connection that keeps the overlay messages (their translation keys) and the sounds sent to the player. */
	static final class Recorder extends ClientConnection {
		final List<String> messages = new ArrayList<>();
		final List<Identifier> sounds = new ArrayList<>();

		Recorder() {
			super(NetworkSide.SERVERBOUND);
		}

		@Override
		public void send(Packet<?> packet) {
			send(packet, null);
		}

		@Override
		public void send(Packet<?> packet, @Nullable PacketCallbacks callbacks) {
			if (packet instanceof GameMessageS2CPacket message && message.content().getContent() instanceof TranslatableTextContent text) {
				messages.add(text.getKey());
			} else if (packet instanceof PlaySoundS2CPacket sound) {
				sounds.add(sound.getSound().value().getId());
			}
		}
	}
}
