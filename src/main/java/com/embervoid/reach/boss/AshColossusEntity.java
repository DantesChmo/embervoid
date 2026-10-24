package com.embervoid.reach.boss;

import com.embervoid.boss.AshStormHazard;
import com.embervoid.boss.BossFight;
import com.embervoid.boss.BossHits;
import com.embervoid.boss.Telegraph;
import com.embervoid.boss.TelegraphShape;
import com.embervoid.fx.CombatFx;
import com.embervoid.fx.SwingArc;
import com.embervoid.reach.mob.ReachMobs;
import com.embervoid.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Ash Colossus, boss of the Ashen Fields (DESIGN §4.5, PLAN 12.4): 4.5 blocks of fused stone and ash with an ember
 * core in its back, Lv 18. What it teaches: use cover, strike the back.
 *
 * <ul>
 *   <li><b>P1.</b> Fist: a cone in front. Arm drag: a wide low sweep that drags players in. Leap: crouches, a ring
 *   marks where it lands on a far target. Boulder: rips up stone and throws it; a falling shadow marks the impact.
 *   Every ~20 s an ash storm: after a roar, for 6 s everyone it can see takes 2 HP/s (magic) and goes blind; a
 *   granary pillar between it and a player shelters them. It smashes the pillar a player hides behind.</li>
 *   <li><b>P2 at 50 %.</b> Sheds its stone: armor drops, it gets faster, the core is exposed, and cinders fall
 *   around the fighters.</li>
 * </ul>
 *
 * Damage taken by where it comes from: front (±60° of its facing) ×{@value #FRONT_DAMAGE}, back (beyond ±120°)
 * ×{@value #BACK_DAMAGE} with the core covered (P1), ×{@value #CORE_DAMAGE} with it exposed (P2), sides ×1.
 * Base damage (Lv 18 on Normal ×2.31): fist 8, leap 9, boulder 7, drag 6, cinders 5. Base HP 105 (~212 at Lv 18; PLAN 18.4).
 */
public class AshColossusEntity extends ReachBoss {
	public static final float FRONT_DAMAGE = 0.5f;
	public static final float BACK_DAMAGE = 1.5f;
	public static final float CORE_DAMAGE = 2.0f;
	/** Ticks between ash storms, the roar before one, and how long it blows. */
	public static final int STORM_INTERVAL = 400;
	public static final int STORM_WINDUP = 30;
	public static final int STORM_TICKS = 120;
	/** Ash storm damage per second to a player it can see (magic, not scaled by level). */
	public static final float STORM_DAMAGE = 2;
	/** Windup of each move in ticks (the balance model reads them, PLAN 18.1). */
	public static final int FIST_WINDUP = 18;
	public static final int DRAG_WINDUP = 16;
	public static final int LEAP_WINDUP = 22;
	public static final int BOULDER_WINDUP = 30;
	public static final int SMASH_WINDUP = 20;
	public static final int CINDER_WINDUP = 30;
	/** Ticks until the brain picks the next move after one, by kind of move, in P1 and in P2 (faster). */
	public static final int SMASH_COOLDOWN = 30;
	public static final int SMASH_COOLDOWN_P2 = 20;
	public static final int MELEE_COOLDOWN = 32;
	public static final int MELEE_COOLDOWN_P2 = 22;
	public static final int RANGED_COOLDOWN = 50;
	public static final int RANGED_COOLDOWN_P2 = 35;
	public static final int FAR_COOLDOWN = 60;
	public static final int FAR_COOLDOWN_P2 = 40;
	/** The pause after shedding its stone (P2 starts). */
	public static final int SHED_PAUSE = 30;
	/** Ticks a leap takes from take-off to over its mark. */
	public static final int LEAP_FLIGHT_TICKS = 14;
	/** Ticks between two cinder falls in P2. */
	public static final int CINDER_INTERVAL = 50;

	public static final String FIST = "attack.fist";
	public static final String DRAG = "attack.drag";
	public static final String LEAP = "attack.leap";
	public static final String BOULDER = "attack.boulder";
	public static final String SMASH = "attack.smash";
	public static final String STORM = "misc.storm";
	public static final String SHED = "misc.shed";

	private static final float FIST_DAMAGE = 8;
	private static final float LEAP_DAMAGE = 9;
	private static final float BOULDER_DAMAGE = 7;
	private static final float DRAG_DAMAGE = 6;
	private static final float CINDER_DAMAGE = 5;
	private static final double LEAP_RADIUS = 4.5;
	private static final double BOULDER_RADIUS = 2.5;
	private static final double CINDER_RADIUS = 2;
	/** How far it reaches to smash a pillar (from its centre to the block). */
	private static final double SMASH_REACH = 5;
	/** A smashed pillar: every block within this of the hit column, floor + 1 up to this high. */
	private static final int SMASH_RADIUS = 2;
	private static final int SMASH_HEIGHT = 10;

	private int stormTimer = STORM_INTERVAL / 2;
	private int cinderTimer = CINDER_INTERVAL;
	/** Landing spot of a leap in the air, or null. */
	@Nullable
	private Vec3d leapTarget;
	private int leapAirTicks;
	private int cindersDropped;

	public AshColossusEntity(EntityType<? extends AshColossusEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 150;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 105)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, FIST_DAMAGE)
				.add(EntityAttributes.GENERIC_ARMOR, 10)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
	}

	/** How long the body lies there after death (the fall in {@code misc.death}, 3 s). */
	public static final int DEATH_TICKS = 60;

	/** Whether the core in its back is bare (P2). */
	public boolean coreExposed() {
		return phase() >= 1;
	}

	/** A hit into the bare core staggers it (visual only); any other hit is a flinch. */
	@Override
	protected String hurtAnimation(DamageSource source, float amount) {
		Vec3d from = source.getPosition();
		return coreExposed() && from != null && directionFactor(from) >= CORE_DAMAGE ? STAGGER : HURT;
	}

	@Override
	protected List<String> hurtAnimations() {
		return List.of(HURT, STAGGER);
	}

	@Override
	public int deathTicks() {
		return DEATH_TICKS;
	}

	/** It crumbles as it falls: stone dust and embers, a last blaze of the core, and the ground shakes when it lands. */
	@Override
	protected void tickDeath(ServerWorld world, int deathTime) {
		if (deathTime % 4 == 0) {
			world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, ModBlocks.ASH_STONE.getDefaultState()),
					getX(), getY() + 2, getZ(), 6, 1.0, 1.2, 1.0, 0.1);
			world.spawnParticles(ParticleTypes.FLAME, getX(), getY() + 2.2, getZ(), 3, 0.8, 1.0, 0.8, 0.02); // TODO own particle (ember)
		}
		if (deathTime == 40) {
			// The mass topples onto its fists (misc.death lands at 2.0 s).
			world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.5f, 0.5f);
			CombatFx.impact(world, getPos(), 1.2f, 3.5f, 40, CombatFx.EMBER);
			world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, ModBlocks.ASH_STONE.getDefaultState()),
					getX(), getY() + 0.5, getZ(), 80, 2.0, 0.4, 2.0, 0.2);
			world.spawnParticles(ParticleTypes.ASH, getX(), getY() + 1, getZ(), 60, 2.5, 0.8, 2.5, 0.05); // TODO own particle (ash flake)
		}
		if (deathTime == DEATH_TICKS) {
			world.spawnParticles(ParticleTypes.LAVA, getX(), getY() + 1, getZ(), 20, 1.0, 0.5, 1.0, 0); // TODO own particle (cinder spark)
			world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 30, 1.5, 0.8, 1.5, 0.02);
		}
	}

	@Override
	public List<String> toggledBones() {
		return List.of("plates", "core");
	}

	@Override
	public boolean showsBone(String bone) {
		return bone.equals("plates") != coreExposed();
	}

	@Override
	public float[] phaseThresholds() {
		return new float[]{0.5f};
	}

	@Override
	public EntityType<? extends MobEntity> scalingAdd() {
		return ReachMobs.CINDER_HUSK;
	}

	@Override
	public void onPhaseChange(int phase) {
		super.onPhaseChange(phase);
		if (phase != 1 || !(getWorld() instanceof ServerWorld world)) {
			return;
		}
		Telegraph.cancel(this);
		leapTarget = null;
		moveCooldown = SHED_PAUSE;
		playAnimation(SHED);
		world.playSound(null, getX(), getY(), getZ(), ReachBossSounds.ASH_COLOSSUS_SHED, SoundCategory.HOSTILE, 2.0f, 1.0f);
		world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, ModBlocks.ASH_STONE.getDefaultState()),
				getX(), getY() + 2.5, getZ(), 80, 1.0, 1.5, 1.0, 0.2);
		world.spawnParticles(ParticleTypes.FLAME, getX(), getY() + 2.5, getZ(), 40, 0.8, 1.0, 0.8, 0.05); // TODO own particle (ember)
		// The plates come off its back (misc.shed drops them at 0.9 s) and crash behind it.
		Vec3d behind = getPos().subtract(Vec3d.fromPolar(0, getYaw()).multiply(1.5));
		CombatFx.impact(world, behind, 0.9f, 3.0f, 32, CombatFx.EMBER);
		world.spawnParticles(ParticleTypes.LAVA, getX(), getY() + 2.5, getZ(), 12, 0.5, 0.5, 0.5, 0); // TODO own particle (cinder spark)
		getAttributeInstance(EntityAttributes.GENERIC_ARMOR).setBaseValue(3);
		getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.26);
	}

	// --- Brain ---

	@Override
	protected void tickBoss() {
		BossFight fight = fight();
		if (fight == null) {
			return;
		}
		if (leapTarget != null && getWorld() instanceof ServerWorld world) {
			tickLeap(world);
		}
		if (--stormTimer <= 0 && !Telegraph.isWindingUp(this) && leapTarget == null) {
			startStorm();
		}
		if (coreExposed() && --cinderTimer <= 0) {
			cinderTimer = CINDER_INTERVAL;
			dropCinders(fight);
		}
	}

	@Override
	protected boolean isBusy() {
		return leapTarget != null;
	}

	@Override
	protected double keepDistance() {
		return 3.5;
	}

	@Override
	protected int chooseMove(LivingEntity target, double squaredDistance) {
		boolean fast = coreExposed();
		BlockPos cover = coverOf(target);
		if (cover != null && cover.getSquaredDistance(getPos()) <= SMASH_REACH * SMASH_REACH) {
			smash(cover);
			return fast ? SMASH_COOLDOWN_P2 : SMASH_COOLDOWN;
		}
		if (squaredDistance <= 5.5 * 5.5) {
			if (random.nextInt(3) == 0) {
				drag(target);
			} else {
				fist(target);
			}
			return fast ? MELEE_COOLDOWN_P2 : MELEE_COOLDOWN;
		}
		if (squaredDistance >= 8 * 8 && squaredDistance <= 22 * 22) {
			if (random.nextBoolean()) {
				leap(target);
			} else {
				boulder(target);
			}
			return fast ? RANGED_COOLDOWN_P2 : RANGED_COOLDOWN;
		}
		if (cover != null) {
			return -1; // walk up to the pillar the target hides behind
		}
		if (squaredDistance > 22 * 22) {
			boulder(target);
			return fast ? FAR_COOLDOWN_P2 : FAR_COOLDOWN;
		}
		return -1;
	}

	// --- Moves ---

	public void fist(LivingEntity target) {
		face(target.getPos());
		telegraph(TelegraphShape.cone(getPos(), directionTo(target.getPos()), 5.5, 45), FIST_WINDUP, FIST, ReachBossSounds.ASH_COLOSSUS_FIST,
				ParticleTypes.FLAME, player -> {
					player.damage(getDamageSources().mobAttack(this), FIST_DAMAGE);
					knockAway(player, 1.2, 0.4);
				});
	}

	/** A wide low sweep that drags the players it catches towards it. */
	public void drag(LivingEntity target) {
		face(target.getPos());
		telegraph(TelegraphShape.cone(getPos(), directionTo(target.getPos()), 6, 70), DRAG_WINDUP, DRAG, ReachBossSounds.ASH_COLOSSUS_DRAG,
				ParticleTypes.ASH, player -> {
					player.damage(getDamageSources().mobAttack(this), DRAG_DAMAGE);
					knockAway(player, -0.9, 0.1);
				});
	}

	/** Every strike that lands has its weight: a trail where an arm sweeps, a flash and a shake where a fist hits the ground. */
	@Override
	protected void afterStrike(String animation) {
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		Vec3d front = getPos().add(Vec3d.fromPolar(0, getYaw()).multiply(3));
		if (animation.equals(DRAG)) {
			CombatFx.trail(this, SwingArc.sweep(6, 1.4f, true, 6, CombatFx.EMBER));
			CombatFx.shake(world, getPos(), 0.5f, CombatFx.shakeTicks(0.5f), 24);
			world.spawnParticles(ParticleTypes.ASH, front.x, front.y + 0.3, front.z, 30, 2.0, 0.2, 2.0, 0.05); // TODO own particle (ash flake)
		} else if (animation.equals(FIST)) {
			CombatFx.trail(this, SwingArc.overhead(4, 2.8f, 5, CombatFx.EMBER));
			CombatFx.impact(world, front, 0.7f, 2.5f, 24, CombatFx.EMBER);
			world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, ModBlocks.ASH_STONE.getDefaultState()),
					front.x, front.y + 0.3, front.z, 30, 1.2, 0.3, 1.2, 0.15);
		} else if (animation.equals(SMASH)) {
			CombatFx.trail(this, SwingArc.overhead(5, 3.0f, 6, CombatFx.EMBER));
			CombatFx.impact(world, front, 1.0f, 3.5f, 32, CombatFx.EMBER);
		}
	}

	/** Crouches with a ring at the target's spot, then leaps there; the landing hurts everyone in the ring. */
	public void leap(LivingEntity target) {
		face(target.getPos());
		Vec3d landing = target.getPos();
		TelegraphShape ring = TelegraphShape.ring(landing, LEAP_RADIUS);
		Telegraph.windup(this, ring, LEAP_WINDUP, LEAP, ReachBossSounds.ASH_COLOSSUS_LEAP, ParticleTypes.FLAME, () -> {
			Vec3d delta = landing.subtract(getPos());
			setVelocity(delta.x / LEAP_FLIGHT_TICKS, 0.9 + Math.max(0, delta.y) * 0.08, delta.z / LEAP_FLIGHT_TICKS);
			velocityModified = true;
			leapTarget = landing;
			leapAirTicks = 0;
		});
	}

	private void tickLeap(ServerWorld world) {
		leapAirTicks++;
		// Steered through the air so it comes down on the ring it marked (drag would leave it short).
		int left = LEAP_FLIGHT_TICKS - leapAirTicks;
		if (left > 0) {
			setVelocity((leapTarget.x - getX()) / left, getVelocity().y, (leapTarget.z - getZ()) / left);
			velocityModified = true;
		} else if (!isOnGround()) {
			setVelocity(0, getVelocity().y, 0);
			velocityModified = true;
		}
		if (leapAirTicks % 3 == 0) {
			TelegraphShape.ring(leapTarget, LEAP_RADIUS).draw(world, ParticleTypes.FLAME, 1);
		}
		if ((isOnGround() && leapAirTicks > 4) || leapAirTicks > 40) {
			TelegraphShape ring = TelegraphShape.ring(leapTarget, LEAP_RADIUS);
			leapTarget = null;
			world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.5f, 0.6f);
			world.spawnParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.5, getZ(), 6, LEAP_RADIUS / 2, 0.2, LEAP_RADIUS / 2, 0);
			CombatFx.impact(world, getPos(), 1.0f, (float) LEAP_RADIUS, 32, CombatFx.EMBER);
			world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, ModBlocks.ASH_STONE.getDefaultState()),
					getX(), getY() + 0.3, getZ(), 60, LEAP_RADIUS / 2, 0.3, LEAP_RADIUS / 2, 0.2);
			world.spawnParticles(ParticleTypes.ASH, getX(), getY() + 0.5, getZ(), 40, LEAP_RADIUS / 2, 0.5, LEAP_RADIUS / 2, 0.05); // TODO own particle (ash flake)
			for (PlayerEntity player : Telegraph.playersIn(this, ring)) {
				player.damage(getDamageSources().mobAttack(this), LEAP_DAMAGE);
				knockAway(player, 1.0, 0.6);
			}
		}
	}

	/** Rips up stone and throws it at where the target stands; a falling shadow marks it. */
	public void boulder(LivingEntity target) {
		face(target.getPos());
		Vec3d impact = target.getPos();
		telegraph(TelegraphShape.shadow(impact, BOULDER_RADIUS), BOULDER_WINDUP, BOULDER, ReachBossSounds.ASH_COLOSSUS_BOULDER, ParticleTypes.SMOKE,
				player -> {
					player.damage(getDamageSources().mobAttack(this), BOULDER_DAMAGE);
					knockAway(player, 0.6, 0.4);
				});
		Telegraph.windup(this, BOULDER_WINDUP, null, null, ParticleTypes.SMOKE, 0, () -> {
			if (getWorld() instanceof ServerWorld world) {
				world.playSound(null, impact.x, impact.y, impact.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.0f, 0.9f);
				world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, ModBlocks.ASH_STONE.getDefaultState()),
						impact.x, impact.y + 0.5, impact.z, 40, 1.0, 0.4, 1.0, 0.2);
				CombatFx.impact(world, impact, 0.7f, (float) BOULDER_RADIUS, 28, CombatFx.EMBER);
				world.spawnParticles(ParticleTypes.LAVA, impact.x, impact.y + 0.4, impact.z, 8, 1.0, 0.2, 1.0, 0); // TODO own particle (cinder spark)
			}
		});
	}

	/** Smashes the pillar (or any cover) at {@code block}: every block around that column goes; it comes back after the fight. */
	public void smash(BlockPos block) {
		face(Vec3d.ofCenter(block));
		telegraph(TelegraphShape.cone(getPos(), directionTo(Vec3d.ofCenter(block)), SMASH_REACH + 1, 40), SMASH_WINDUP, SMASH,
				ReachBossSounds.ASH_COLOSSUS_SMASH, ParticleTypes.FLAME, player -> {
					player.damage(getDamageSources().mobAttack(this), FIST_DAMAGE);
					knockAway(player, 1.2, 0.4);
				});
		Telegraph.windup(this, SMASH_WINDUP, null, null, ParticleTypes.FLAME, 0, () -> breakCover(block));
	}

	private void breakCover(BlockPos block) {
		BossFight fight = fight();
		if (fight == null || !(getWorld() instanceof ServerWorld world)) {
			return;
		}
		int floor = fight.center().getY();
		for (int dx = -SMASH_RADIUS; dx <= SMASH_RADIUS; dx++) {
			for (int dz = -SMASH_RADIUS; dz <= SMASH_RADIUS; dz++) {
				for (int y = floor + 1; y <= floor + SMASH_HEIGHT; y++) {
					BlockPos pos = new BlockPos(block.getX() + dx, y, block.getZ() + dz);
					BlockState state = world.getBlockState(pos);
					if (!state.isAir() && state.getFluidState().isEmpty()) {
						world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5,
								pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.1);
						fight.blocks().set(pos, Blocks.AIR.getDefaultState());
					}
				}
			}
		}
		world.playSound(null, block, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2.0f, 0.5f);
		CombatFx.impact(world, Vec3d.ofCenter(block), 1.1f, 3.0f, 36, CombatFx.EMBER);
	}

	/** The ash storm: a roar, then 6 s of ash on everyone it can see. */
	public void startStorm() {
		stormTimer = STORM_INTERVAL;
		Telegraph.windup(this, STORM_WINDUP, STORM, ReachBossSounds.ASH_COLOSSUS_STORM, ParticleTypes.ASH, 6, () -> {
			BossFight fight = fight();
			if (fight != null) {
				fight.addHazard(new AshStormHazard(this, STORM_TICKS, STORM_DAMAGE));
			}
			if (getWorld() instanceof ServerWorld world) {
				// The roar: the ground trembles and ash bursts off its shoulders.
				CombatFx.shake(world, getPos(), 0.6f, 30, 40);
				world.spawnParticles(ParticleTypes.ASH, getX(), getY() + 3, getZ(), 120, 2.0, 1.0, 2.0, 0.1); // TODO own particle (ash flake)
			}
		});
	}

	/** Whether a block stands between its core and the player's eyes (a pillar, rubble). */
	public boolean isSheltered(PlayerEntity player) {
		return AshStormHazard.isSheltered(this, player);
	}

	/** The first block between its core and the target's eyes, or null if it sees them. */
	@Nullable
	private BlockPos coverOf(Entity target) {
		Vec3d from = getPos().add(0, getHeight() * 0.8, 0);
		BlockHitResult hit = getWorld().raycast(new RaycastContext(from, target.getEyePos(), RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE, this));
		return hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
	}

	/** P2: cinders fall around the fighters, each marked by a falling shadow. */
	private void dropCinders(BossFight fight) {
		for (ServerPlayerEntity player : fight.fighters()) {
			Vec3d at = player.getPos().add(random.nextDouble() * 6 - 3, 0, random.nextDouble() * 6 - 3);
			TelegraphShape shadow = TelegraphShape.shadow(at, CINDER_RADIUS);
			cindersDropped++;
			Telegraph.windup(this, shadow, CINDER_WINDUP, null, null, ParticleTypes.FLAME, () -> {
				fight.world().spawnParticles(ParticleTypes.LAVA, at.x, at.y + 0.2, at.z, 8, 0.6, 0.1, 0.6, 0);
				for (PlayerEntity hit : Telegraph.playersIn(this, shadow)) {
					hit.damage(BossHits.source(this, DamageTypes.IN_FIRE), CINDER_DAMAGE);
					hit.setOnFireFor(3);
				}
			});
		}
	}

	/** Cinders dropped so far (tests). */
	public int cindersDropped() {
		return cindersDropped;
	}

	// --- Damage by direction ---

	@Override
	public boolean damage(DamageSource source, float amount) {
		Vec3d from = source.getPosition();
		if (!getWorld().isClient && from != null && !source.isOf(DamageTypes.GENERIC_KILL) && !source.isOf(DamageTypes.OUT_OF_WORLD)) {
			amount *= directionFactor(from);
		}
		return super.damage(source, amount);
	}

	/** Damage factor for a hit coming from {@code from}: front, side or back (the core). */
	public float directionFactor(Vec3d from) {
		Vec3d facing = Vec3d.fromPolar(0, getYaw());
		Vec3d toward = new Vec3d(from.x - getX(), 0, from.z - getZ());
		if (toward.lengthSquared() < 1e-4) {
			return 1;
		}
		double cos = facing.dotProduct(toward.normalize());
		if (cos >= 0.5) {
			return FRONT_DAMAGE;
		}
		if (cos <= -0.5) {
			return coreExposed() ? CORE_DAMAGE : BACK_DAMAGE;
		}
		return 1;
	}

	@Override
	protected List<String> actionAnimations() {
		return List.of(FIST, DRAG, LEAP, BOULDER, SMASH, STORM, SHED);
	}

	@Override
	public boolean isFireImmune() {
		return true;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ReachBossSounds.ASH_COLOSSUS_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ReachBossSounds.ASH_COLOSSUS_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ReachBossSounds.ASH_COLOSSUS_DEATH;
	}

	@Override
	protected SoundEvent stepSound() {
		return ReachBossSounds.ASH_COLOSSUS_STEP;
	}
}
