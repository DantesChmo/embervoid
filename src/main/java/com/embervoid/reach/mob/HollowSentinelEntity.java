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

/** Necropolis tank (DESIGN §4.5): spearman behind a tower shield. */
public class HollowSentinelEntity extends ReachMob {
	static final Move MELEE = new Move("attack.thrust", 12, 40, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f);

	public HollowSentinelEntity(EntityType<? extends HollowSentinelEntity> type, World world) {
		super(type, world);
		experiencePoints = 30;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 45)
				.add(EntityAttributes.GENERIC_ARMOR, 10)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.9)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24);
	}

	@Override
	public Move meleeMove() {
		return MELEE;
	}

	@Override
	protected List<String> actionAnimations() {
		return List.of("attack.thrust", "attack.bash");
	}

	@Override
	public EntityGroup getGroup() {
		return EntityGroup.UNDEAD;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_SKELETON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_WITHER_SKELETON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_WITHER_SKELETON_DEATH;
	}

	@Override
	public float getSoundPitch() {
		return super.getSoundPitch() * 0.7f;
	}
}
