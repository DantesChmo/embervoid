package com.embervoid.gametest;

import com.embervoid.Embervoid;
import com.embervoid.entity.AshRemnantEntity;
import com.embervoid.entity.mob.HollowEntity;
import com.embervoid.entity.mob.MobEntities;
import com.embervoid.entity.mob.MobSounds;
import com.embervoid.nightmob.GhostEntity;
import com.embervoid.nightmob.NightMobSounds;
import com.embervoid.nightmob.NightMobs;
import com.embervoid.player.PlayerDataManager;
import com.embervoid.player.PlayerSounds;
import com.embervoid.player.StaminaManager;
import com.embervoid.realm.RealmSounds;
import com.embervoid.realm.api.RealmTeleports;
import com.embervoid.registry.ModEntities;
import com.embervoid.registry.ModItems;
import com.embervoid.sanctum.AshenMaidenEntity;
import com.embervoid.sanctum.SanctumBlocks;
import com.embervoid.sanctum.SanctumEntities;
import com.embervoid.sanctum.SanctumItems;
import com.embervoid.sanctum.SanctumSounds;
import com.embervoid.vampire.VampireEntities;
import com.embervoid.vampire.VampireEntity;
import com.embervoid.vampire.VampireSounds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.test.GameTest;
import net.minecraft.test.GameTestException;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Sounds of the 0.2.0 / 0.3.0 content (PLAN L3): the overworld and moon-night mobs and the Vampire play their own
 * events (read from {@link SoundLog}); the Sanctum, the flask, the Ash Remnant, stamina and obelisk travel play theirs
 * to the player (read from the sound packets the player is sent).
 */
public final class OverworldSoundTests implements FabricGameTest {
	private static final double NEAR = 0.5;

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "sounds")
	public void overworldMobsPlayTheirOwnSounds(TestContext context) {
		List<String> problems = new ArrayList<>();
		voice(context, ReachTests.still(context, MobEntities.ASH_GHOUL, 2, 0, 2), MobSounds.ASH_GHOUL_AMBIENT,
				MobSounds.ASH_GHOUL_HURT, null, problems);
		voice(context, ReachTests.still(context, MobEntities.EMBER_WISP, 5, 0, 2), MobSounds.EMBER_WISP_AMBIENT,
				MobSounds.EMBER_WISP_HURT, null, problems);
		voice(context, ReachTests.still(context, NightMobs.ASHEN_ZOMBIE, 8, 0, 2), NightMobSounds.ASHEN_ZOMBIE_AMBIENT,
				NightMobSounds.ASHEN_ZOMBIE_HURT, null, problems);
		// A hit on a Ghost can pass through it: then it plays the miss instead of hurting.
		GhostEntity ghost = ReachTests.still(context, NightMobs.GHOST, 11, 1, 2);
		voice(context, ghost, NightMobSounds.GHOST_AMBIENT, NightMobSounds.GHOST_HURT, NightMobSounds.GHOST_PHASE, problems);

		HollowEntity hollow = ReachTests.still(context, MobEntities.HOLLOW, 14, 0, 2);
		voice(context, hollow, MobSounds.HOLLOW_AMBIENT, MobSounds.HOLLOW_HURT, null, problems);
		invoke(hollow, HollowEntity.class, "startSwing");
		expect(context, hollow, MobSounds.HOLLOW_SWING, "hollow swing", problems);

		VampireEntity vampire = ReachTests.still(context, VampireEntities.VAMPIRE, 17, 0, 2);
		SoundLog.forget(context.getWorld(), vampire.getPos(), 2);
		invoke(vampire, VampireEntity.class, "startTurning");
		expect(context, vampire, VampireSounds.VAMPIRE_TRANSFORM, "vampire turning", problems);
		invoke(vampire, VampireEntity.class, "vanish");
		expect(context, vampire, VampireSounds.VAMPIRE_VANISH, "vampire vanishing", problems);

		context.assertTrue(problems.isEmpty(), String.join("; ", problems));
		ReachTests.finish(context);
	}

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "sounds", tickLimit = 200)
	public void playerSystemsPlayTheirOwnSounds(TestContext context) {
		BossArena arena = new BossArena(context, null);
		arena.step(() -> {
			arena.floor(4, 4, 4);
			SoundRecorder heard = new SoundRecorder();
			ServerPlayerEntity player = arena.join(4, 4, heard);
			List<String> problems = new ArrayList<>();

			// The shrine and a Flask Shard offered at it.
			BlockPos shrine = new BlockPos(6, 1, 4);
			context.setBlockState(shrine, SanctumBlocks.EMBER_SHRINE);
			context.useBlock(shrine, player);
			heard.expect(SanctumSounds.SHRINE_REST, "shrine rest", problems);
			player.setStackInHand(Hand.MAIN_HAND, new ItemStack(SanctumItems.FLASK_SHARD));
			context.useBlock(shrine, player);
			heard.expect(SanctumSounds.FLASK_SHARD_OFFER, "flask shard offered", problems);

			// The flask: an empty sip, and its drinking sound.
			PlayerDataManager.get(player).flaskCharges = 0;
			player.setStackInHand(Hand.MAIN_HAND, new ItemStack(ModItems.HEALING_FLASK));
			ModItems.HEALING_FLASK.use(context.getWorld(), player, Hand.MAIN_HAND);
			heard.expect(PlayerSounds.FLASK_EMPTY, "empty flask", problems);
			if (ModItems.HEALING_FLASK.getDrinkSound() != PlayerSounds.FLASK_DRINK) {
				problems.add("the flask drinks with " + ModItems.HEALING_FLASK.getDrinkSound().getId());
			}

			// A special attack with and without stamina.
			Item sweep = Registries.ITEM.get(Embervoid.id("debug_sweep"));
			player.setStackInHand(Hand.MAIN_HAND, new ItemStack(sweep));
			sweep.use(context.getWorld(), player, Hand.MAIN_HAND);
			heard.expect(PlayerSounds.DEBUG_SWEEP, "debug sweep", problems);
			StaminaManager.set(player, 0);
			player.getItemCooldownManager().remove(sweep);
			sweep.use(context.getWorld(), player, Hand.MAIN_HAND);
			heard.expect(PlayerSounds.STAMINA_EMPTY, "no stamina", problems);

			// The Ashen Maiden turns to a player who talks to her.
			AshenMaidenEntity maiden = context.spawnMob(SanctumEntities.ASHEN_MAIDEN, 2, 1, 4);
			maiden.interact(player, Hand.MAIN_HAND);
			heard.expect(SanctumSounds.MAIDEN_GREET, "maiden greeting", problems);

			// A locked realm refuses; leaving travels.
			RealmTeleports.enterRealm(player, Embervoid.id("realm_1"), false);
			heard.expect(RealmSounds.REFUSE, "locked obelisk", problems);

			maiden.discard();

			// Death leaves a remnant with a sound; taking it back plays another (after the join invulnerability).
			arena.after(BossArena.JOIN_INVULNERABLE_TICKS, () -> {
				player.getInventory().setStack(5, new ItemStack(Items.DIAMOND));
				player.kill();
				heard.expect(PlayerSounds.REMNANT_PLACED, "remnant placed", problems);
				ServerPlayerEntity respawned = TestPlayers.respawn(player);
				List<? extends AshRemnantEntity> remnants = context.getWorld().getEntitiesByType(ModEntities.ASH_REMNANT,
						remnant -> respawned.getUuid().equals(remnant.getOwner()));
				context.assertTrue(remnants.size() == 1, "expected one remnant, found " + remnants.size());
				// Back to the remnant from the respawn point, like a player walking there.
				AshRemnantEntity remnant = remnants.get(0);
				respawned.teleport(context.getWorld(), remnant.getX() + 1, remnant.getY(), remnant.getZ(), 0, 0);
				remnant.interact(respawned, Hand.MAIN_HAND);
				heard.expect(PlayerSounds.REMNANT_CLAIMED, "remnant claimed", problems);

				RealmTeleports.leaveRealm(respawned);
				heard.expect(RealmSounds.TRAVEL, "obelisk travel", problems);
				context.assertTrue(problems.isEmpty(), String.join("; ", problems));
				arena.finish();
			});
		});
	}

	private static void voice(TestContext context, MobEntity mob, SoundEvent ambient, SoundEvent hurt, @Nullable SoundEvent orInstead,
			List<String> problems) {
		mob.setAiDisabled(true);
		SoundLog.forget(context.getWorld(), mob.getPos(), 2);
		String name = Registries.ENTITY_TYPE.getId(mob.getType()).getPath();
		mob.playAmbientSound();
		expect(context, mob, ambient, name + " ambient", problems);
		mob.damage(context.getWorld().getDamageSources().magic(), 1);
		if (orInstead == null || !SoundLog.heard(context.getWorld(), orInstead, mob.getPos(), NEAR)) {
			expect(context, mob, hurt, name + " hurt", problems);
		}
	}

	private static void expect(TestContext context, LivingEntity entity, SoundEvent sound, String what, List<String> problems) {
		if (!SoundLog.heard(context.getWorld(), sound, entity.getPos(), NEAR)) {
			problems.add(what + ": " + sound.getId() + " not heard");
		}
	}

	/** A package-private entity method without arguments (the tests live in another package). */
	private static void invoke(Object target, Class<?> type, String name) {
		try {
			Method method = type.getDeclaredMethod(name);
			method.setAccessible(true);
			method.invoke(target);
		} catch (ReflectiveOperationException e) {
			throw new GameTestException(type.getSimpleName() + "." + name + ": " + e);
		}
	}

	/** A connection that records the id of every sound its player is sent. */
	static final class SoundRecorder extends ClientConnection {
		final List<Identifier> sounds = new ArrayList<>();

		SoundRecorder() {
			super(NetworkSide.SERVERBOUND);
		}

		@Override
		public void send(Packet<?> packet) {
			send(packet, null);
		}

		@Override
		public void send(Packet<?> packet, @Nullable PacketCallbacks callbacks) {
			if (packet instanceof PlaySoundS2CPacket sound) {
				sounds.add(sound.getSound().value().getId());
			}
		}

		void expect(SoundEvent sound, String what, List<String> problems) {
			if (!sounds.contains(sound.getId())) {
				problems.add(what + ": " + sound.getId() + " not sent (" + sounds + ")");
			}
		}
	}
}
