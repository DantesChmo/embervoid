package com.embervoid.entity.mob;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Fast, weak undead that hunts in packs of 3–5 at night and underground (DESIGN §3.2). When one finds a target the
 * rest of the pack nearby joins in. Burns in daylight. On death it bursts into a cloud of ash that blinds whoever
 * stands in it, so killing the first ghoul of a pack blinds you for the next ones.
 */
public class AshGhoulEntity extends HostileEntity implements GeoEntity {
	/** How far the pack call reaches when a ghoul picks a target. */
	private static final double PACK_RANGE = 16;
	private static final float CLOUD_RADIUS = 2.5f;
	private static final int CLOUD_TICKS = 80;
	private static final int BLINDNESS_TICKS = 60;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);

	public AshGhoulEntity(EntityType<? extends AshGhoulEntity> type, World world) {
		super(type, world);
		experiencePoints = 5;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 12)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.33)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.5)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(2, new MeleeAttackGoal(this, 1.1, false));
		goalSelector.add(6, new WanderAroundFarGoal(this, 0.9));
		goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8));
		goalSelector.add(8, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this).setGroupRevenge());
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
		targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
	}

	@Override
	public void setTarget(@Nullable LivingEntity target) {
		boolean fresh = target != null && target != getTarget();
		super.setTarget(target);
		if (fresh && !getWorld().isClient) {
			callPack(target);
		}
	}

	/** Gives idle ghouls nearby the same target. */
	private void callPack(LivingEntity target) {
		for (AshGhoulEntity other : getWorld().getEntitiesByClass(AshGhoulEntity.class,
				getBoundingBox().expand(PACK_RANGE), ghoul -> ghoul != this && ghoul.getTarget() == null)) {
			other.setTargetQuietly(target);
		}
	}

	/** Sets the target without calling the pack again (no chain reaction across the whole map). */
	private void setTargetQuietly(LivingEntity target) {
		super.setTarget(target);
	}

	@Override
	public void tickMovement() {
		if (isAlive() && isAffectedByDaylight()) {
			setOnFireFor(8);
		}
		super.tickMovement();
	}

	@Override
	public void onDeath(DamageSource source) {
		boolean firstDeath = !dead;
		super.onDeath(source);
		if (firstDeath && getWorld() instanceof ServerWorld world) {
			burstIntoAsh(world);
		}
	}

	private void burstIntoAsh(ServerWorld world) {
		AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(world, getX(), getY(), getZ());
		cloud.setOwner(this);
		cloud.setParticleType(ParticleTypes.LARGE_SMOKE);
		cloud.setRadius(CLOUD_RADIUS);
		cloud.setDuration(CLOUD_TICKS);
		cloud.setWaitTime(0);
		cloud.setRadiusGrowth(-CLOUD_RADIUS / (CLOUD_TICKS * 2));
		cloud.addEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, BLINDNESS_TICKS));
		world.spawnEntity(cloud);
		world.spawnParticles(ParticleTypes.ASH, getX(), getBodyY(0.5), getZ(), 80, 1.0, 0.8, 1.0, 0.02);
		world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getBodyY(0.5), getZ(), 20, 0.6, 0.5, 0.6, 0.02);
		world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.HOSTILE, 1.0f, 0.6f);
	}

	@Override
	public EntityGroup getGroup() {
		return EntityGroup.UNDEAD;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_HUSK_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_HUSK_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_HUSK_DEATH;
	}

	@Override
	public float getSoundPitch() {
		return super.getSoundPitch() * 1.3f;
	}

	@Override
	public int getLimitPerChunk() {
		return 5;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(DefaultAnimations.genericWalkIdleController(this));
		controllers.add(DefaultAnimations.genericAttackAnimation(this, DefaultAnimations.ATTACK_SWING));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animations;
	}
}
