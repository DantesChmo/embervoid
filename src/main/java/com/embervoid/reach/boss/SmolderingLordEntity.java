package com.embervoid.reach.boss;

import com.embervoid.boss.AreaHazard;
import com.embervoid.boss.AshStormHazard;
import com.embervoid.boss.BossFight;
import com.embervoid.boss.BossHits;
import com.embervoid.boss.Telegraph;
import com.embervoid.boss.TelegraphShape;
import com.embervoid.reach.boss.arena.ArenaFrame;
import com.embervoid.reach.boss.arena.ReachArenas;
import com.embervoid.reach.mob.ReachMob;
import com.embervoid.reach.mob.ReachMobs;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Smoldering Lord, the realm boss of the Ashen Reach (DESIGN §4.5, PLAN 13.4): the greedy king, 3.5 blocks, a
 * greatsword, Lv 30, in the courtyard below the keep wall. The exam: it replays the three arena hazards.
 *
 * <ul>
 *   <li><b>P0, on the wall</b> (a scripted phase, untouchable): mortar rings fall on the fighters, flame lines run
 *   across the courtyard, two Sparking Marksmen shoot from the wall. Lighting the three signal braziers brings him
 *   down: a ring r {@value #LANDING_RADIUS} marks where he lands in the middle.</li>
 *   <li><b>P1.</b> Greatsword: two cone swings and a slam; a lunge along a line that leaves a fire trail.</li>
 *   <li><b>P2 at 50 %.</b> His armor shatters: ×{@value #SHATTERED_DAMAGE} damage taken, faster. Every ~25 s one zone
 *   hazard in turn: the Colossus's ash storm (cover shelters), the Grove's fog, the Necropolis's darkness (while it
 *   lasts he takes half damage outside block light ≥ 8 — the signal braziers still burn).</li>
 * </ul>
 *
 * Base damage (Lv 30 on Normal ×3.06): swings 8, slam 10, lunge 8 + trail 2 per 0.5 s, mortar 8, flame line 8,
 * landing 8 (R40.1c: ordinary hits heavy, the slam unblockable against full diamond). Base HP 260 (~712 at Lv 30).
 * First kill: his soul and a Flask Ember ({@link ReachBosses}).
 */
public class SmolderingLordEntity extends ReachBoss {
	public static final Identifier MUSIC = new Identifier("music.dragon");
	public static final double LANDING_RADIUS = 6;
	public static final float SHATTERED_DAMAGE = 1.25f;
	public static final float DARK_DAMAGE = 0.5f;
	public static final int LIT_LEVEL = 8;
	/** Ticks between two zone hazards in P2 (the first comes sooner). */
	public static final int HAZARD_INTERVAL = 500;
	public static final int FOG_TICKS = 400;
	public static final int DARK_TICKS = 300;
	public static final float FOG_DENSITY = 0.8f;
	public static final int FOG_COLOR = 0x402418;

	public static final String SWING_1 = "attack.greatsword_1";
	public static final String SWING_2 = "attack.greatsword_2";
	public static final String SLAM = "attack.slam";
	public static final String LUNGE = "attack.lunge";
	public static final String MORTAR = "attack.mortar";
	public static final String FLAME_LINE = "attack.flame_line";
	public static final String DESCEND = "misc.descend";
	public static final String SHATTER = "misc.shatter";
	public static final String HAZARD = "misc.hazard";

	private static final float SWING_DAMAGE = 8;
	private static final float SLAM_DAMAGE = 10;
	private static final float LUNGE_DAMAGE = 8;
	private static final float TRAIL_DAMAGE = 2;
	private static final float MORTAR_DAMAGE = 8;
	private static final float FLAME_LINE_DAMAGE = 8;
	private static final float LANDING_DAMAGE = 8;
	private static final int WALL_ATTACK_INTERVAL = 60;

	private final List<BlockPos> braziers = new ArrayList<>();
	private int wallAttackTimer = WALL_ATTACK_INTERVAL;
	private boolean descending;
	private int hazardTimer = HAZARD_INTERVAL / 2;
	private int nextHazard;
	private int fogTicks;
	private int darkTicks;

	public SmolderingLordEntity(EntityType<? extends SmolderingLordEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 500;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 260)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, SWING_DAMAGE)
				.add(EntityAttributes.GENERIC_ARMOR, 12)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.26)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
	}

	/** P0: on the wall, untouchable. */
	public boolean isOnWall() {
		return phase() == 0;
	}

	public boolean isArmorShattered() {
		return phase() >= 2;
	}

	public boolean isDark() {
		return darkTicks > 0;
	}

	@Override
	public int scriptedPhases() {
		return 1;
	}

	@Override
	public float[] phaseThresholds() {
		return new float[]{0.5f};
	}

	@Override
	public EntityType<? extends MobEntity> scalingAdd() {
		return ReachMobs.CINDER_HOUND;
	}

	@Override
	public Identifier music() {
		return MUSIC;
	}

	@Override
	public List<String> toggledBones() {
		return List.of("armor");
	}

	@Override
	public boolean showsBone(String bone) {
		return !isArmorShattered();
	}

	@Override
	public void onPhaseChange(int phase) {
		super.onPhaseChange(phase);
		BossFight fight = fight();
		if (fight == null || !(getWorld() instanceof ServerWorld world)) {
			return;
		}
		ArenaFrame frame = ArenaFrame.of(fight.center());
		if (phase == 0) {
			// Up on the keep wall with his marksmen; the signal braziers wait below.
			Vec3d wall = Vec3d.ofBottomCenter(frame.at(ReachArenas.lordOnWall().getX(), ReachArenas.lordOnWall().getY(), ReachArenas.lordOnWall().getZ()));
			refreshPositionAndAngles(wall.x, wall.y, wall.z, 0, 0);
			face(fight.centerPos());
			for (BlockPos spot : ReachArenas.marksmenOnWall()) {
				MobEntity marksman = fight.spawnAdd(ReachMobs.SPARKING_MARKSMAN, Vec3d.ofBottomCenter(frame.at(spot.getX(), spot.getY(), spot.getZ())));
				if (marksman instanceof ReachMob placed) {
					placed.markPlaced(); // never leaves the wall
				}
			}
			findBraziers(fight);
			return;
		}
		Telegraph.cancel(this);
		if (phase == 1) {
			descend(fight);
		} else if (phase == 2) {
			playAnimation(SHATTER);
			world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.HOSTILE, 2.0f, 0.5f);
			world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_BLAZE_DEATH, SoundCategory.HOSTILE, 1.5f, 0.6f);
			world.spawnParticles(ParticleTypes.LAVA, getX(), getY() + 2, getZ(), 30, 0.5, 0.8, 0.5, 0);
			getAttributeInstance(EntityAttributes.GENERIC_ARMOR).setBaseValue(4);
			getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.32);
			moveCooldown = 30;
		}
	}

	/** The signal braziers: every campfire in the courtyard; their state comes back after the fight. */
	private void findBraziers(BossFight fight) {
		BlockPos center = fight.center();
		int r = fight.spec().radius();
		for (BlockPos pos : BlockPos.iterate(center.add(-r, 0, -r), center.add(r, 6, r))) {
			if (fight.world().getBlockState(pos).isOf(Blocks.CAMPFIRE)) {
				braziers.add(pos.toImmutable());
				fight.blocks().remember(pos);
			}
		}
	}

	public List<BlockPos> braziers() {
		return List.copyOf(braziers);
	}

	private boolean burning(BlockPos pos) {
		BlockState state = getWorld().getBlockState(pos);
		return state.getBlock() instanceof CampfireBlock && state.get(CampfireBlock.LIT);
	}

	/** All signal braziers lit: he comes down, a ring marking where he lands in the middle of the courtyard. */
	private void descend(BossFight fight) {
		descending = true;
		Vec3d landing = fight.centerPos();
		TelegraphShape ring = TelegraphShape.ring(landing, LANDING_RADIUS);
		getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 2.0f, 0.7f);
		Telegraph.windup(this, ring, 30, DESCEND, null, ParticleTypes.FLAME, () -> {
			descending = false;
			requestTeleport(landing.x, landing.y, landing.z);
			if (getWorld() instanceof ServerWorld world) {
				world.playSound(null, landing.x, landing.y, landing.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2.0f, 0.6f);
				world.spawnParticles(ParticleTypes.EXPLOSION, landing.x, landing.y + 0.5, landing.z, 8, LANDING_RADIUS / 2, 0.2, LANDING_RADIUS / 2, 0);
				world.spawnParticles(ParticleTypes.FLAME, landing.x, landing.y + 0.5, landing.z, 60, LANDING_RADIUS / 2, 0.3, LANDING_RADIUS / 2, 0.05);
			}
			for (PlayerEntity player : Telegraph.playersIn(this, ring)) {
				player.damage(getDamageSources().mobAttack(this), LANDING_DAMAGE);
				knockAway(player, 1.2, 0.5);
			}
			moveCooldown = 30;
		});
	}

	// --- Brain ---

	@Override
	protected void tickBoss() {
		BossFight fight = fight();
		if (fight == null) {
			return;
		}
		if (isOnWall()) {
			setVelocity(0, getVelocity().y, 0);
			if (!braziers.isEmpty() && braziers.stream().allMatch(this::burning)) {
				fight.advancePhase();
				return;
			}
			if (moveCooldown > 0) {
				moveCooldown--; // held (summon pause, tests)
			} else if (--wallAttackTimer <= 0 && !Telegraph.isWindingUp(this)) {
				wallAttackTimer = WALL_ATTACK_INTERVAL;
				List<ServerPlayerEntity> fighters = fight.fighters();
				if (!fighters.isEmpty()) {
					if (random.nextBoolean()) {
						mortar();
					} else {
						flameLine(fighters.get(random.nextInt(fighters.size())));
					}
				}
			}
			return;
		}
		if (fogTicks > 0 && --fogTicks == 0) {
			fight.clearFog();
		}
		if (darkTicks > 0) {
			if (darkTicks % 40 == 0) {
				for (ServerPlayerEntity player : fight.fighters()) {
					player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 80, 0, false, false), this);
				}
			}
			darkTicks--;
		}
		if (isArmorShattered() && --hazardTimer <= 0 && !Telegraph.isWindingUp(this)) {
			hazardTimer = HAZARD_INTERVAL;
			startZoneHazard(nextHazard);
			nextHazard = (nextHazard + 1) % 3;
		}
	}

	@Override
	protected boolean isBusy() {
		return isOnWall() || descending;
	}

	@Override
	protected double keepDistance() {
		return 3;
	}

	@Override
	protected int chooseMove(LivingEntity target, double squaredDistance) {
		boolean fast = isArmorShattered();
		if (squaredDistance <= 5 * 5) {
			comboSwing(target, 1);
			return fast ? 22 : 30;
		}
		if (squaredDistance <= 14 * 14) {
			lunge(target);
			return fast ? 30 : 40;
		}
		return -1;
	}

	// --- Wall moves (P0) ---

	/** Mortar rings: a ring at each of up to two fighters, falling after a long flight. */
	public void mortar() {
		BossFight fight = fight();
		if (fight == null) {
			return;
		}
		playAnimation(MORTAR);
		getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 2.0f, 0.6f);
		List<ServerPlayerEntity> fighters = new ArrayList<>(fight.fighters());
		int shells = Math.min(2, fighters.size());
		for (int i = 0; i < shells; i++) {
			ServerPlayerEntity target = fighters.remove(random.nextInt(fighters.size()));
			TelegraphShape ring = TelegraphShape.ring(target.getPos(), 3);
			Telegraph.windup(this, ring, 40, null, null, ParticleTypes.FLAME, () -> {
				Vec3d at = target.getPos();
				fight.world().spawnParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.3, at.z, 2, 1, 0.1, 1, 0);
				for (PlayerEntity hit : Telegraph.playersIn(this, ring)) {
					hit.damage(BossHits.source(this, DamageTypes.IN_FIRE), MORTAR_DAMAGE);
					hit.setOnFireFor(3);
				}
			});
		}
	}

	/** A line of fire across the courtyard floor from below the wall towards a fighter; it burns on for 3 s. */
	public void flameLine(LivingEntity target) {
		BossFight fight = fight();
		if (fight == null) {
			return;
		}
		Vec3d start = new Vec3d(getX(), fight.centerPos().y, getZ());
		Vec3d direction = new Vec3d(target.getX() - start.x, 0, target.getZ() - start.z);
		direction = direction.lengthSquared() < 1e-4 ? new Vec3d(0, 0, -1) : direction.normalize();
		TelegraphShape line = TelegraphShape.line(start, start.add(direction.multiply(fight.spec().radius() * 2)), 1.2);
		telegraph(line, 30, FLAME_LINE, SoundEvents.ITEM_FIRECHARGE_USE, ParticleTypes.FLAME, player -> {
			player.damage(BossHits.source(this, DamageTypes.IN_FIRE), FLAME_LINE_DAMAGE);
			player.setOnFireFor(3);
		});
		Telegraph.windup(this, 30, null, null, ParticleTypes.FLAME, 0, () -> fight.addHazard(new AreaHazard(line, 60, 10,
				BossHits.source(this, DamageTypes.IN_FIRE), TRAIL_DAMAGE, ParticleTypes.FLAME, player -> player.setOnFireFor(2))));
	}

	// --- Greatsword (P1, P2) ---

	/** Combo: 1 and 2 are cone swings, 3 a slam in front of him that breaks a raised shield. */
	public void comboSwing(LivingEntity target, int hit) {
		face(target.getPos());
		Vec3d facing = directionTo(target.getPos());
		if (hit < 3) {
			telegraph(TelegraphShape.cone(getPos(), facing, 5, 60), hit == 1 ? 14 : 10, hit == 1 ? SWING_1 : SWING_2,
					SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, ParticleTypes.FLAME, player -> {
						player.damage(getDamageSources().mobAttack(this), SWING_DAMAGE);
						player.setOnFireFor(2);
					});
		} else {
			telegraph(TelegraphShape.ring(getPos().add(facing.multiply(2.5)), 3.5), 20, SLAM, SoundEvents.ENTITY_RAVAGER_ROAR,
					ParticleTypes.LAVA, player -> {
						BossHits.shieldBreaking(this, player, SLAM_DAMAGE);
						knockAway(player, 1.0, 0.5);
					});
		}
	}

	@Override
	protected void afterStrike(String animation) {
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive()) {
			return;
		}
		if (animation.equals(SWING_1)) {
			comboSwing(target, 2);
		} else if (animation.equals(SWING_2)) {
			comboSwing(target, 3);
		}
	}

	/** A lunge along a line at a farther target, leaving a trail of fire behind. */
	public void lunge(LivingEntity target) {
		face(target.getPos());
		Vec3d facing = directionTo(target.getPos());
		double length = Math.max(6, Math.min(14, Math.sqrt(squaredDistanceTo(target)) + 1.5));
		TelegraphShape line = TelegraphShape.line(getPos(), getPos().add(facing.multiply(length)), 1.2);
		telegraph(line, 18, LUNGE, SoundEvents.ENTITY_RAVAGER_ATTACK, ParticleTypes.FLAME, player -> {
			player.damage(getDamageSources().mobAttack(this), LUNGE_DAMAGE);
			knockAway(player, 0.8, 0.3);
		});
		Telegraph.windup(this, 18, null, null, ParticleTypes.FLAME, 0, () -> {
			setVelocity(facing.x * 1.6, 0.1, facing.z * 1.6);
			velocityModified = true;
			BossFight fight = fight();
			if (fight != null) {
				fight.addHazard(new AreaHazard(line, 80, 10, BossHits.source(this, DamageTypes.IN_FIRE), TRAIL_DAMAGE,
						ParticleTypes.FLAME, player -> player.setOnFireFor(2)));
			}
		});
	}

	// --- Zone hazards (P2) ---

	/** One of the three arena hazards: 0 ash storm, 1 fog, 2 darkness. */
	public void startZoneHazard(int which) {
		BossFight fight = fight();
		if (fight == null) {
			return;
		}
		playAnimation(HAZARD);
		switch (which) {
			case 0 -> Telegraph.windup(this, 30, null, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, ParticleTypes.ASH, 6,
					() -> fight.addHazard(new AshStormHazard(this, 120, 2)));
			case 1 -> {
				fogTicks = FOG_TICKS;
				fight.setFog(FOG_DENSITY, FOG_COLOR);
			}
			default -> {
				darkTicks = DARK_TICKS;
				for (ServerPlayerEntity player : fight.fighters()) {
					player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 80, 0, false, false), this);
				}
			}
		}
	}

	/** Whether he stands in block light ≥ {@value #LIT_LEVEL} (the darkness hazard halves damage outside it). */
	public boolean isLit() {
		BlockPos feet = getBlockPos();
		return Math.max(getWorld().getLightLevel(LightType.BLOCK, feet), getWorld().getLightLevel(LightType.BLOCK, feet.up(2))) >= LIT_LEVEL;
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (!getWorld().isClient && !source.isOf(DamageTypes.GENERIC_KILL) && !source.isOf(DamageTypes.OUT_OF_WORLD)) {
			if (isOnWall() || descending) {
				return false; // up on the wall he cannot be touched
			}
			if (isArmorShattered()) {
				amount *= SHATTERED_DAMAGE;
			}
			if (isDark() && !isLit()) {
				amount *= DARK_DAMAGE;
			}
		}
		return super.damage(source, amount);
	}

	@Override
	protected List<String> actionAnimations() {
		return List.of(SWING_1, SWING_2, SLAM, LUNGE, MORTAR, FLAME_LINE, DESCEND, SHATTER, HAZARD);
	}

	@Override
	public boolean isFireImmune() {
		return true;
	}
}
