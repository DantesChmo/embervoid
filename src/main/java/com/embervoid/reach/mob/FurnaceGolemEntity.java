package com.embervoid.reach.mob;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Citadel tank (DESIGN §4.5): a walking furnace. A two-armed slam (9 damage, 14-tick windup). Every hit it takes
 * stokes it; at {@link #HITS_TO_BLAST} hits it glows for {@link #HEAT_TICKS} ticks (feet planted) and lets out a fire
 * wave: {@link #WAVE_DAMAGE_FACTOR} of its attack damage as fire damage, {@link #WAVE_FIRE_SECONDS} s of burning and
 * a push to everything within {@link #WAVE_RADIUS}. Then it starts cold again. Fire does not hurt it.
 */
public class FurnaceGolemEntity extends ReachMob {
	public static final int HITS_TO_BLAST = 4;
	public static final int HEAT_TICKS = 25;
	public static final double WAVE_RADIUS = 4;
	public static final float WAVE_DAMAGE_FACTOR = 0.7f;
	public static final int WAVE_FIRE_SECONDS = 4;
	static final Move SLAM = new Move("attack.slam", 14, 50, ReachMobSounds.FURNACE_GOLEM_WINDUP, 1.0f);
	static final Move HEAT = new Move("misc.heat", HEAT_TICKS, 0, ReachMobSounds.FURNACE_GOLEM_HEAT, 1.0f);
	private static final byte STATE_HEATING = 1;

	private int heat;

	public FurnaceGolemEntity(EntityType<? extends FurnaceGolemEntity> type, World world) {
		super(type, world);
		experiencePoints = 30;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 30)
				.add(EntityAttributes.GENERIC_ARMOR, 12)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.9)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.5)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.15)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24);
	}

	/** Hits taken since the last fire wave. */
	public int heat() {
		return heat;
	}

	public boolean isHeating() {
		return getState() == STATE_HEATING;
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		boolean hurt = super.damage(source, amount);
		if (hurt && !getWorld().isClient && isAlive() && !isHeating() && ++heat >= HITS_TO_BLAST) {
			cancelMove();
			resetMoveCooldown();
			startMove(HEAT);
		}
		return hurt;
	}

	@Override
	protected void onMoveStarted(Move started) {
		if (started == HEAT) {
			setState(STATE_HEATING);
		}
	}

	@Override
	protected boolean rootedDuringWindup() {
		return true;
	}

	@Override
	protected void tickWindup(Move running, int ticksLeft) {
		if (running == HEAT && getWorld() instanceof ServerWorld world) {
			world.spawnParticles(ParticleTypes.FLAME, getX(), getBodyY(0.6), getZ(), 4, 0.6, 0.6, 0.6, 0.02);
			world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + getHeight(), getZ(), 2, 0.1, 0.1, 0.1, 0.02);
		}
	}

	@Override
	protected void performMove(Move landed) {
		if (landed != HEAT) {
			super.performMove(landed);
			return;
		}
		setState(STATE_NONE);
		heat = 0;
		triggerAnim(ACTION, "attack.wave");
		playSound(ReachMobSounds.FURNACE_GOLEM_FIRE_WAVE, 2.0f, 1.0f);
		float damage = (float) getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) * WAVE_DAMAGE_FACTOR;
		for (LivingEntity victim : getWorld().getEntitiesByClass(LivingEntity.class, getBoundingBox().expand(WAVE_RADIUS),
				entity -> entity != this && entity.isAlive() && entity.squaredDistanceTo(this) <= WAVE_RADIUS * WAVE_RADIUS)) {
			victim.damage(getDamageSources().create(DamageTypes.IN_FIRE, this), damage);
			if (!victim.isFireImmune()) {
				victim.setOnFireFor(WAVE_FIRE_SECONDS);
			}
			Vec3d push = victim.getPos().subtract(getPos()).multiply(1, 0, 1);
			if (push.lengthSquared() > 1.0E-4) {
				push = push.normalize();
				victim.takeKnockback(1.0, -push.x, -push.z);
			}
		}
		if (getWorld() instanceof ServerWorld world) {
			for (int i = 0; i < 48; i++) {
				double angle = Math.PI * 2 * i / 48;
				world.spawnParticles(ParticleTypes.FLAME, getX() + Math.cos(angle) * WAVE_RADIUS, getY() + 0.3,
						getZ() + Math.sin(angle) * WAVE_RADIUS, 2, 0.1, 0.1, 0.1, 0.01);
			}
		}
	}

	@Override
	public boolean isFireImmune() {
		return true;
	}

	@Override
	public List<String> toggledBones() {
		return List.of("heat");
	}

	/** The glowing shell shows while it heats up. */
	@Override
	public boolean showsBone(String bone) {
		return isHeating();
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("Heat", heat);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		heat = Math.min(nbt.getInt("Heat"), HITS_TO_BLAST - 1);
	}

	@Override
	public Move meleeMove() {
		return SLAM;
	}

	@Override
	protected List<String> actionAnimations() {
		return List.of("attack.slam", "misc.heat", "attack.wave");
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ReachMobSounds.FURNACE_GOLEM_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ReachMobSounds.FURNACE_GOLEM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ReachMobSounds.FURNACE_GOLEM_DEATH;
	}

	@Override
	protected SoundEvent stepSound() {
		return ReachMobSounds.FURNACE_GOLEM_STEP;
	}

	@Override
	protected float stepVolume() {
		return 1.0f;
	}
}
