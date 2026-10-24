package com.embervoid.reach.mob;

import com.embervoid.fx.particle.EmberParticles;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

import java.util.List;

/** Fields fodder (DESIGN §4.5): a burnt peasant with a pitchfork. Plain melee with a 10-tick raised-arm windup. */
public class CinderHuskEntity extends ReachMob {
	public static final int SWING_WINDUP = 10;
	/** Ticks the body stays for {@code misc.death}: the fall's length in the model. */
	public static final int DEATH_TICKS = 40;
	static final Move MELEE = new Move("attack.swing", SWING_WINDUP, 30, ReachMobSounds.CINDER_HUSK_WINDUP, 1.0f);

	public CinderHuskEntity(EntityType<? extends CinderHuskEntity> type, World world) {
		super(type, world);
		experiencePoints = 6;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 10)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32);
	}

	@Override
	public Move meleeMove() {
		return MELEE;
	}

	/** Embers drift off the burning body now and then. */
	@Override
	public void tick() {
		super.tick();
		if (getWorld() instanceof ServerWorld world && isAlive() && age % 9 == 0) {
			EmberParticles.spawn(world, EmberParticles.EMBER, getPos().add(0, getHeight() * 0.6, 0), 1, 0.3, 0.02);
		}
	}

	/** The fall of {@code misc.death}: 2 s face down into the ash (tools/reach_mob_art.py). */
	@Override
	public int deathTicks() {
		return DEATH_TICKS;
	}

	@Override
	protected void tickDeath(ServerWorld world, int deathTime) {
		if (deathTime == 18) {
			// It hits the ground: a puff of ash and a shower of embers.
			EmberParticles.spawn(world, EmberParticles.ASH_FLAKE, getPos().add(0, 0.2, 0), 10, 0.6, 0.03);
			EmberParticles.ringOut(world, EmberParticles.EMBER, getPos(), 10, 0.12, 0.15);
		} else if (deathTime % 6 == 0) {
			EmberParticles.spawn(world, EmberParticles.EMBER, getPos().add(0, 0.3, 0), 1, 0.5, 0.02);
		}
	}

	@Override
	protected List<String> actionAnimations() {
		return List.of("attack.swing");
	}

	@Override
	public EntityGroup getGroup() {
		return EntityGroup.UNDEAD;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ReachMobSounds.CINDER_HUSK_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ReachMobSounds.CINDER_HUSK_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ReachMobSounds.CINDER_HUSK_DEATH;
	}

	@Override
	protected SoundEvent stepSound() {
		return ReachMobSounds.CINDER_HUSK_STEP;
	}
}
