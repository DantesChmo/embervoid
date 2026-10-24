package com.embervoid.nightmob;

import com.embervoid.entity.mob.MobAnimations;
import com.embervoid.fx.particle.EmberParticles;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

/**
 * The full moon's answer to a player with no curse (DESIGN §3.3): three of these come instead of Ashen Zombies.
 * Twice an Ashen Zombie's HP and damage; it flies and drifts through blocks like a vex, sees through walls, and
 * {@link NightMobConfig#ghostMissChance} of the hits on it pass through it harmlessly.
 *
 * <p>Animations (PLAN 19.3, {@link MobAnimations}): {@code misc.hurt} on a hit that lands (a flinch and a shiver);
 * on death it dissolves ({@code misc.death}: it rises, turns and shrinks away) over {@link #DEATH_TICKS} ticks in cold
 * flame, the souls it was leaving upward at the end. Cold flame licks off it as it drifts.
 */
public class GhostEntity extends HostileEntity implements GeoEntity {
	/** Speed factor of the move control: about 4.4 blocks/s chasing (a vex is 1.0, about 11 blocks/s). */
	private static final double CHASE_SPEED = 0.4;
	private static final double WANDER_SPEED = 0.2;
	/** One touch per 2 s (R40.1c: with one a second, three Ghosts out-traded any day-24 player). */
	private static final int ATTACK_COOLDOWN_TICKS = 40;
	/** How long the body stays after death: the length of {@code misc.death} (1.6 s). */
	public static final int DEATH_TICKS = 32;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private int lastHurtAnimation = -100;
	private int hurtFlinches;

	public GhostEntity(EntityType<? extends GhostEntity> type, World world) {
		super(type, world);
		moveControl = new GhostMoveControl(this);
		experiencePoints = 40;
	}

	/** Twice the Ashen Zombie's HP and damage. */
	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 48)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(2, new ChaseGoal(this));
		goalSelector.add(7, new DriftGoal(this));
		goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8));
		targetSelector.add(1, new RevengeGoal(this));
		// Through walls: no line of sight needed.
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, false, false, null));
	}

	@Override
	public void move(MovementType type, Vec3d movement) {
		super.move(type, movement);
		checkBlockCollision();
	}

	@Override
	public void tick() {
		// Like a vex: no block collision for this tick's movement, no gravity.
		noClip = true;
		super.tick();
		noClip = false;
		setNoGravity(true);
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		if (getWorld().isClient && random.nextInt(5) == 0) {
			getWorld().addParticle(ParticleTypes.SOUL, getParticleX(0.4), getRandomBodyY(), getParticleZ(0.4), 0, 0.02, 0);
		}
		if (getWorld() instanceof ServerWorld world && isAlive() && random.nextInt(6) == 0) {
			EmberParticles.spawn(world, EmberParticles.COLD_FLAME, getPos().add(0, getHeight() * 0.3, 0), 1, 0.3, 0.01);
		}
	}

	/** Some attacks pass through it: the hit does nothing, with a puff and a whoosh instead of the hurt sound. */
	@Override
	public boolean damage(DamageSource source, float amount) {
		if (!getWorld().isClient && source.getAttacker() != null && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)
				// Only a hit that would count, not one swallowed by the hurt cooldown anyway.
				&& timeUntilRegen <= 10 && random.nextFloat() < NightMobConfig.get().ghostMissChance()) {
			miss();
			return false;
		}
		boolean hurt = super.damage(source, amount);
		if (MobAnimations.mayFlinch(this, hurt, amount, lastHurtAnimation)) {
			lastHurtAnimation = age;
			hurtFlinches++;
			triggerAnim(MobAnimations.HURT_CONTROLLER, MobAnimations.HURT);
			if (getWorld() instanceof ServerWorld world) {
				EmberParticles.spawn(world, EmberParticles.COLD_FLAME, getPos().add(0, getHeight() * 0.5, 0), 5, 0.4, 0.03);
			}
		}
		return hurt;
	}

	/** Flinches shown so far (tests). */
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

	/** The dissolve: cold flame streams off the rising, shrinking shade; at the end its souls fly up and it is gone. */
	@Override
	protected void updatePostDeath() {
		deathTime++;
		if (getWorld() instanceof ServerWorld world && !isRemoved()) {
			if (deathTime % 2 == 0) {
				double rise = deathTime / (double) deathTicks();
				EmberParticles.spawn(world, EmberParticles.COLD_FLAME, getPos().add(0, 0.4 + rise * 1.2, 0), 2, 0.5 * (1 - rise * 0.6), 0.03);
			}
			if (deathTime == deathTicks() - 1) {
				Vec3d top = getPos().add(0, 1.8, 0);
				EmberParticles.burst(world, top, 1.2f, 0x9fe8ff);
				EmberParticles.spawn(world, EmberParticles.COLD_FLAME, top, 14, 0.4, 0.05);
				EmberParticles.souls(world, top, top.add(0, 6, 0), 6);
			}
		}
		MobAnimations.tickDeath(this, deathTime, deathTicks());
	}

	private void miss() {
		if (getWorld() instanceof ServerWorld world) {
			world.spawnParticles(ParticleTypes.POOF, getX(), getBodyY(0.5), getZ(), 8, 0.3, 0.4, 0.3, 0.02);
			world.playSound(null, getX(), getY(), getZ(), NightMobSounds.GHOST_PHASE,
					SoundCategory.HOSTILE, 1.0f, 0.6f);
		}
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		return source.isOf(DamageTypes.IN_WALL) || source.isOf(DamageTypes.DROWN) || super.isInvulnerableTo(source);
	}

	@Override
	public boolean canBreatheInWater() {
		return true;
	}

	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
		return false;
	}

	@Override
	protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
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
		return NightMobSounds.GHOST_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return NightMobSounds.GHOST_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return NightMobSounds.GHOST_DEATH;
	}

	@Override
	public float getSoundPitch() {
		return super.getSoundPitch() * 0.5f;
	}

	/** Fly / idle, the flinch, the claws, the dissolve last: later controllers win on the bones they animate. */
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(DefaultAnimations.genericFlyIdleController(this));
		controllers.add(MobAnimations.hurtController(this, MobAnimations.HURT));
		controllers.add(DefaultAnimations.genericAttackAnimation(this, DefaultAnimations.ATTACK_SWING));
		controllers.add(MobAnimations.deathController(this));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animations;
	}

	/** Flies straight at the move target, ignoring blocks (the vex's move control, at the ghost's own speed). */
	static class GhostMoveControl extends MoveControl {
		private final GhostEntity ghost;

		GhostMoveControl(GhostEntity ghost) {
			super(ghost);
			this.ghost = ghost;
		}

		@Override
		public void tick() {
			if (state != State.MOVE_TO) {
				return;
			}
			Vec3d toTarget = new Vec3d(targetX - ghost.getX(), targetY - ghost.getY(), targetZ - ghost.getZ());
			double distance = toTarget.length();
			if (distance < ghost.getBoundingBox().getAverageSideLength()) {
				state = State.WAIT;
				ghost.setVelocity(ghost.getVelocity().multiply(0.5));
				return;
			}
			ghost.setVelocity(ghost.getVelocity().add(toTarget.multiply(speed * 0.05 / distance)));
			LivingEntity target = ghost.getTarget();
			double dx = target == null ? ghost.getVelocity().x : target.getX() - ghost.getX();
			double dz = target == null ? ghost.getVelocity().z : target.getZ() - ghost.getZ();
			ghost.setYaw(-(float) MathHelper.atan2(dx, dz) * MathHelper.DEGREES_PER_RADIAN);
			ghost.bodyYaw = ghost.getYaw();
		}
	}

	/** Flies at the target's body and strikes when in reach, once a second. */
	static class ChaseGoal extends Goal {
		private final GhostEntity ghost;
		private int cooldown;

		ChaseGoal(GhostEntity ghost) {
			this.ghost = ghost;
			setControls(EnumSet.of(Control.MOVE, Control.LOOK));
		}

		@Override
		public boolean canStart() {
			LivingEntity target = ghost.getTarget();
			return target != null && target.isAlive();
		}

		@Override
		public boolean shouldContinue() {
			return canStart();
		}

		@Override
		public boolean shouldRunEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity target = ghost.getTarget();
			if (target == null) {
				return;
			}
			ghost.getLookControl().lookAt(target, 30, 30);
			Vec3d aim = target.getPos().add(0, target.getHeight() * 0.4, 0);
			ghost.getMoveControl().moveTo(aim.x, aim.y, aim.z, CHASE_SPEED);
			if (cooldown > 0) {
				cooldown--;
				return;
			}
			double reach = ghost.getWidth() * 2 * ghost.getWidth() * 2 + target.getWidth();
			if (ghost.squaredDistanceTo(target) <= reach + 1) {
				ghost.swingHand(Hand.MAIN_HAND);
				ghost.tryAttack(target);
				cooldown = ATTACK_COOLDOWN_TICKS;
			}
		}
	}

	/** Without a target: drifts to a random spot nearby, a few blocks above the ground. */
	static class DriftGoal extends Goal {
		private final GhostEntity ghost;

		DriftGoal(GhostEntity ghost) {
			this.ghost = ghost;
			setControls(EnumSet.of(Control.MOVE));
		}

		@Override
		public boolean canStart() {
			return ghost.getTarget() == null && !ghost.getMoveControl().isMoving() && ghost.random.nextInt(7) == 0;
		}

		@Override
		public boolean shouldContinue() {
			return false;
		}

		@Override
		public void start() {
			BlockPos from = ghost.getBlockPos();
			int x = from.getX() + ghost.random.nextInt(15) - 7;
			int z = from.getZ() + ghost.random.nextInt(15) - 7;
			// Hover 1–4 blocks above the surface below the spot, but never far from where it is now.
			int ground = ghost.getWorld().getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
			int y = MathHelper.clamp(ground + 1 + ghost.random.nextInt(4), from.getY() - 4, from.getY() + 4);
			ghost.getMoveControl().moveTo(x + 0.5, y + 0.5, z + 0.5, WANDER_SPEED);
		}
	}
}
