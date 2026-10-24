package com.embervoid.reach.mob;

import com.embervoid.fx.particle.EmberParticles;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * Citadel ranged mob (DESIGN §4.5): a crossbowman on the walls and the gatehouse. It never moves from its post (no
 * walking, no knockback); it turns to its target and, with a clear line, aims: a spark lights on the bolt tip
 * {@link #AIM_TICKS} ticks before the bolt flies. The bolt does {@link #BOLT_DAMAGE} and sets the target alight.
 */
public class SparkingMarksmanEntity extends ReachMob {
	public static final int AIM_TICKS = 20;
	public static final float BOLT_DAMAGE = 6;
	public static final double RANGE = 24;
	/** Ticks the body stays for {@code misc.death}: shot off its post, it goes down on its back, 2 s. */
	public static final int DEATH_TICKS = 40;
	/** Crossbow bolt speed, blocks per tick. */
	private static final float BOLT_SPEED = 3.15f;
	static final Move AIM = new Move("attack.aim", AIM_TICKS, 60, ReachMobSounds.SPARKING_MARKSMAN_SPARK, 1.0f);
	private static final byte STATE_AIMING = 1;

	public SparkingMarksmanEntity(EntityType<? extends SparkingMarksmanEntity> type, World world) {
		super(type, world);
		experiencePoints = 15;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 15)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, RANGE);
	}

	/** At its post: no movement goals at all. */
	@Override
	protected void initGoals() {
		goalSelector.add(2, new ShootGoal(this));
		goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 16));
		goalSelector.add(8, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this));
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
	}

	@Nullable
	@Override
	public Move meleeMove() {
		return null;
	}

	/** Starts aiming at the current target. */
	public boolean startAim() {
		return getTarget() != null && startMove(AIM);
	}

	public boolean isAiming() {
		return getState() == STATE_AIMING;
	}

	@Override
	protected void onMoveStarted(Move started) {
		setState(STATE_AIMING);
	}

	@Override
	public void cancelMove() {
		super.cancelMove();
		if (!getWorld().isClient) {
			setState(STATE_NONE);
		}
	}

	@Override
	protected void tickWindup(Move running, int ticksLeft) {
		LivingEntity target = getTarget();
		if (target != null) {
			getLookControl().lookAt(target, 60, 60);
		}
		if (getWorld() instanceof ServerWorld world && ticksLeft % 2 == 0) {
			// The spark on the bolt tip: cinder sparks and an ember, brighter as the shot nears.
			Vec3d tip = getEyePos().add(getRotationVector().multiply(0.9)).add(0, -0.3, 0);
			EmberParticles.spawn(world, EmberParticles.CINDER_SPARK, tip, ticksLeft < 8 ? 2 : 1, 0.03, 0.04);
			world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, tip.x, tip.y, tip.z, 1, 0.03, 0.03, 0.03, 0.02);
			if (ticksLeft % 4 == 0) {
				EmberParticles.spawn(world, EmberParticles.EMBER, tip, 1, 0.02, 0.01);
			}
		}
	}

	@Override
	public int deathTicks() {
		return DEATH_TICKS;
	}

	/** The spark dies on the dropped crossbow: a last few cinders, then dust. */
	@Override
	protected void tickDeath(ServerWorld world, int deathTime) {
		if (deathTime < 8 && deathTime % 2 == 0) {
			EmberParticles.spawn(world, EmberParticles.CINDER_SPARK, getPos().add(0, 0.8, 0), 2, 0.3, 0.05);
		} else if (deathTime == 18) {
			EmberParticles.spawn(world, EmberParticles.SOOT, getPos().add(0, 0.2, 0), 6, 0.6, 0.02);
		}
	}

	@Override
	protected void performMove(Move landed) {
		setState(STATE_NONE);
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive()) {
			return;
		}
		triggerAnim(ACTION, "attack.shoot");
		ArrowEntity bolt = new ArrowEntity(getWorld(), this);
		bolt.setPosition(getX(), getEyeY() - 0.1, getZ());
		double dx = target.getX() - bolt.getX();
		double dy = target.getBodyY(0.5) - bolt.getY();
		double dz = target.getZ() - bolt.getZ();
		double distance = Math.sqrt(dx * dx + dz * dz);
		bolt.setVelocity(dx, dy + distance * 0.02, dz, BOLT_SPEED, 1.0f);
		// Arrow damage is speed × base: this makes a full-speed bolt do BOLT_DAMAGE.
		bolt.setDamage(BOLT_DAMAGE / BOLT_SPEED);
		bolt.setOnFireFor(100);
		bolt.setShotFromCrossbow(true);
		bolt.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
		getWorld().spawnEntity(bolt);
		playSound(ReachMobSounds.SPARKING_MARKSMAN_SHOOT, 1.0f, 1.0f);
	}

	@Override
	public void takeKnockback(double strength, double x, double z) {
		// Planted on the wall.
	}

	@Override
	public List<String> toggledBones() {
		return List.of("spark");
	}

	/** The spark on the bolt tip shows while it aims. */
	@Override
	public boolean showsBone(String bone) {
		return isAiming();
	}

	@Override
	protected List<String> actionAnimations() {
		return List.of("attack.aim", "attack.shoot");
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ReachMobSounds.SPARKING_MARKSMAN_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ReachMobSounds.SPARKING_MARKSMAN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ReachMobSounds.SPARKING_MARKSMAN_DEATH;
	}

	/** Faces its target and aims whenever it has a clear line within range. */
	static class ShootGoal extends Goal {
		private final SparkingMarksmanEntity marksman;

		ShootGoal(SparkingMarksmanEntity marksman) {
			this.marksman = marksman;
			setControls(EnumSet.of(Control.LOOK));
		}

		@Override
		public boolean canStart() {
			LivingEntity target = marksman.getTarget();
			return target != null && target.isAlive();
		}

		@Override
		public boolean shouldRunEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity target = marksman.getTarget();
			if (target == null) {
				return;
			}
			marksman.getLookControl().lookAt(target, 30, 30);
			if (marksman.squaredDistanceTo(target) <= RANGE * RANGE && marksman.getVisibilityCache().canSee(target)) {
				marksman.startAim();
			}
		}
	}
}
