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

/** Woods fodder (DESIGN §4.5): a hanged poacher; placed ones hang in cages and drop on a player passing under. */
public class WitheredThrallEntity extends ReachMob {
	static final Move MELEE = new Move("attack.swing", 10, 30, SoundEvents.ENTITY_ZOMBIE_VILLAGER_AMBIENT, 0.7f);

	public WitheredThrallEntity(EntityType<? extends WitheredThrallEntity> type, World world) {
		super(type, world);
		experiencePoints = 8;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 18)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.5)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.24)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32);
	}

	@Override
	public Move meleeMove() {
		return MELEE;
	}

	@Override
	protected List<String> actionAnimations() {
		return List.of("attack.swing", "attack.drop");
	}

	@Override
	public EntityGroup getGroup() {
		return EntityGroup.UNDEAD;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_ZOMBIE_VILLAGER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_ZOMBIE_VILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_ZOMBIE_VILLAGER_DEATH;
	}

	@Override
	public float getSoundPitch() {
		return super.getSoundPitch() * 0.8f;
	}
}
