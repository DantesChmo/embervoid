package com.embervoid.boss;

import com.embervoid.Embervoid;
import com.embervoid.mob.MobLevels;
import com.embervoid.progress.ProgressManager;
import com.embervoid.realm.RealmDefinition;
import com.embervoid.realm.Realms;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * One running boss fight in one arena (DESIGN §4.3).
 *
 * <ul>
 *   <li>Started by {@link ArenaManager} when a player who has not beaten the boss enters; everyone inside joins.</li>
 *   <li>Fog wall: a cylinder at the arena radius. Fighters cannot walk out, others cannot walk in.</li>
 *   <li>A fighter leaves by dying, logging out or getting out some other way (pearl, teleport). With no fighters
 *   left the fight resets: the boss is removed and comes back whole for the next attempt.</li>
 *   <li>On the kill, every fighter still inside gets credit; a first kill gives that player their own Ember Heart.</li>
 * </ul>
 */
public class BossFight {
	/** Fog wall particles are drawn this often, in ticks. */
	private static final int FOG_INTERVAL = 10;
	/** Fog wall height above the arena floor. */
	private static final int FOG_HEIGHT = 4;

	private final ServerWorld world;
	private final ArenaSpec spec;
	/** Arena floor center. */
	private final BlockPos center;
	private final ServerBossBar bar;
	private final Set<UUID> fighters = new LinkedHashSet<>();
	private UUID bossUuid;
	@Nullable
	private Identifier music;
	private int phase;
	private boolean ended;

	BossFight(ServerWorld world, ArenaSpec spec, BlockPos center) {
		this.world = world;
		this.spec = spec;
		this.center = center;
		this.bar = new ServerBossBar(barName(), BossBar.Color.RED, BossBar.Style.NOTCHED_10);
	}

	public ServerWorld world() {
		return world;
	}

	public ArenaSpec spec() {
		return spec;
	}

	public BlockPos center() {
		return center;
	}

	public boolean isEnded() {
		return ended;
	}

	@Nullable
	public UUID bossUuid() {
		return bossUuid;
	}

	private Text bossName() {
		return Text.translatable(EmberHeartItem.bossKey(spec.bossId()));
	}

	private Text barName() {
		return Text.translatable("embervoid.boss.bar", bossName(), spec.level());
	}

	/** Summons the boss; everyone inside the arena becomes a fighter. Returns false if the boss cannot be created. */
	boolean start() {
		Entity entity = spec.entity().create(world);
		if (!(entity instanceof MobEntity boss) || !(entity instanceof ArenaBoss)) {
			if (entity != null) {
				entity.discard();
			}
			Embervoid.LOGGER.warn("Arena boss {} is not an ArenaBoss mob: {}", spec.bossId(), spec.entity());
			return false;
		}
		boss.refreshPositionAndAngles(center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5, world.random.nextFloat() * 360, 0);
		boss.setPersistent();
		boss.setCustomName(bossName());
		bossUuid = boss.getUuid();
		music = ((ArenaBoss) boss).music();
		for (ServerPlayerEntity player : world.getPlayers()) {
			if (isInside(player.getPos(), 0) && !player.isSpectator()) {
				addFighter(player);
			}
		}
		world.spawnEntity(boss);
		MobLevels.setLevel(boss, spec.level());
		world.playSound(null, center, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 1.0f, 0.8f);
		((ArenaBoss) boss).onPhaseChange(0);
		return true;
	}

	private void addFighter(ServerPlayerEntity player) {
		if (fighters.add(player.getUuid())) {
			bar.addPlayer(player);
			BossMusic.play(player, music);
		}
	}

	@Nullable
	private Entity boss() {
		return bossUuid == null ? null : world.getEntity(bossUuid);
	}

	/** Horizontal check against the fog wall radius (plus a margin), within the arena's height band. */
	private boolean isInside(Vec3d pos, double margin) {
		double dx = pos.x - (center.getX() + 0.5);
		double dz = pos.z - (center.getZ() + 0.5);
		return dx * dx + dz * dz <= (spec.radius() + margin) * (spec.radius() + margin)
				&& pos.y > center.getY() - 3 && pos.y < center.getY() + 20;
	}

	void tick() {
		if (ended) {
			return;
		}
		Entity bossEntity = boss();
		if (!(bossEntity instanceof MobEntity boss) || bossEntity.isRemoved()) {
			// Gone without dying (chunk unloaded, killed by a command that removes, ...): start over.
			reset();
			return;
		}

		// Fighters who died, logged out or got out some other way have left the fight.
		List<ServerPlayerEntity> present = new ArrayList<>();
		for (UUID uuid : List.copyOf(fighters)) {
			ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
			if (player == null || player.getServerWorld() != world || !player.isAlive() || !isInside(player.getPos(), 2)) {
				fighters.remove(uuid);
				if (player != null) {
					bar.removePlayer(player);
					BossMusic.stop(player);
				}
			} else {
				present.add(player);
			}
		}
		if (present.isEmpty()) {
			reset();
			return;
		}

		holdFogWall();
		if (!isInside(boss.getPos(), -1)) {
			boss.requestTeleport(center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5);
		}

		float fraction = boss.getHealth() / boss.getMaxHealth();
		bar.setPercent(fraction);
		float[] thresholds = ((ArenaBoss) boss).phaseThresholds();
		while (phase < thresholds.length && fraction <= thresholds[phase]) {
			phase++;
			((ArenaBoss) boss).onPhaseChange(phase);
		}
	}

	/** Pushes fighters back in and everyone else back out; draws the wall near the arena edge. */
	private void holdFogWall() {
		double cx = center.getX() + 0.5;
		double cz = center.getZ() + 0.5;
		double r = spec.radius();
		for (ServerPlayerEntity player : world.getPlayers()) {
			if (player.isSpectator() || player.isCreative() || !isInside(player.getPos(), 6)) {
				continue;
			}
			double dx = player.getX() - cx;
			double dz = player.getZ() - cz;
			double distance = Math.sqrt(dx * dx + dz * dz);
			boolean fighter = fighters.contains(player.getUuid());
			if (distance < 0.01 || (fighter ? distance < r - 0.7 : distance > r + 0.7)) {
				continue;
			}
			double sign = fighter ? -1 : 1;
			player.setVelocity(sign * dx / distance * 0.6, Math.max(player.getVelocity().y, 0.1), sign * dz / distance * 0.6);
			player.velocityModified = true;
			if (!fighter) {
				player.sendMessage(Text.translatable("embervoid.boss.fog"), true);
			}
		}
		if (world.getTime() % FOG_INTERVAL == 0) {
			int points = (int) (r * 4);
			for (int i = 0; i < points; i++) {
				double angle = Math.PI * 2 * i / points;
				for (int y = 1; y <= FOG_HEIGHT; y++) {
					world.spawnParticles(ParticleTypes.WHITE_ASH, cx + Math.cos(angle) * r, center.getY() + y, cz + Math.sin(angle) * r,
							2, 0.2, 0.4, 0.2, 0);
				}
			}
		}
	}

	/** The boss died: credit the fighters still inside, hand out Ember Hearts, mark realms completed. */
	void onBossDeath() {
		if (ended) {
			return;
		}
		RealmDefinition realm = Realms.byWorld(world.getRegistryKey());
		boolean finalBoss = realm != null && realm.finalBoss().equals(spec.bossId());
		for (UUID uuid : fighters) {
			ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
			if (player == null || player.getServerWorld() != world || !player.isAlive() || !isInside(player.getPos(), 2)) {
				continue;
			}
			if (ProgressManager.markBossBeaten(player, spec.bossId())) {
				// Personal loot: straight into this player's inventory, nobody else can pick it up.
				player.getInventory().offerOrDrop(EmberHeartItem.of(spec.bossId()));
				player.sendMessage(Text.translatable("embervoid.boss.first_kill", bossName()), false);
				player.sendMessage(Text.translatable("embervoid.shrine.kindled"), true);
			} else {
				player.sendMessage(Text.translatable("embervoid.boss.victory", bossName()), true);
			}
			if (finalBoss) {
				completeRealm(player, realm);
			}
		}
		end();
	}

	private static void completeRealm(ServerPlayerEntity player, RealmDefinition realm) {
		ProgressManager.completeRealm(player, realm.id());
		int index = Realms.ALL.indexOf(realm);
		if (index >= 0 && index + 1 < Realms.ALL.size()) {
			ProgressManager.unlockRealm(player, Realms.ALL.get(index + 1).id());
		}
		player.sendMessage(Text.translatable("embervoid.realm.completed"), false);
	}

	/** Ends the fight without a kill: the boss goes away and comes back whole next time. */
	void reset() {
		Entity boss = boss();
		if (boss != null) {
			boss.discard();
		}
		end();
	}

	private void end() {
		ended = true;
		for (ServerPlayerEntity player : List.copyOf(bar.getPlayers())) {
			BossMusic.stop(player);
		}
		bar.clearPlayers();
	}

	@Override
	public String toString() {
		return spec.bossId() + " at " + center.toShortString() + ", fighters " + fighters.size() + ", phase " + phase;
	}

	/** Id of the boss this fight is for. */
	public Identifier bossId() {
		return spec.bossId();
	}
}
