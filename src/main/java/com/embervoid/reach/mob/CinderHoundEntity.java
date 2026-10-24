package com.embervoid.reach.mob;

import com.embervoid.fx.particle.EmberParticles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Citadel pack fodder (DESIGN §4.5): a fast burning hound. Its bite (6-tick lunge) sets the target alight for
 * {@link #BITE_FIRE_SECONDS} s. When it picks a new target it howls, and idle hounds within {@link #HOWL_RANGE} join
 * the hunt (no chain howls: the called hounds do not call others).
 */
public class CinderHoundEntity extends ReachMob {
	public static final double HOWL_RANGE = 24;
	public static final int BITE_FIRE_SECONDS = 2;
	public static final int BITE_WINDUP = 6;
	/** Ticks the body stays for {@code misc.death}: the legs fold and it drops onto its side, 1.6 s. */
	public static final int DEATH_TICKS = 32;
	static final Move BITE = new Move("attack.bite", BITE_WINDUP, 20, ReachMobSounds.CINDER_HOUND_SNARL, 1.0f);

	public CinderHoundEntity(EntityType<? extends CinderHoundEntity> type, World world) {
		super(type, world);
		experiencePoints = 7;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 14)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32);
	}

	@Override
	protected void initAttackGoals() {
		goalSelector.add(2, new ReachMeleeGoal(this, 1.2));
	}

	@Override
	public void setTarget(@Nullable LivingEntity target) {
		boolean fresh = target != null && target != getTarget();
		super.setTarget(target);
		if (fresh && !getWorld().isClient) {
			howl(target);
		}
	}

	private void howl(LivingEntity target) {
		triggerAnim(ACTION, "misc.howl");
		playSound(ReachMobSounds.CINDER_HOUND_HOWL, 1.5f, 1.0f);
		for (CinderHoundEntity other : getWorld().getEntitiesByClass(CinderHoundEntity.class,
				getBoundingBox().expand(HOWL_RANGE), hound -> hound != this && hound.isAlive() && hound.getTarget() == null)) {
			other.joinHunt(target);
		}
	}

	/** Takes the target without howling again. */
	private void joinHunt(LivingEntity target) {
		super.setTarget(target);
	}

	@Override
	public boolean tryAttack(Entity target) {
		boolean hit = super.tryAttack(target);
		if (hit) {
			target.setOnFireFor(BITE_FIRE_SECONDS);
			if (getWorld() instanceof ServerWorld world) {
				// The jaws close in a shower of sparks.
				EmberParticles.throwAll(world, EmberParticles.CINDER_SPARK, target.getPos().add(0, target.getHeight() * 0.6, 0), 8,
						getRotationVector().multiply(0.15), 0.1);
			}
		}
		return hit;
	}

	/** Embers drift off the mane and the rib seams as it runs. */
	@Override
	public void tick() {
		super.tick();
		if (getWorld() instanceof ServerWorld world && isAlive() && age % (isMoving() ? 4 : 10) == 0) {
			EmberParticles.spawn(world, EmberParticles.EMBER, getPos().add(0, getHeight() * 0.7, 0), 1, 0.3, 0.02);
		}
	}

	private boolean isMoving() {
		return getVelocity().horizontalLengthSquared() > 1.0E-3;
	}

	@Override
	public int deathTicks() {
		return DEATH_TICKS;
	}

	/** The embers in the hide go out one by one while the body lies there. */
	@Override
	protected void tickDeath(ServerWorld world, int deathTime) {
		if (deathTime == 16) {
			EmberParticles.ringOut(world, EmberParticles.EMBER, getPos(), 12, 0.12, 0.15);
			EmberParticles.spawn(world, EmberParticles.SOOT, getPos().add(0, 0.3, 0), 5, 0.5, 0.02);
		} else if (deathTime % 3 == 0) {
			EmberParticles.spawn(world, EmberParticles.EMBER, getPos().add(0, 0.3, 0), 1, 0.5, 0.02);
		}
	}

	@Override
	public boolean isFireImmune() {
		return true;
	}

	@Override
	public Move meleeMove() {
		return BITE;
	}

	@Override
	protected List<String> actionAnimations() {
		return List.of("attack.bite", "misc.howl");
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ReachMobSounds.CINDER_HOUND_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ReachMobSounds.CINDER_HOUND_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ReachMobSounds.CINDER_HOUND_DEATH;
	}

	@Override
	protected SoundEvent stepSound() {
		return ReachMobSounds.CINDER_HOUND_STEP;
	}
}
