package com.embervoid.reach.mob;

import com.embervoid.fx.particle.EmberParticles;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * Woods ranged mob (DESIGN §4.5). Keeps {@link #MIN_RANGE}–{@link #MAX_RANGE} blocks from her target (backs off when
 * it comes closer, walks up when it is farther) and throws a {@link FireFlaskEntity} in an arc. Each throw is
 * telegraphed: she raises her lantern {@link #THROW_WINDUP} ticks before the flask leaves her hand, aimed at where
 * the target stands then.
 */
public class LanternHagEntity extends ReachMob {
	public static final int THROW_WINDUP = 15;
	public static final double MIN_RANGE = 8;
	public static final double MAX_RANGE = 12;
	/** Closer than this she only backs off: a flask at her feet would burn her too. */
	public static final double MIN_THROW_RANGE = 4;
	/** Ticks the body stays for {@code misc.death}: she goes down on her back, the lantern rolling over, 2 s. */
	public static final int DEATH_TICKS = 40;
	static final Move THROW = new Move("attack.throw", THROW_WINDUP, 50, ReachMobSounds.LANTERN_HAG_RAISE_LANTERN, 1.0f);

	public LanternHagEntity(EntityType<? extends LanternHagEntity> type, World world) {
		super(type, world);
		experiencePoints = 12;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 13)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24);
	}

	@Override
	protected void initAttackGoals() {
		goalSelector.add(2, new KeepRangeGoal(this));
	}

	@Nullable
	@Override
	public Move meleeMove() {
		return null;
	}

	/** Starts a throw at the current target (the goal calls this in range and in sight). */
	public boolean startThrow() {
		return getTarget() != null && startMove(THROW);
	}

	@Override
	protected void tickWindup(Move running, int ticksLeft) {
		if (running == THROW && getWorld() instanceof ServerWorld world) {
			// The raised lantern flares: embers and an amber mote off it.
			Vec3d hand = getPos().add(Vec3d.fromPolar(0, bodyYaw + 90).multiply(0.4)).add(0, getHeight() + 0.3, 0);
			EmberParticles.spawn(world, EmberParticles.EMBER, hand, 2, 0.08, 0.02);
			if (ticksLeft % 3 == 0) {
				EmberParticles.spawn(world, EmberParticles.AMBER_MOTE, hand, 1, 0.1, 0.01);
			}
		}
	}

	@Override
	public int deathTicks() {
		return DEATH_TICKS;
	}

	/** As she falls the lantern hits the ground and bursts, the flasks on the bandolier cracking after it. */
	@Override
	protected void tickDeath(ServerWorld world, int deathTime) {
		if (deathTime == 18) {
			Vec3d lantern = getPos().add(Vec3d.fromPolar(0, bodyYaw + 90).multiply(0.6)).add(0, 0.3, 0);
			EmberParticles.burst(world, lantern, 1.2f, 0xFFB347);
			EmberParticles.ringOut(world, EmberParticles.EMBER, lantern, 14, 0.15, 0.2);
			EmberParticles.scorch(world, lantern, 1.0f);
		} else if (deathTime > 18 && deathTime % 5 == 0) {
			EmberParticles.spawn(world, EmberParticles.EMBER, getPos().add(0, 0.3, 0), 2, 0.5, 0.03);
		}
	}

	@Override
	protected void performMove(Move landed) {
		LivingEntity target = getTarget();
		if (landed != THROW || target == null || !target.isAlive()) {
			return;
		}
		FireFlaskEntity flask = new FireFlaskEntity(getWorld(), this);
		double dx = target.getX() - getX();
		double dy = target.getY() - flask.getY();
		double dz = target.getZ() - getZ();
		double distance = Math.sqrt(dx * dx + dz * dz);
		// The witch's arc: aim above the target by a fifth of the distance.
		flask.setVelocity(dx, dy + distance * 0.2, dz, 0.75f, 2.0f);
		getWorld().spawnEntity(flask);
		playSound(ReachMobSounds.LANTERN_HAG_THROW, 1.0f, 0.9f + random.nextFloat() * 0.2f);
	}

	@Override
	protected List<String> actionAnimations() {
		return List.of("attack.throw");
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ReachMobSounds.LANTERN_HAG_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ReachMobSounds.LANTERN_HAG_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ReachMobSounds.LANTERN_HAG_DEATH;
	}

	/**
	 * Steps straight away from the target (6 blocks), or to a random spot away from it when that path is blocked.
	 */
	void backOff(LivingEntity target) {
		Vec3d away = getPos().subtract(target.getPos()).multiply(1, 0, 1);
		if (away.lengthSquared() > 1.0E-4) {
			Vec3d spot = getPos().add(away.normalize().multiply(6));
			if (getNavigation().startMovingTo(spot.x, spot.y, spot.z, 1.2)) {
				return;
			}
		}
		Vec3d spot = NoPenaltyTargeting.findFrom(this, 16, 7, target.getPos());
		if (spot != null) {
			getNavigation().startMovingTo(spot.x, spot.y, spot.z, 1.2);
		}
	}

	/** Holds the 8–12 block band around the target and throws when it can see it. */
	static class KeepRangeGoal extends Goal {
		private final LanternHagEntity hag;
		private int repath;

		KeepRangeGoal(LanternHagEntity hag) {
			this.hag = hag;
			setControls(EnumSet.of(Control.MOVE, Control.LOOK));
		}

		@Override
		public boolean canStart() {
			LivingEntity target = hag.getTarget();
			return target != null && target.isAlive();
		}

		@Override
		public void stop() {
			hag.getNavigation().stop();
		}

		@Override
		public boolean shouldRunEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity target = hag.getTarget();
			if (target == null) {
				return;
			}
			hag.getLookControl().lookAt(target, 30, 30);
			double distance = hag.distanceTo(target);
			boolean seen = hag.getVisibilityCache().canSee(target);
			if (hag.isWindingUp()) {
				hag.getNavigation().stop();
				return;
			}
			if (--repath <= 0) {
				repath = 10;
				if (distance < MIN_RANGE) {
					hag.backOff(target);
				} else if (distance > MAX_RANGE || !seen) {
					hag.getNavigation().startMovingTo(target, 1.0);
				} else {
					hag.getNavigation().stop();
				}
			}
			if (seen && distance >= MIN_THROW_RANGE && distance <= MAX_RANGE + 2) {
				hag.startThrow();
			}
		}
	}
}
