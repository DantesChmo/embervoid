package com.embervoid.boss;

import com.embervoid.Embervoid;
import com.embervoid.block.setpiece.ArenaEvents;
import com.embervoid.mob.MobLevels;
import com.embervoid.progress.ProgressManager;
import com.embervoid.realm.RealmDefinition;
import com.embervoid.realm.Realms;
import com.embervoid.realm.worldgen.RealmFeatures;
import com.embervoid.registry.ModBlocks;
import com.embervoid.sanctum.SanctumBlocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * One running boss fight in one arena (DESIGN §4.3, §4.5).
 *
 * <ul>
 *   <li>Started by {@link ArenaManager} when a player who has not beaten the boss enters; everyone inside joins.
 *   With n fighters the boss has ×(1 + 0.5·(n − 1)) HP and brings n − 1 adds ({@link BossConfig}).</li>
 *   <li>Fog wall: a cylinder at the arena radius. Fighters cannot walk out, others cannot walk in.</li>
 *   <li>A fighter leaves by dying, logging out or getting out some other way (pearl, teleport). With no fighters
 *   left the fight resets: the boss is removed and comes back whole for the next attempt.</li>
 *   <li>The fight owns what the boss leaves in the arena: adds ({@link #spawnAdd}), hazards ({@link #addHazard}),
 *   changed blocks ({@link #blocks}) and arena fog ({@link #setFog}). All of it goes when the fight ends.</li>
 *   <li>Every 8–10 s the boss turns to the fighter who hurt it most meanwhile (or to another one).</li>
 *   <li>On the kill, every fighter still inside gets credit; a first kill gives that player their own personal loot
 *   ({@link BossReward}: the boss's soul and more, or else an Ember Heart).</li>
 * </ul>
 */
public class BossFight {
	/** Fog wall particles are drawn this often, in ticks. */
	private static final int FOG_INTERVAL = 10;
	/** Fog wall height above the arena floor. */
	private static final int FOG_HEIGHT = 4;
	private static final UUID FIGHTERS_HEALTH_ID = UUID.fromString("6f1b8c52-3a9e-4f0d-b2c1-8e7d5a4c3b21");

	private final ServerWorld world;
	private final ArenaSpec spec;
	/** Arena floor center. */
	private final BlockPos center;
	private final ServerBossBar bar;
	private final Set<UUID> fighters = new LinkedHashSet<>();
	private final List<UUID> adds = new ArrayList<>();
	private final List<ArenaHazard> hazards = new ArrayList<>();
	private final ArenaBlocks blocks;
	/** Damage each fighter dealt the boss since the last target switch. */
	private final Map<UUID, Float> damageDealt = new HashMap<>();
	private UUID bossUuid;
	@Nullable
	private Identifier music;
	private int phase;
	private boolean ended;
	private int switchIn;
	/** A boss summoned again with Ash of Rematch (PLAN 12.7): no soul for a repeat kill. */
	private boolean rematch;
	/** Arena fog now shown to the fighters: {density, colour}, or null. */
	private float @Nullable [] fog;

	BossFight(ServerWorld world, ArenaSpec spec, BlockPos center) {
		this.world = world;
		this.spec = spec;
		this.center = center;
		this.blocks = new ArenaBlocks(world);
		this.bar = new ServerBossBar(barName(), BossBar.Color.RED, BossBar.Style.NOTCHED_10);
	}

	/** The running fight of a boss entity, or null (client side, no fight, ended). */
	@Nullable
	public static BossFight of(Entity boss) {
		return boss.getWorld().isClient ? null : ArenaManager.fightOf(boss.getUuid());
	}

	public ServerWorld world() {
		return world;
	}

	public ArenaSpec spec() {
		return spec;
	}

	/** Arena floor center block. */
	public BlockPos center() {
		return center;
	}

	/** Top center of the arena floor block. */
	public Vec3d centerPos() {
		return new Vec3d(center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5);
	}

	public boolean isEnded() {
		return ended;
	}

	@Nullable
	public UUID bossUuid() {
		return bossUuid;
	}

	public int phase() {
		return phase;
	}

	/** Whether this fight was started with Ash of Rematch: a repeat kill never gives the soul. */
	public boolean isRematch() {
		return rematch;
	}

	void setRematch(boolean rematch) {
		this.rematch = rematch;
	}

	/** Real blocks the fight changes; put back when it ends. */
	public ArenaBlocks blocks() {
		return blocks;
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
		if (!(entity instanceof MobEntity boss) || !(entity instanceof ArenaBoss arenaBoss)) {
			if (entity != null) {
				entity.discard();
			}
			Embervoid.LOGGER.warn("Arena boss {} is not an ArenaBoss mob: {}", spec.bossId(), spec.entity());
			return false;
		}
		Vec3d spawn = standingSpot(boss);
		boss.refreshPositionAndAngles(spawn.x, spawn.y, spawn.z, world.random.nextFloat() * 360, 0);
		boss.setPersistent();
		boss.setCustomName(bossName());
		bossUuid = boss.getUuid();
		music = arenaBoss.music();
		for (ServerPlayerEntity player : world.getPlayers()) {
			if (isInside(player.getPos(), 0) && !player.isSpectator()) {
				addFighter(player);
			}
		}
		world.spawnEntity(boss);
		MobLevels.setLevel(boss, spec.level());
		scaleToFighters(boss, arenaBoss);
		world.playSound(null, center, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 1.0f, 0.8f);
		BossConfig config = BossConfig.get();
		switchIn = BossScaling.switchTicks(config.targetSwitchMinSeconds(), config.targetSwitchMaxSeconds(), world.random.nextDouble());
		arenaBoss.onPhaseChange(0);
		fireArenaEvent(ArenaEvents.SUMMON);
		return true;
	}

	/**
	 * Tells the arena's set pieces what happened (PLAN 20.1): {@code summon}, {@code phase <n>}, {@code hazard <id>},
	 * {@code bossHit heavy}, {@code kill}, {@code reset}, or a boss's own word. Returns how many pieces heard it.
	 */
	public int fireArenaEvent(String event) {
		return ArenaEvents.fire(this, event);
	}

	/** Share of the boss's max health one hit must take to count as {@code bossHit heavy}. */
	public static final float HEAVY_HIT_FRACTION = 0.04f;

	/** The lowest spot over the arena centre where {@code entity} fits (a dais or rubble may stand there). */
	private Vec3d standingSpot(Entity entity) {
		Vec3d spot = centerPos();
		for (int up = 0; up < 12; up++) {
			Vec3d at = spot.add(0, up, 0);
			if (world.isSpaceEmpty(entity, entity.getType().getDimensions().getBoxAt(at))) {
				return at;
			}
		}
		return spot;
	}

	/** Multiplayer scaling at summon: more HP and one add per fighter beyond the first. */
	private void scaleToFighters(MobEntity boss, ArenaBoss arenaBoss) {
		BossConfig config = BossConfig.get();
		int count = fighters.size();
		double multiplier = BossScaling.healthMultiplier(count, config.healthPerExtraFighter());
		EntityAttributeInstance maxHealth = boss.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
		if (maxHealth != null && multiplier != 1) {
			maxHealth.removeModifier(FIGHTERS_HEALTH_ID);
			maxHealth.addPersistentModifier(new EntityAttributeModifier(FIGHTERS_HEALTH_ID, "Embervoid boss fighters", multiplier - 1,
					EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
			boss.setHealth(boss.getMaxHealth());
		}
		EntityType<? extends MobEntity> addType = arenaBoss.scalingAdd();
		int addCount = addType == null ? 0 : BossScaling.adds(count, config.addsPerExtraFighter());
		for (int i = 0; i < addCount; i++) {
			double angle = Math.PI * 2 * i / addCount;
			spawnAdd(addType, centerPos().add(Math.cos(angle) * 3, 0, Math.sin(angle) * 3));
		}
	}

	private void addFighter(ServerPlayerEntity player) {
		if (fighters.add(player.getUuid())) {
			bar.addPlayer(player);
			BossMusic.play(player, music);
			if (fog != null) {
				ArenaFog.push(player, ArenaFog.source(spec.bossId()), fog[0], (int) fog[1]);
			}
		}
	}

	private void removeFighter(UUID uuid, @Nullable ServerPlayerEntity player) {
		fighters.remove(uuid);
		damageDealt.remove(uuid);
		if (player != null) {
			bar.removePlayer(player);
			BossMusic.stop(player);
			if (fog != null) {
				ArenaFog.clear(player, ArenaFog.source(spec.bossId()));
			}
		}
	}

	/** The boss entity, or null if it is gone. */
	@Nullable
	public MobEntity boss() {
		return bossUuid != null && world.getEntity(bossUuid) instanceof MobEntity boss ? boss : null;
	}

	/** Fighters still in the fight (alive, inside, online). */
	public List<ServerPlayerEntity> fighters() {
		List<ServerPlayerEntity> present = new ArrayList<>();
		for (UUID uuid : fighters) {
			ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
			if (player != null && player.getServerWorld() == world && player.isAlive()) {
				present.add(player);
			}
		}
		return present;
	}

	/** Whether a position is inside the fog wall (plus {@code margin} blocks), within the arena's height band. */
	public boolean contains(Vec3d pos, double margin) {
		return isInside(pos, margin);
	}

	/** Horizontal check against the fog wall radius (plus a margin), within the arena's height band. */
	private boolean isInside(Vec3d pos, double margin) {
		double dx = pos.x - (center.getX() + 0.5);
		double dz = pos.z - (center.getZ() + 0.5);
		return dx * dx + dz * dz <= (spec.radius() + margin) * (spec.radius() + margin)
				&& pos.y > center.getY() - 3 && pos.y < center.getY() + 20;
	}

	// --- Fight-bound adds, hazards, fog, phases ---

	/**
	 * Summons an add for this fight at {@code pos} (or the first free spot above it): persistent, {@link BossConfig#addLevelOffset} levels below the boss,
	 * aimed at the nearest fighter, removed when the fight ends. Returns null if the type cannot be created.
	 */
	@Nullable
	public <T extends MobEntity> T spawnAdd(EntityType<T> type, Vec3d pos) {
		T add = type.create(world);
		if (add == null) {
			return null;
		}
		Vec3d at = pos;
		for (int up = 0; up < 6 && !world.isSpaceEmpty(add, type.getDimensions().getBoxAt(at)); up++) {
			at = pos.add(0, up + 1, 0); // out of a walkway or rubble it would stand in
		}
		add.refreshPositionAndAngles(at.x, at.y, at.z, world.random.nextFloat() * 360, 0);
		add.initialize(world, world.getLocalDifficulty(add.getBlockPos()), SpawnReason.MOB_SUMMONED, null, null);
		add.setPersistent();
		world.spawnEntity(add);
		MobLevels.setLevel(add, Math.max(1, spec.level() - BossConfig.get().addLevelOffset()));
		fighters().stream().min(Comparator.comparingDouble(add::squaredDistanceTo)).ifPresent(add::setTarget);
		adds.add(add.getUuid());
		return add;
	}

	/** This fight's adds that are still alive. */
	public List<MobEntity> adds() {
		List<MobEntity> living = new ArrayList<>();
		for (UUID uuid : adds) {
			if (world.getEntity(uuid) instanceof MobEntity add && add.isAlive() && !add.isRemoved()) {
				living.add(add);
			}
		}
		return living;
	}

	/** Starts a hazard; the fight ticks it until it is over and ends it with the fight. */
	public void addHazard(ArenaHazard hazard) {
		if (!ended) {
			hazards.add(hazard);
			fireArenaEvent(ArenaEvents.HAZARD + " " + hazard.id());
		}
	}

	public List<ArenaHazard> hazards() {
		return List.copyOf(hazards);
	}

	/** Shows arena fog to every fighter (density 0–1, colour 0xRRGGBB) until changed, cleared or the fight ends. */
	public void setFog(float density, int color) {
		fog = new float[]{density, color};
		for (ServerPlayerEntity player : fighters()) {
			ArenaFog.push(player, ArenaFog.source(spec.bossId()), density, color);
		}
	}

	public void clearFog() {
		if (fog == null) {
			return;
		}
		fog = null;
		for (ServerPlayerEntity player : fighters()) {
			ArenaFog.clear(player, ArenaFog.source(spec.bossId()));
		}
	}

	/** The arena fog density now shown, or 0. */
	public float fogDensity() {
		return fog == null ? 0 : fog[0];
	}

	/** Ends a scripted phase ({@link ArenaBoss#scriptedPhases}); does nothing once the health phases have begun. */
	public void advancePhase() {
		MobEntity boss = boss();
		if (ended || !(boss instanceof ArenaBoss arenaBoss) || phase >= arenaBoss.scriptedPhases()) {
			return;
		}
		phase++;
		arenaBoss.onPhaseChange(phase);
		fireArenaEvent(ArenaEvents.PHASE + " " + phase);
	}

	/** A fighter hurt the boss (before armor): counts toward the next target switch; a heavy one shakes the arena. */
	void recordDamage(ServerPlayerEntity player, float amount) {
		if (fighters.contains(player.getUuid())) {
			damageDealt.merge(player.getUuid(), amount, Float::sum);
		}
		MobEntity boss = boss();
		if (boss != null && amount >= boss.getMaxHealth() * HEAVY_HIT_FRACTION) {
			fireArenaEvent(ArenaEvents.BOSS_HIT_HEAVY);
		}
	}

	void tick() {
		if (ended) {
			return;
		}
		MobEntity boss = boss();
		if (boss == null || boss.isRemoved()) {
			// Gone without dying (chunk unloaded, killed by a command that removes, ...): start over.
			reset();
			return;
		}

		// Fighters who died, logged out or got out some other way have left the fight.
		List<ServerPlayerEntity> present = new ArrayList<>();
		for (UUID uuid : List.copyOf(fighters)) {
			ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
			if (player == null || player.getServerWorld() != world || !player.isAlive() || !isInside(player.getPos(), 2)) {
				removeFighter(uuid, player);
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
			Vec3d back = standingSpot(boss);
			boss.requestTeleport(back.x, back.y, back.z);
		}
		for (MobEntity add : adds()) {
			if (!isInside(add.getPos(), 1)) {
				Vec3d back = standingSpot(add);
				add.requestTeleport(back.x, back.y, back.z);
			}
		}
		tickHazards();
		tickTargetSwitch((ArenaBoss) boss, boss, present);

		float fraction = boss.getHealth() / boss.getMaxHealth();
		bar.setPercent(fraction);
		ArenaBoss arenaBoss = (ArenaBoss) boss;
		int scripted = arenaBoss.scriptedPhases();
		float[] thresholds = arenaBoss.phaseThresholds();
		while (phase >= scripted && phase - scripted < thresholds.length && fraction <= thresholds[phase - scripted]) {
			phase++;
			arenaBoss.onPhaseChange(phase);
			fireArenaEvent(ArenaEvents.PHASE + " " + phase);
		}
	}

	private void tickHazards() {
		for (ArenaHazard hazard : List.copyOf(hazards)) {
			if (!hazards.contains(hazard)) {
				continue;
			}
			if (!hazard.tick(this)) {
				hazards.remove(hazard);
				hazard.end(this);
			}
		}
	}

	/** Every 8–10 s: turn to whoever dealt the most damage meanwhile, or to another fighter if nobody did. */
	private void tickTargetSwitch(ArenaBoss arenaBoss, MobEntity boss, List<ServerPlayerEntity> present) {
		if (--switchIn > 0) {
			return;
		}
		BossConfig config = BossConfig.get();
		switchIn = BossScaling.switchTicks(config.targetSwitchMinSeconds(), config.targetSwitchMaxSeconds(), world.random.nextDouble());
		List<ServerPlayerEntity> candidates = present.stream().filter(p -> !p.isCreative()).toList();
		if (!arenaBoss.switchesTargets() || candidates.size() < 2) {
			damageDealt.clear();
			return;
		}
		ServerPlayerEntity next = candidates.stream()
				.filter(p -> damageDealt.getOrDefault(p.getUuid(), 0f) > 0)
				.max(Comparator.comparingDouble(p -> damageDealt.get(p.getUuid())))
				.orElse(null);
		if (next == null) {
			List<ServerPlayerEntity> others = candidates.stream().filter(p -> p != boss.getTarget()).toList();
			next = others.get(world.random.nextInt(others.size()));
		}
		damageDealt.clear();
		boss.setTarget(next);
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
		fireArenaEvent(ArenaEvents.KILL);
		RealmDefinition realm = Realms.byWorld(world.getRegistryKey());
		boolean finalBoss = realm != null && realm.finalBoss().equals(spec.bossId());
		boolean shrine = ensureShrine();
		BossReward reward = BossReward.of(spec.bossId());
		for (UUID uuid : fighters) {
			ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
			if (player == null || player.getServerWorld() != world || !player.isAlive() || !isInside(player.getPos(), 2)) {
				continue;
			}
			// Personal loot: straight into this player's inventory, nobody else can pick it up.
			if (ProgressManager.markBossBeaten(player, spec.bossId())) {
				if (reward == null) {
					player.getInventory().offerOrDrop(EmberHeartItem.of(spec.bossId()));
					player.sendMessage(Text.translatable("embervoid.boss.first_kill", bossName()), false);
				} else {
					List<ItemStack> items = reward.firstKillItems(world, player);
					items.forEach(stack -> player.getInventory().offerOrDrop(stack.copy()));
					player.sendMessage(Text.translatable("embervoid.boss.first_kill.items", bossName(), itemList(items)), false);
				}
				if (shrine) {
					player.sendMessage(Text.translatable("embervoid.shrine.kindled"), true);
				}
			} else if (reward != null && reward.soulEveryKill() && !rematch) {
				ItemStack soul = new ItemStack(reward.soul());
				player.getInventory().offerOrDrop(soul.copy());
				player.sendMessage(Text.translatable("embervoid.boss.first_kill.items", bossName(), itemList(List.of(soul))), false);
			} else if (reward != null && !rematch && world.random.nextFloat() < BossConfig.get().repeatSoulChance()) {
				// Helping a friend: now and then the soul answers again.
				player.getInventory().offerOrDrop(new ItemStack(reward.soul()));
				player.sendMessage(Text.translatable("embervoid.boss.repeat_soul", bossName(), new ItemStack(reward.soul()).toHoverableText()), false);
			} else {
				player.sendMessage(Text.translatable("embervoid.boss.victory", bossName()), true);
			}
			if (finalBoss) {
				completeRealm(player, realm);
			}
		}
		end();
	}

	/**
	 * Every arena leaves an Ember Shrine (milestone 7). Worldgen places it; if it is missing (another structure or
	 * terrain overwrote it, a player removed it in creative), it is put back now. Returns false if the arena's
	 * structure is not found, so no shrine can be promised.
	 */
	private boolean ensureShrine() {
		RealmFeatures.Placed feature = RealmFeatures.at(world, center);
		if (feature == null) {
			return false;
		}
		BlockPos shrine = feature.piece().shrinePos();
		if (!world.getBlockState(shrine).isOf(SanctumBlocks.EMBER_SHRINE)) {
			if (world.getBlockState(shrine.down()).getCollisionShape(world, shrine.down()).isEmpty()) {
				world.setBlockState(shrine.down(), ModBlocks.POLISHED_ASH_STONE.getDefaultState());
			}
			world.setBlockState(shrine, SanctumBlocks.EMBER_SHRINE.getDefaultState());
			Embervoid.LOGGER.warn("Arena shrine of {} was missing at {}; placed it", spec.bossId(), shrine.toShortString());
		}
		return true;
	}

	/** "a, b and c" of the items' names, for the first-kill message. */
	private static Text itemList(List<ItemStack> items) {
		MutableText list = Text.empty();
		for (int i = 0; i < items.size(); i++) {
			if (i > 0) {
				list.append(Text.literal(", "));
			}
			list.append(items.get(i).toHoverableText());
		}
		return list;
	}

	private static void completeRealm(ServerPlayerEntity player, RealmDefinition realm) {
		ProgressManager.completeRealm(player, realm.id());
		// The next obelisk lights up even while its realm is not built yet.
		Identifier next = ProgressManager.nextRealm(realm.id());
		if (next != null) {
			ProgressManager.unlockRealm(player, next);
		}
		player.sendMessage(Text.translatable("embervoid.realm.completed"), false);
	}

	/** Ends the fight without a kill: the boss goes away and comes back whole next time. */
	void reset() {
		Entity boss = boss();
		if (boss != null) {
			boss.discard();
		}
		fireArenaEvent(ArenaEvents.RESET);
		end();
	}

	/** Ends the fight: hazards end, adds go, changed blocks come back, fog, bar and music stop. */
	private void end() {
		ended = true;
		for (ArenaHazard hazard : List.copyOf(hazards)) {
			hazard.end(this);
		}
		hazards.clear();
		for (MobEntity add : adds()) {
			add.discard();
		}
		adds.clear();
		blocks.restore();
		if (fog != null) {
			for (ServerPlayerEntity player : fighters()) {
				ArenaFog.clear(player, ArenaFog.source(spec.bossId()));
			}
			fog = null;
		}
		for (ServerPlayerEntity player : List.copyOf(bar.getPlayers())) {
			BossMusic.stop(player);
		}
		bar.clearPlayers();
	}

	@Override
	public String toString() {
		return spec.bossId() + " at " + center.toShortString() + ", fighters " + fighters.size() + ", phase " + phase
				+ ", adds " + adds().size() + ", hazards " + hazards.size();
	}

	/** Id of the boss this fight is for. */
	public Identifier bossId() {
		return spec.bossId();
	}
}
