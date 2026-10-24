package com.embervoid.reach.mob;

import com.embervoid.fx.CombatFx;
import com.embervoid.fx.SwingArc;
import com.embervoid.fx.particle.EmberParticles;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
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
	/** Ticks the body stays for {@code misc.death}: down on one knee over the planted sword, then onto its face, 2.2 s. */
	public static final int DEATH_TICKS = 44;
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
				if (getWorld() instanceof ServerWorld world) {
					// The point goes in: a hot burst and sparks where it lands.
					Vec3d hit = target.getPos().add(0, target.getHeight() * 0.5, 0);
					EmberParticles.burst(world, hit, 1.2f, CombatFx.EMBER);
					EmberParticles.spawn(world, EmberParticles.CINDER_SPARK, hit, 10, 0.2, 0.2);
				}
			} else if (getWorld() instanceof ServerWorld world) {
				// Embers stream off the blade and the heels along the dash.
				EmberParticles.spawn(world, EmberParticles.EMBER, getPos().add(0, 0.2, 0), 3, 0.25, 0.02);
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
		swingFx(landed);
		if (landed == COMBO_1 || landed == COMBO_2) {
			resetMoveCooldown();
			startMove(landed == COMBO_1 ? COMBO_2 : COMBO_3);
		}
	}

	/** The blade's arc for each blow of the combo; the finisher also strikes the ground in front. */
	private void swingFx(Move landed) {
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		if (landed == COMBO_1) {
			CombatFx.trail(this, SwingArc.sweep(2.6f, 1.5f, false, 4, CombatFx.EMBER));
		} else if (landed == COMBO_2) {
			CombatFx.trail(this, SwingArc.sweep(2.6f, 1.3f, true, 4, CombatFx.EMBER));
		} else if (landed == COMBO_3) {
			CombatFx.trail(this, SwingArc.overhead(2.8f, 2.0f, 5, CombatFx.EMBER));
			Vec3d front = getPos().add(Vec3d.fromPolar(0, getYaw()).multiply(1.6));
			CombatFx.impact(world, front, 0.4f, 0.9f, 10, CombatFx.EMBER);
			EmberParticles.scorch(world, front, 1.2f);
			EmberParticles.ringOut(world, EmberParticles.CINDER_SPARK, front, 10, 0.2, 0.25);
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
			// Kicking off: soot and embers from the heels.
			EmberParticles.spawn(world, EmberParticles.SOOT, getPos().add(0, 0.2, 0), 8, 0.4, 0.03);
			EmberParticles.ringOut(world, EmberParticles.EMBER, getPos(), 12, 0.15, 0.1);
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
	public int deathTicks() {
		return DEATH_TICKS;
	}

	/** On the knee the seams go out one by one (embers, then soot); the fall onto its face shakes the ground. */
	@Override
	protected void tickDeath(ServerWorld world, int deathTime) {
		if (deathTime == 9) {
			EmberParticles.spawn(world, EmberParticles.SOOT, getPos().add(0, 0.2, 0), 6, 0.5, 0.03);
		} else if (deathTime > 9 && deathTime < 28 && deathTime % 3 == 0) {
			EmberParticles.spawn(world, EmberParticles.EMBER, getPos().add(0, 1.0, 0), 2, 0.3, 0.03);
		} else if (deathTime == 30) {
			Vec3d front = getPos().add(Vec3d.fromPolar(0, getYaw()).multiply(1.0));
			CombatFx.shake(world, front, 0.25f, CombatFx.shakeTicks(0.25f), 8);
			EmberParticles.spawn(world, EmberParticles.SOOT, front.add(0, 0.2, 0), 10, 0.9, 0.04);
			EmberParticles.ringOut(world, EmberParticles.ASH_FLAKE, front, 10, 0.1, 0.1);
		} else if (deathTime > 30 && deathTime % 5 == 0) {
			EmberParticles.spawn(world, EmberParticles.SOOT, getPos().add(0, 0.3, 0), 1, 0.5, 0.02);
		}
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
