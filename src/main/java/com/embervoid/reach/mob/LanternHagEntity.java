package com.embervoid.reach.mob;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
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
	static final Move THROW = new Move("attack.throw", THROW_WINDUP, 50, SoundEvents.ENTITY_WITCH_CELEBRATE, 0.8f);

	public LanternHagEntity(EntityType<? extends LanternHagEntity> type, World world) {
		super(type, world);
		experiencePoints = 12;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 16)
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
			// The raised lantern flares.
			Vec3d hand = getPos().add(Vec3d.fromPolar(0, bodyYaw + 90).multiply(0.4)).add(0, getHeight() + 0.3, 0);
			world.spawnParticles(ParticleTypes.FLAME, hand.x, hand.y, hand.z, 2, 0.08, 0.08, 0.08, 0.01);
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
		playSound(SoundEvents.ENTITY_WITCH_THROW, 1.0f, 0.8f + random.nextFloat() * 0.4f);
	}

	@Override
	protected List<String> actionAnimations() {
		return List.of("attack.throw");
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_WITCH_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_WITCH_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_WITCH_DEATH;
	}

	@Override
	public float getSoundPitch() {
		return super.getSoundPitch() * 0.8f;
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
					Vec3d away = NoPenaltyTargeting.findFrom(hag, 16, 7, target.getPos());
					if (away != null) {
						hag.getNavigation().startMovingTo(away.x, away.y, away.z, 1.2);
					}
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
