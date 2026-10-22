package com.embervoid.reach.mob;

import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

import java.util.List;

/** Necropolis harasser (DESIGN §4.5): a moth drawn to light; blinds with a pollen burst. */
public class CryptMothEntity extends FlyingReachMob {
	static final Move MELEE = new Move("attack.bite", 6, 25, null, 1.0f);

	public CryptMothEntity(EntityType<? extends CryptMothEntity> type, World world) {
		super(type, world);
		experiencePoints = 8;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 10)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.5)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16);
	}

	@Override
	public Move meleeMove() {
		return MELEE;
	}

	@Override
	protected List<String> actionAnimations() {
		return List.of("attack.bite", "attack.burst");
	}

	@Override
	public EntityGroup getGroup() {
		return EntityGroup.ARTHROPOD;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_BAT_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_BAT_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_BAT_DEATH;
	}

	@Override
	public float getSoundPitch() {
		return super.getSoundPitch() * 0.6f;
	}
}
