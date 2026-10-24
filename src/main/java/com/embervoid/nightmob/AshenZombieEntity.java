package com.embervoid.nightmob;

import com.embervoid.entity.mob.MobAnimations;
import com.embervoid.fx.particle.EmberParticles;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
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
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The full moon's zombie (DESIGN §3.3): one per Curse of the Night stack comes for the cursed player. A sturdier
 * zombie that the sun does not burn, so a wave still standing at dawn keeps walking. No natural spawns; the wave
 * sets its level (spot level + {@link NightMobConfig#zombieLevelBonus}).
 *
 * <p>Animations (PLAN 19.3, {@link MobAnimations}): {@code misc.hurt} on a hit, {@code misc.stagger} on one of
 * {@link #STAGGER_SHARE} of its health or more (visual only), and on death it topples forward ({@code misc.death}) and
 * lies there {@link #DEATH_TICKS} ticks, ash flaking off it. Ash flakes drift off it as it walks.
 */
public class AshenZombieEntity extends HostileEntity implements GeoEntity {
	/** How long the body lies there after death: the length of {@code misc.death} (2 s). */
	public static final int DEATH_TICKS = 40;
	/** A hit of this share of its max health shows the stagger instead of the flinch. */
	public static final float STAGGER_SHARE = 0.25f;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private int lastHurtAnimation = -100;
	private int hurtFlinches;

	public AshenZombieEntity(EntityType<? extends AshenZombieEntity> type, World world) {
		super(type, world);
		experiencePoints = 20;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 24)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4)
				.add(EntityAttributes.GENERIC_ARMOR, 2)
				// A wave spawns 16–32 blocks from its player.
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.3);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(2, new MeleeAttackGoal(this, 1.0, true));
		goalSelector.add(6, new WanderAroundFarGoal(this, 0.8));
		goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8));
		goalSelector.add(8, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this));
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
		targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		if (getWorld().isClient && random.nextInt(4) == 0) {
			getWorld().addParticle(ParticleTypes.ASH, getParticleX(0.5), getRandomBodyY(), getParticleZ(0.5), 0, 0, 0);
		}
		if (getWorld() instanceof ServerWorld world && isAlive() && random.nextInt(8) == 0) {
			EmberParticles.spawn(world, EmberParticles.ASH_FLAKE, getPos().add(0, getHeight() * 0.7, 0), 1, 0.3, 0.01);
		}
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		boolean hurt = super.damage(source, amount);
		if (MobAnimations.mayFlinch(this, hurt, amount, lastHurtAnimation)) {
			lastHurtAnimation = age;
			hurtFlinches++;
			triggerAnim(MobAnimations.HURT_CONTROLLER, amount >= getMaxHealth() * STAGGER_SHARE ? MobAnimations.STAGGER : MobAnimations.HURT);
			if (getWorld() instanceof ServerWorld world) {
				EmberParticles.spawn(world, EmberParticles.ASH_FLAKE, getPos().add(0, getHeight() * 0.6, 0), 4, 0.4, 0.02);
			}
		}
		return hurt;
	}

	/** Flinches and staggers shown so far (tests). */
	public int hurtFlinches() {
		return hurtFlinches;
	}

	@Override
	public void onDeath(DamageSource source) {
		boolean firstDeath = !dead;
		super.onDeath(source);
		if (firstDeath && !getWorld().isClient) {
			triggerAnim(MobAnimations.DEATH_CONTROLLER, MobAnimations.DEATH);
		}
	}

	/** How long the body stays after death for its death animation, in ticks. */
	public int deathTicks() {
		return DEATH_TICKS;
	}

	/** It topples forward and lands at 0.95 s (misc.death): a puff of ash and soot, then flakes off the body. */
	@Override
	protected void updatePostDeath() {
		deathTime++;
		if (getWorld() instanceof ServerWorld world && !isRemoved()) {
			if (deathTime % 5 == 0) {
				EmberParticles.spawn(world, EmberParticles.ASH_FLAKE, getPos().add(0, 0.4, 0), 2, 0.6, 0.02);
			}
			if (deathTime == 19) {
				EmberParticles.ringOut(world, EmberParticles.SOOT, getPos().add(0, 0.2, 0), 8, 0.08, 0.03);
				EmberParticles.spawn(world, EmberParticles.ASH_FLAKE, getPos().add(0, 0.3, 0), 12, 0.7, 0.03);
			}
		}
		MobAnimations.tickDeath(this, deathTime, deathTicks());
	}

	/** A wave mob must stay in the overworld, where its wave is counted. */
	@Override
	public boolean canUsePortals() {
		return false;
	}

	@Override
	public EntityGroup getGroup() {
		return EntityGroup.UNDEAD;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return NightMobSounds.ASHEN_ZOMBIE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return NightMobSounds.ASHEN_ZOMBIE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return NightMobSounds.ASHEN_ZOMBIE_DEATH;
	}

	@Override
	public float getSoundPitch() {
		return super.getSoundPitch() * 0.8f;
	}

	/** Walk / idle, the flinch or stagger, the swing, the death last: later controllers win on the bones they animate. */
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(DefaultAnimations.genericWalkIdleController(this));
		controllers.add(MobAnimations.hurtController(this, MobAnimations.HURT, MobAnimations.STAGGER));
		controllers.add(DefaultAnimations.genericAttackAnimation(this, DefaultAnimations.ATTACK_SWING));
		controllers.add(MobAnimations.deathController(this));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animations;
	}
}
