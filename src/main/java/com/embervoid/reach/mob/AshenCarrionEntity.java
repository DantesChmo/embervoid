package com.embervoid.reach.mob;

import com.embervoid.boss.Telegraph;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.List;

/**
 * Fields elite flyer (DESIGN §4.5). Without a target it perches (on a scarecrow, an outpost wall, the ground). With
 * one it takes off and circles above it, and dives: every {@link #DIVE_MIN_TICKS}–{@link #DIVE_MAX_TICKS}, or as
 * soon as the target drops below half health (at most once per {@link #DIVE_SPACING_TICKS}). Each dive is
 * telegraphed {@link #SHRIEK_TICKS} ahead: it shrieks and hangs in the air while a shadow gathers on the ground at
 * the target's feet. The dive hits once on contact and the bird climbs back to its circle.
 */
public class AshenCarrionEntity extends FlyingReachMob {
	public static final int SHRIEK_TICKS = 15;
	public static final int DIVE_MIN_TICKS = 160;
	public static final int DIVE_MAX_TICKS = 240;
	/** A wounded target draws a dive at once, but no sooner than this after the last one. */
	public static final int DIVE_SPACING_TICKS = 40;
	static final Move SHRIEK = new Move("attack.shriek", SHRIEK_TICKS, 0, ReachMobSounds.ASHEN_CARRION_SHRIEK, 1.0f);
	/** Longest dive before it gives up and climbs. */
	private static final int DIVE_TICKS = 40;
	/** Circles this far from the target, this high above it. */
	private static final double CIRCLE_RADIUS = 7;
	private static final double CIRCLE_HEIGHT = 6;
	private static final double CIRCLE_SPEED = 0.55;
	private static final double DIVE_SPEED = 1.0;
	/** Without a target for this long it comes down to perch. */
	private static final int PERCH_AFTER_TICKS = 100;
	private static final byte STATE_PERCHED = 1;
	private static final byte STATE_DIVING = 2;
	private static final RawAnimation PERCHED = RawAnimation.begin().thenLoop("misc.idle");
	private static final RawAnimation DIVING = RawAnimation.begin().thenLoop("attack.dive");

	private int diveTimer = -1;
	private int sinceDive = DIVE_SPACING_TICKS;
	private int diveTicks;
	private int idleTicks;
	private float circleAngle;

	public AshenCarrionEntity(EntityType<? extends AshenCarrionEntity> type, World world) {
		super(type, world);
		experiencePoints = 15;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 18)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32);
	}

	@Override
	public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason reason,
			@Nullable EntityData data, @Nullable NbtCompound nbt) {
		EntityData result = super.initialize(world, difficulty, reason, data, nbt);
		perch();
		return result;
	}

	/** Its own flight: no movement goals, only targeting. */
	@Override
	protected void initGoals() {
		goalSelector.add(8, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this));
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
	}

	@Override
	protected void onPlaced() {
		perch();
	}

	public boolean isPerched() {
		return getState() == STATE_PERCHED;
	}

	public boolean isDiving() {
		return getState() == STATE_DIVING;
	}

	/** Shrieking: the telegraph of a dive is running. */
	public boolean isShrieking() {
		return currentMove() == SHRIEK;
	}

	/** Ticks until the next scheduled dive (-1 before it has a target). */
	public int diveTimer() {
		return diveTimer;
	}

	private void perch() {
		setState(STATE_PERCHED);
		setVelocity(Vec3d.ZERO);
		idleTicks = 0;
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient || !isAlive()) {
			return;
		}
		LivingEntity target = getTarget();
		if (target != null && !target.isAlive()) {
			setTarget(target = null);
		}
		sinceDive++;
		if (isPerched()) {
			setVelocity(Vec3d.ZERO);
			if (target != null) {
				setState(STATE_NONE);
				setVelocity(0, 0.5, 0);
				playSound(ReachMobSounds.ASHEN_CARRION_FLAP, 1.0f, 1.0f);
			}
			return;
		}
		if (isDiving()) {
			tickDive(target);
			return;
		}
		if (target == null) {
			diveTimer = -1;
			cancelMove();
			if (++idleTicks >= PERCH_AFTER_TICKS) {
				comeDown();
			} else {
				steer(getPos().add(0, getY() < groundY() + 4 ? 0.5 : 0, 0), 0.3, 0.04);
			}
			return;
		}
		idleTicks = 0;
		if (isWindingUp()) {
			// Hangs in the air while it shrieks.
			setVelocity(getVelocity().multiply(0.7));
			lookAtEntity(target, 30, 30);
			return;
		}
		if (diveTimer < 0) {
			diveTimer = nextDiveDelay();
		}
		boolean wounded = target.getHealth() < target.getMaxHealth() * 0.5f && sinceDive >= DIVE_SPACING_TICKS;
		if (--diveTimer <= 0 || wounded) {
			diveTimer = nextDiveDelay();
			startMove(SHRIEK);
			return;
		}
		circleAngle += 0.06f;
		Vec3d point = target.getPos().add(MathHelper.cos(circleAngle) * CIRCLE_RADIUS, CIRCLE_HEIGHT,
				MathHelper.sin(circleAngle) * CIRCLE_RADIUS);
		steer(point, CIRCLE_SPEED, 0.08);
	}

	private int nextDiveDelay() {
		return DIVE_MIN_TICKS + random.nextInt(DIVE_MAX_TICKS - DIVE_MIN_TICKS + 1);
	}

	/** Accelerates toward {@code point}, capped at {@code speed} blocks per tick. */
	private void steer(Vec3d point, double speed, double acceleration) {
		Vec3d toPoint = point.subtract(getPos());
		Vec3d velocity = getVelocity();
		if (toPoint.lengthSquared() > 0.01) {
			velocity = velocity.add(toPoint.normalize().multiply(acceleration));
		}
		if (velocity.length() > speed) {
			velocity = velocity.normalize().multiply(speed);
		}
		setVelocity(velocity);
		faceVelocity(velocity);
	}

	private void faceVelocity(Vec3d velocity) {
		if (velocity.horizontalLengthSquared() > 1.0E-4) {
			float yaw = (float) (MathHelper.atan2(velocity.z, velocity.x) * MathHelper.DEGREES_PER_RADIAN) - 90;
			setYaw(yaw);
			bodyYaw = yaw;
			headYaw = yaw;
		}
	}

	private int groundY() {
		return getWorld().getTopY(Heightmap.Type.MOTION_BLOCKING, getBlockX(), getBlockZ());
	}

	private void comeDown() {
		if (isOnGround() || getY() - groundY() < 0.05) {
			perch();
		} else {
			setVelocity(getVelocity().multiply(0.6, 0, 0.6).add(0, -0.15, 0));
		}
	}

	@Override
	protected void tickWindup(Move running, int ticksLeft) {
		LivingEntity target = getTarget();
		if (running == SHRIEK && target != null && getWorld() instanceof ServerWorld world) {
			// The shadow gathers at the target's feet, tighter as the dive nears.
			double radius = 0.6 + 1.2 * ticksLeft / SHRIEK_TICKS;
			Telegraph.ring(world, target.getPos(), radius, ParticleTypes.SQUID_INK);
			world.spawnParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 0.05, target.getZ(), 3, 0.3, 0, 0.3, 0);
		}
	}

	@Override
	protected void performMove(Move landed) {
		if (landed == SHRIEK && getTarget() != null) {
			setState(STATE_DIVING);
			diveTicks = DIVE_TICKS;
			sinceDive = 0;
			playSound(ReachMobSounds.ASHEN_CARRION_DIVE, 1.5f, 1.0f);
		}
	}

	private void tickDive(@Nullable LivingEntity target) {
		if (target == null || --diveTicks <= 0) {
			endDive();
			return;
		}
		Vec3d aim = target.getPos().add(0, target.getHeight() * 0.5, 0);
		Vec3d velocity = aim.subtract(getPos()).normalize().multiply(DIVE_SPEED);
		setVelocity(velocity);
		faceVelocity(velocity);
		if (getBoundingBox().expand(0.3).intersects(target.getBoundingBox())) {
			swingHand(Hand.MAIN_HAND);
			triggerAnim(ACTION, "attack.peck");
			tryAttack(target);
			endDive();
		}
	}

	private void endDive() {
		setState(STATE_NONE);
		setVelocity(getVelocity().multiply(-0.3, 0, -0.3).add(0, 0.6, 0));
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		boolean hurt = super.damage(source, amount);
		if (hurt && isPerched() && !getWorld().isClient) {
			setState(STATE_NONE);
			setVelocity(0, 0.5, 0);
		}
		return hurt;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("Perched", isPerched());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.getBoolean("Perched")) {
			perch();
		}
	}

	@Nullable
	@Override
	protected RawAnimation stateAnimation(byte state) {
		return state == STATE_PERCHED ? PERCHED : state == STATE_DIVING ? DIVING : null;
	}

	@Nullable
	@Override
	public Move meleeMove() {
		return null;
	}

	@Override
	protected List<String> actionAnimations() {
		return List.of("attack.peck", "attack.shriek");
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ReachMobSounds.ASHEN_CARRION_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ReachMobSounds.ASHEN_CARRION_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ReachMobSounds.ASHEN_CARRION_DEATH;
	}
}
