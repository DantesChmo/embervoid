package com.embervoid.reach.mob;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
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
	static final Move BITE = new Move("attack.bite", 6, 20, SoundEvents.ENTITY_WOLF_GROWL, 0.7f);

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
		playSound(SoundEvents.ENTITY_WOLF_HOWL, 1.5f, 0.6f);
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
		}
		return hit;
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
		return SoundEvents.ENTITY_WOLF_GROWL;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_WOLF_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_WOLF_DEATH;
	}

	@Override
	public float getSoundPitch() {
		return super.getSoundPitch() * 0.7f;
	}
}
