package com.embervoid.reach.mob;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Citadel elite (DESIGN §4.5): the king's guard with a greatsword. In reach it swings a 3-hit combo with short
 * windups ({@link #COMBO_1}, {@link #COMBO_2}, {@link #COMBO_3}: 6, 5, 8 ticks); every blow of the combo lands even
 * inside the target's hurt cooldown. A target {@link #LUNGE_MIN}–{@link #LUNGE_MAX} blocks away gets the lunge: a
 * {@link #CROUCH_TICKS}-tick crouch, then a dash of about 6 blocks that hits the first thing it reaches.
 */
public class SmolderingKnightEntity extends ReachMob {
	public static final int CROUCH_TICKS = 15;
	public static final double LUNGE_MIN = 4;
	public static final double LUNGE_MAX = 7;
	/** Ticks the dash lasts (horizontal speed × this ≈ 6 blocks). */
	private static final int LUNGE_TICKS = 8;
	private static final double LUNGE_SPEED = 0.85;
	private static final float LUNGE_DAMAGE_FACTOR = 1.25f;
	static final Move COMBO_1 = new Move("attack.combo1", 6, 0, ReachMobSounds.SMOLDERING_KNIGHT_SWING, 0.95f);
	static final Move COMBO_2 = new Move("attack.combo2", 5, 0, ReachMobSounds.SMOLDERING_KNIGHT_SWING, 1.05f);
	static final Move COMBO_3 = new Move("attack.combo3", 8, 40, ReachMobSounds.SMOLDERING_KNIGHT_HEAVY, 1.0f);
	static final Move CROUCH = new Move("attack.crouch", CROUCH_TICKS, 80, ReachMobSounds.SMOLDERING_KNIGHT_CROUCH, 1.0f);

	private int lungeTicks;

	public SmolderingKnightEntity(EntityType<? extends SmolderingKnightEntity> type, World world) {
		super(type, world);
		experiencePoints = 50;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 32)
				.add(EntityAttributes.GENERIC_ARMOR, 10)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24);
	}

	@Override
	public Move meleeMove() {
		return COMBO_1;
	}

	/** Starts the lunge at the current target (crouch first). */
	public boolean startLunge() {
		return getTarget() != null && startMove(CROUCH);
	}

	public boolean isLunging() {
		return lungeTicks > 0;
	}

	@Override
	protected boolean rootedDuringWindup() {
		return currentMove() == CROUCH;
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			return;
		}
		LivingEntity target = getTarget();
		if (lungeTicks > 0) {
			lungeTicks--;
			if (target != null && target.isAlive() && getBoundingBox().expand(0.5).intersects(target.getBoundingBox())) {
				lungeTicks = 0;
				swingHand(Hand.MAIN_HAND);
				float damage = (float) getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) * LUNGE_DAMAGE_FACTOR;
				target.damage(getDamageSources().mobAttack(this), damage);
			}
		} else if (target != null && target.isAlive() && !isWindingUp() && getVisibilityCache().canSee(target)) {
			double distance = distanceTo(target);
			if (distance >= LUNGE_MIN && distance <= LUNGE_MAX && random.nextInt(10) == 0) {
				startLunge();
			}
		}
	}

	@Override
	protected void performMove(Move landed) {
		if (landed == CROUCH) {
			lunge();
			return;
		}
		LivingEntity target = getTarget();
		if (target != null && landed != COMBO_1) {
			// The combo's blows land inside the target's hurt cooldown.
			target.timeUntilRegen = 0;
		}
		strike();
		if (landed == COMBO_1 || landed == COMBO_2) {
			resetMoveCooldown();
			startMove(landed == COMBO_1 ? COMBO_2 : COMBO_3);
		}
	}

	private void lunge() {
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive()) {
			return;
		}
		Vec3d toTarget = target.getPos().subtract(getPos()).multiply(1, 0, 1);
		if (toTarget.lengthSquared() < 1.0E-4) {
			return;
		}
		Vec3d dash = toTarget.normalize().multiply(LUNGE_SPEED);
		setVelocity(dash.x, 0.15, dash.z);
		velocityModified = true;
		lungeTicks = LUNGE_TICKS;
		triggerAnim(ACTION, "attack.lunge");
		playSound(ReachMobSounds.SMOLDERING_KNIGHT_LUNGE, 1.0f, 1.0f);
		if (getWorld() instanceof ServerWorld world) {
			world.spawnParticles(ParticleTypes.FLAME, getX(), getY() + 0.2, getZ(), 20, 0.3, 0.1, 0.3, 0.02);
		}
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		if (lungeTicks > 0 && !getWorld().isClient) {
			// Keep the dash going against ground friction.
			Vec3d velocity = getVelocity();
			Vec3d horizontal = velocity.multiply(1, 0, 1);
			if (horizontal.lengthSquared() > 1.0E-4 && horizontal.length() < LUNGE_SPEED) {
				horizontal = horizontal.normalize().multiply(LUNGE_SPEED);
				setVelocity(horizontal.x, velocity.y, horizontal.z);
			}
		}
	}

	@Override
	protected List<String> actionAnimations() {
		return List.of("attack.combo1", "attack.combo2", "attack.combo3", "attack.crouch", "attack.lunge");
	}

	@Override
	public boolean isFireImmune() {
		return true;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ReachMobSounds.SMOLDERING_KNIGHT_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ReachMobSounds.SMOLDERING_KNIGHT_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ReachMobSounds.SMOLDERING_KNIGHT_DEATH;
	}

	@Override
	protected SoundEvent stepSound() {
		return ReachMobSounds.SMOLDERING_KNIGHT_STEP;
	}

	@Override
	protected float stepVolume() {
		return 0.4f;
	}
}
