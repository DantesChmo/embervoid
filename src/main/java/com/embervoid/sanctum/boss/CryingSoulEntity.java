package com.embervoid.sanctum.boss;

import com.embervoid.boss.BossFight;
import com.embervoid.boss.Telegraph;
import com.embervoid.boss.TelegraphShape;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * A crying soul (DESIGN §2, PLAN 16.3): one of the Fallen Sister's tears, an add of her fight in phase 2. It circles
 * her head until she sends it; then it rears back with a shriek while a line of soul fire marks the ground from it to
 * where its target stood ({@value #DIVE_WINDUP} ticks), dives along that line ({@value #DIVE_TICKS} ticks) hitting the
 * first player it touches, hangs dazed near the floor where it landed ({@value #DAZED_TICKS} ticks, the moment to kill
 * it) and flies back to her. Any hit kills it. It has no AI of its own: the Sister drives it ({@link
 * FallenSisterEntity#tickBoss}) and it leaves when she does.
 */
public class CryingSoulEntity extends HostileEntity implements GeoEntity {
	public static final int DIVE_WINDUP = 20;
	public static final int DIVE_TICKS = 8;
	public static final int DAZED_TICKS = 40;
	/** The circle it flies around her: radius, and height above her feet. */
	public static final double ORBIT_RADIUS = 2.2;
	public static final double ORBIT_HEIGHT = 2.6;
	/** Blocks per tick flying back to the circle after a dive. */
	private static final double RETURN_SPEED = 0.45;
	/** Half the width of the line a dive marks and sweeps. */
	private static final double DIVE_HALF_WIDTH = 0.8;

	public static final String ACTION = "action";
	public static final String DIVE = "attack.dive";
	public static final String DAZED = "misc.dazed";

	public enum State { CIRCLING, WINDUP, DIVING, DAZED, RETURNING }

	private static final TrackedData<Byte> STATE = DataTracker.registerData(CryingSoulEntity.class, TrackedDataHandlerRegistry.BYTE);

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	@Nullable
	private UUID sisterUuid;
	private int index;
	private int count = 1;
	private int stateTicks;
	private Vec3d diveFrom = Vec3d.ZERO;
	private Vec3d diveTo = Vec3d.ZERO;
	private boolean struck;

	public CryingSoulEntity(EntityType<? extends CryingSoulEntity> type, World world) {
		super(type, world);
		setNoGravity(true);
		this.noClip = true;
		this.experiencePoints = 0;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 1)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32);
	}

	@Override
	protected void initDataTracker() {
		super.initDataTracker();
		dataTracker.startTracking(STATE, (byte) State.CIRCLING.ordinal());
	}

	@Override
	protected void initGoals() {
	}

	/** Makes it one of {@code count} souls circling {@code sister}, at place {@code index} of the circle. */
	void bind(FallenSisterEntity sister, int index, int count) {
		this.sisterUuid = sister.getUuid();
		this.index = index;
		this.count = Math.max(1, count);
	}

	@Nullable
	private FallenSisterEntity sister() {
		return sisterUuid != null && getWorld() instanceof ServerWorld world && world.getEntity(sisterUuid) instanceof FallenSisterEntity sister
				? sister : null;
	}

	public State state() {
		return State.values()[dataTracker.get(STATE)];
	}

	private void setState(State state) {
		dataTracker.set(STATE, (byte) state.ordinal());
		stateTicks = 0;
	}

	/** Whether it circles her, ready to be sent. */
	public boolean isCircling() {
		return state() == State.CIRCLING;
	}

	/**
	 * Starts a dive at {@code target}: the windup marks a line on the ground from below the soul to where the target
	 * stands now; the dive follows it after the windup. Returns false if the soul is not circling.
	 */
	public boolean startDive(LivingEntity target) {
		if (!isCircling() || getWorld().isClient) {
			return false;
		}
		setState(State.WINDUP);
		Vec3d to = target.getPos().add(0, 0.3, 0);
		TelegraphShape line = TelegraphShape.line(new Vec3d(getX(), target.getY(), getZ()), target.getPos(), DIVE_HALF_WIDTH);
		triggerAnim(ACTION, DIVE);
		Telegraph.windup(this, line, DIVE_WINDUP, null, SanctumBossSounds.CRYING_SOUL_SHRIEK, ParticleTypes.SOUL_FIRE_FLAME, () -> {
			diveFrom = getPos();
			diveTo = to;
			struck = false;
			setState(State.DIVING);
			playSound(SanctumBossSounds.CRYING_SOUL_DIVE, 1.5f, 1.0f);
		});
		return true;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(getWorld() instanceof ServerWorld world) || isRemoved() || isDead()) {
			return;
		}
		FallenSisterEntity sister = sister();
		if (sister == null || !sister.isAlive()) {
			discard();
			return;
		}
		stateTicks++;
		switch (state()) {
			case CIRCLING -> moveTo(orbitPoint(sister), 0.6);
			case WINDUP -> {
				// The glowing trail it draws as it rears back.
				world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.3, getZ(), 2, 0.15, 0.15, 0.15, 0.01);
				world.spawnParticles(ParticleTypes.END_ROD, getX(), getY() + 0.3, getZ(), 1, 0.1, 0.1, 0.1, 0);
			}
			case DIVING -> {
				float progress = Math.min(1, stateTicks / (float) DIVE_TICKS);
				Vec3d at = diveFrom.lerp(diveTo, progress);
				setPosition(at.x, at.y, at.z);
				face(diveTo.subtract(diveFrom));
				world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.3, getZ(), 3, 0.1, 0.1, 0.1, 0.01);
				if (!struck) {
					for (PlayerEntity player : world.getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(0.5),
							player -> player.isAlive() && !player.isSpectator() && !player.isCreative())) {
						sister.diveHit(player);
						struck = true;
						break;
					}
				}
				if (stateTicks >= DIVE_TICKS) {
					setState(State.DAZED);
				}
			}
			case DAZED -> {
				setPosition(diveTo.x, diveTo.y + 0.4 + 0.1 * Math.sin(age * 0.3), diveTo.z);
				if (age % 6 == 0) {
					world.spawnParticles(ParticleTypes.SOUL, getX(), getY() + 0.4, getZ(), 1, 0.2, 0.1, 0.2, 0.01);
				}
				if (stateTicks >= DAZED_TICKS) {
					setState(State.RETURNING);
				}
			}
			case RETURNING -> {
				Vec3d point = orbitPoint(sister);
				if (getPos().distanceTo(point) < 0.6) {
					setState(State.CIRCLING);
				} else {
					moveTo(point, RETURN_SPEED);
				}
			}
		}
	}

	/**
	 * Its place on the circle around her head now, turning slowly, bobbing; pulled in from the fog wall when she stands
	 * by it (the fight puts an add that strays past the wall back in the middle).
	 */
	private Vec3d orbitPoint(FallenSisterEntity sister) {
		double angle = sister.age * 0.05 + Math.PI * 2 * index / count;
		Vec3d point = sister.getPos().add(Math.cos(angle) * ORBIT_RADIUS, ORBIT_HEIGHT + 0.2 * Math.sin(sister.age * 0.15 + index),
				Math.sin(angle) * ORBIT_RADIUS);
		BossFight fight = sister.fight();
		if (fight != null) {
			Vec3d center = fight.centerPos();
			Vec3d out = new Vec3d(point.x - center.x, 0, point.z - center.z);
			double limit = fight.spec().radius() - 0.5;
			if (out.length() > limit) {
				out = out.multiply(limit / out.length());
				point = new Vec3d(center.x + out.x, point.y, center.z + out.z);
			}
		}
		return point;
	}

	/** Steps toward a point, at most {@code speed} blocks this tick, facing the way it goes. */
	private void moveTo(Vec3d point, double speed) {
		Vec3d step = point.subtract(getPos());
		double length = step.length();
		if (length > speed) {
			step = step.multiply(speed / length);
		}
		if (step.horizontalLengthSquared() > 1e-4) {
			face(step);
		}
		setPosition(getX() + step.x, getY() + step.y, getZ() + step.z);
	}

	private void face(Vec3d direction) {
		float yaw = (float) (MathHelper.atan2(direction.z, direction.x) * 180 / Math.PI) - 90;
		setYaw(yaw);
		setHeadYaw(yaw);
		setBodyYaw(yaw);
	}

	/** Any hit kills it, a fist included; falls, fire and walls do nothing. */
	@Override
	public boolean damage(DamageSource source, float amount) {
		if (source.isOf(DamageTypes.FALL) || source.isOf(DamageTypes.IN_WALL) || source.isOf(DamageTypes.IN_FIRE)
				|| source.isOf(DamageTypes.ON_FIRE) || amount <= 0) {
			return false;
		}
		return super.damage(source, Math.max(amount, getMaxHealth() * 2));
	}

	/** It bursts at once: no corpse lying about. */
	@Override
	protected void updatePostDeath() {
		if (++deathTime >= 2 && getWorld() instanceof ServerWorld world && !isRemoved()) {
			world.spawnParticles(ParticleTypes.SOUL, getX(), getY() + 0.3, getZ(), 8, 0.2, 0.2, 0.2, 0.03);
			world.spawnParticles(ParticleTypes.SMALL_FLAME, getX(), getY() + 0.3, getZ(), 4, 0.15, 0.15, 0.15, 0.01);
			remove(RemovalReason.KILLED);
		}
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	@Override
	public boolean canPickUpLoot() {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SanctumBossSounds.CRYING_SOUL_AMBIENT;
	}

	/** One hit kills it: the death sound alone. */
	@Override
	@Nullable
	protected SoundEvent getHurtSound(DamageSource source) {
		return null;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SanctumBossSounds.CRYING_SOUL_DEATH;
	}

	// --- Animation ---

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "main", 3, state -> state.setAndContinue(
				state.getAnimatable().state() == State.DAZED ? RawAnimation.begin().thenLoop(DAZED) : RawAnimation.begin().thenLoop("misc.idle"))));
		controllers.add(new AnimationController<>(this, ACTION, 0, state -> PlayState.STOP)
				.triggerableAnim(DIVE, RawAnimation.begin().thenPlay(DIVE)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animations;
	}
}
