package com.embervoid.gametest;

import com.embervoid.fx.CombatFx;
import com.embervoid.fx.SwingArc;
import com.embervoid.reach.boss.AshColossusEntity;
import com.embervoid.reach.boss.ReachBossEntities;
import com.embervoid.reach.boss.ReachBosses;
import com.embervoid.reach.boss.SmolderingLordEntity;
import com.embervoid.reach.boss.arena.ReachArenas;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Combat effects (PLAN 19.2): the packets reach the players near enough and nobody else, and the heavy moves wired so
 * far send them when they land (the Ash Colossus leap and arm drag, the Smoldering Lord's landing and greatsword slam;
 * the Vampire boss leap is in {@link VampireTests}). How the effects look is not checked.
 */
public final class CombatFxTests implements FabricGameTest {
	private static final int FLOOR_Y = 120;
	/** A row of far spots of its own, apart from the boss tests' rows. */
	private static final int SPOTS_Z = 27000;
	/** An outsider stands this far from the fight: beyond every effect's reach. */
	private static final int OUTSIDER_DISTANCE = 120;

	/** Shake to within its radius, flashes and trails to within {@link CombatFx#RANGE}, a player's own shake to them alone. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combat_fx_range", tickLimit = 40)
	public void sentOnlyToPlayersInRange(TestContext context) {
		BossArena arena = new BossArena(context, null);
		arena.step(() -> {
			arena.floor(2, 2, 1);
			BossArena.FogRecorder near = new BossArena.FogRecorder();
			BossArena.FogRecorder mid = new BossArena.FogRecorder();
			BossArena.FogRecorder far = new BossArena.FogRecorder();
			ServerPlayerEntity nearPlayer = arena.join(2, 2, near);
			ServerPlayerEntity midPlayer = arena.join(2, 2 + 30, mid);
			arena.join(2, 2 + 100, far);
			ServerWorld world = context.getWorld();
			Vec3d at = nearPlayer.getPos();
			SwingArc arc = SwingArc.overhead(3.5f, 2.6f, 6, CombatFx.EMBER);

			CombatFx.shake(world, at, 1, 20, 16);
			CombatFx.flash(world, at, 2, CombatFx.EMBER);
			CombatFx.trail(nearPlayer, arc);
			CombatFx.shake(midPlayer, 0.5f, 10);

			context.assertTrue(near.fx(CombatFx.SHAKE_PACKET) == 1 && near.fx(CombatFx.FLASH_PACKET) == 1 && near.fx(CombatFx.TRAIL_PACKET) == 1,
					"near: " + channels(near));
			context.assertTrue(mid.fx(CombatFx.SHAKE_PACKET) == 1 && mid.fx(CombatFx.FLASH_PACKET) == 1 && mid.fx(CombatFx.TRAIL_PACKET) == 1,
					"30 blocks out: " + channels(mid));
			context.assertTrue(far.fx.isEmpty(), "100 blocks out: " + channels(far));

			PacketByteBuf shake = data(near, CombatFx.SHAKE_PACKET);
			context.assertTrue(shake.readFloat() == 1 && shake.readVarInt() == 20 && shake.readBoolean(), "the near shake's strength, ticks or source");
			Vec3d source = new Vec3d(shake.readDouble(), shake.readDouble(), shake.readDouble());
			context.assertTrue(source.equals(at) && shake.readFloat() == 16, "shake source " + source + ", expected " + at);
			PacketByteBuf own = data(mid, CombatFx.SHAKE_PACKET);
			context.assertTrue(own.readFloat() == 0.5f && own.readVarInt() == 10 && !own.readBoolean(), "the player's own shake has a source");
			PacketByteBuf trail = data(mid, CombatFx.TRAIL_PACKET);
			context.assertTrue(trail.readVarInt() == nearPlayer.getId(), "the trail follows someone else");
			trail.readFloat();
			trail.readDouble();
			trail.readDouble();
			trail.readDouble();
			context.assertTrue(SwingArc.read(trail).equals(arc), "the arc changed on the way");
			arena.finish();
		});
	}

	/** The leap's landing shakes and flashes for the fighter (not in the windup); the arm drag leaves a trail. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combat_fx_colossus", tickLimit = PlacedFight.LOAD_LIMIT + 400)
	public void colossusLeapAndDrag(TestContext context) {
		PlacedFight.start(context, ReachArenas.FIELDS, new BlockPos(0, FLOOR_Y, SPOTS_Z), "ashen_reach/zone_a",
				ReachBossEntities.ASH_COLOSSUS, 18, new int[][]{{0, 0, 14}}, f -> {
					AshColossusEntity boss = f.boss;
					ServerPlayerEntity player = f.player(0);
					BossArena.FogRecorder fighter = f.fog.get(0);
					BossArena.FogRecorder outsider = outsider(f);
					// The landing alone would kill them, and the fight (the drag with it) would end.
					player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 1000, 3));
					f.arena.after(BossArena.JOIN_INVULNERABLE_TICKS, () -> {
						context.assertTrue(fighter.fx.isEmpty(), "effects before any move: " + channels(fighter));
						boss.leap(player);
					});
					f.arena.after(BossArena.JOIN_INVULNERABLE_TICKS + 10, () ->
							context.assertTrue(fighter.fx.isEmpty(), "effects in the leap's windup: " + channels(fighter)));
					f.arena.after(BossArena.JOIN_INVULNERABLE_TICKS + 80, () -> {
						context.assertTrue(fighter.fx(CombatFx.SHAKE_PACKET) == 1 && fighter.fx(CombatFx.FLASH_PACKET) == 1,
								"the landing: " + channels(fighter) + ", boss on ground " + boss.isOnGround());
						boss.drag(player);
					});
					f.arena.after(BossArena.JOIN_INVULNERABLE_TICKS + 110, () -> {
						context.assertTrue(fighter.fx(CombatFx.TRAIL_PACKET) == 1, "the drag: " + channels(fighter));
						context.assertTrue(outsider.fx.isEmpty(), "sent to a player " + OUTSIDER_DISTANCE + " blocks away: " + channels(outsider));
						f.arena.finish();
					});
				});
	}

	/** Coming down from the wall shakes and flashes; the greatsword slam adds an overhead trail, a shake and a flash. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combat_fx_lord", tickLimit = PlacedFight.LOAD_LIMIT + 400)
	public void lordLandingAndSlam(TestContext context) {
		PlacedFight.start(context, ReachArenas.CITADEL, new BlockPos(500, FLOOR_Y, SPOTS_Z), ReachBosses.CITADEL.getPath(),
				ReachBossEntities.SMOLDERING_LORD, 30, new int[][]{{0, 0, -3}}, f -> {
					SmolderingLordEntity lord = f.boss;
					ServerPlayerEntity near = f.player(0);
					BossArena.FogRecorder fighter = f.fog.get(0);
					BossArena.FogRecorder outsider = outsider(f);
					near.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 1000, 3));
					context.assertTrue(fighter.fx.isEmpty(), "effects while he waits on the wall: " + channels(fighter));
					lord.braziers().forEach(b -> f.world().setBlockState(b, f.world().getBlockState(b).with(CampfireBlock.LIT, true)));
					f.arena.await(() -> !lord.isOnWall() && lord.getY() < f.fight.center().getY() + 3, 80, "the Lord to come down", () -> {
						context.assertTrue(fighter.fx(CombatFx.SHAKE_PACKET) == 1 && fighter.fx(CombatFx.FLASH_PACKET) == 1,
								"the landing: " + channels(fighter));
						// The landing sets his move cooldown: hold him, so the only swing is the test's.
						lord.setAiDisabled(true);
						lord.holdMoves(Integer.MAX_VALUE);
						f.arena.after(BossArena.JOIN_INVULNERABLE_TICKS, () -> {
							context.assertTrue(fighter.fx(CombatFx.TRAIL_PACKET) == 0, "trails before the slam: " + channels(fighter));
							lord.comboSwing(near, 3);
						});
						f.arena.after(BossArena.JOIN_INVULNERABLE_TICKS + 30, () -> {
							context.assertTrue(fighter.fx(CombatFx.TRAIL_PACKET) == 1 && fighter.fx(CombatFx.SHAKE_PACKET) == 2
									&& fighter.fx(CombatFx.FLASH_PACKET) == 2, "the slam: " + channels(fighter));
							context.assertTrue(outsider.fx.isEmpty(), "sent to a player " + OUTSIDER_DISTANCE + " blocks away: " + channels(outsider));
							f.arena.finish();
						});
					});
				});
	}

	/** A player joined far beyond the fight, recording what reaches them. */
	private static BossArena.FogRecorder outsider(PlacedFight<?> f) {
		BossArena.FogRecorder recorder = new BossArena.FogRecorder();
		f.arena.joinAt(f.fight.centerPos().add(0, 0, OUTSIDER_DISTANCE), recorder);
		return recorder;
	}

	private static PacketByteBuf data(BossArena.FogRecorder recorder, Identifier channel) {
		CustomPayloadS2CPacket payload = recorder.fx.stream().filter(p -> p.getChannel().equals(channel)).findFirst().orElseThrow();
		return new PacketByteBuf(payload.getData().copy());
	}

	private static String channels(BossArena.FogRecorder recorder) {
		return recorder.fx.stream().map(p -> p.getChannel().getPath()).toList().toString();
	}
}
