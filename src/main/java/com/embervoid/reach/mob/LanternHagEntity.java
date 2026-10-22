package com.embervoid.reach.mob;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

import java.util.List;

/** Woods ranged mob (DESIGN §4.5): keeps her distance and throws fire flasks. */
public class LanternHagEntity extends ReachMob {
	static final Move MELEE = new Move("attack.throw", 15, 50, SoundEvents.ENTITY_WITCH_CELEBRATE, 0.8f);

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
	public Move meleeMove() {
		return MELEE;
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
}
