package com.embervoid.vampire;

import com.embervoid.boss.ArenaBoss;
import com.embervoid.boss.Telegraph;
import com.embervoid.fx.CombatFx;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The Vampire's boss form (PLAN 8.11). Its fight is {@link VampireFight}, not an arena {@code BossFight}: it implements
 * {@link ArenaBoss} for the phase thresholds and {@link Telegraph}'s animation hook, and does not call {@code
 * ArenaManager.tickBoss} (that would discard it, having no arena).
 *
 * <p>Moves (base values: claws 5, leap 7, nova 7.5, set in R40.1c so that at its lowest level, 15, no hit on a
 * player in iron goes above heavy; mob levels scale health and every hit):
 * <ul>
 *   <li>Claws: melee.</li>
 *   <li>Blood Leap: crouches for 1 s with a ring marking where it will land, then leaps there; a hit around the
 *   landing spot.</li>
 *   <li>Blood Nova (from 60 %): a shrinking ring around it for 1.5 s, then a burst that hurts everyone in the ring and
 *   heals it by a quarter of the damage done.</li>
 *   <li>Mist Step (from 30 %): dissolves into smoke and reappears behind its target with a strike. Faster.</li>
 * </ul>
 */
public class VampireBossEntity extends HostileEntity implements ArenaBoss, GeoEntity {
	public static final Identifier MUSIC = new Identifier("music.nether.crimson_forest");
	private static final DustParticleEffect BLOOD = new DustParticleEffect(new Vector3f(0.6f, 0.02f, 0.05f), 1.6f);

	/** Windups and cooldowns in ticks (the balance model reads them, PLAN 18.1); later phases are faster. */
	public static final int LEAP_WINDUP = 20;
	public static final int NOVA_WINDUP = 30;
	public static final int MIST_WINDUP = 12;
	public static final int LEAP_COOLDOWN = 140;
	public static final int LEAP_COOLDOWN_LATE = 100;
	public static final int NOVA_COOLDOWN = 160;
	public static final int NOVA_COOLDOWN_P2 = 120;
	public static final int MIST_COOLDOWN = 100;
	private static final double LEAP_RADIUS = 3.5;
	private static final float LEAP_DAMAGE = 7;
	private static final double NOVA_RADIUS = 6;
	private static final float NOVA_DAMAGE = 7.5f;
	private static final float NOVA_HEAL = 0.25f;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private int phase;
	private int leapCooldown = 60;
	private int novaCooldown = 100;
	private int mistCooldown = 80;
	/** Landing spot of a leap in the air, or null. */
	@Nullable
	private Vec3d leapTarget;
	private int leapAirTicks;

	public VampireBossEntity(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 200;
		setPersistent();
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 220)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5)
				.add(EntityAttributes.GENERIC_ARMOR, 4)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.7);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(2, new MeleeAttackGoal(this, 1.1, true));
		goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 16));
		goalSelector.add(8, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this));
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient || isRemoved()) {
			return;
		}
		if (!VampireFight.belongs(this)) {
			discard();
			return;
		}
		if (leapTarget != null) {
			tickLeap((ServerWorld) getWorld());
		}
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		if (isRemoved() || Telegraph.isWindingUp(this) || leapTarget != null) {
			return;
		}
		leapCooldown--;
		novaCooldown--;
		mistCooldown--;
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive()) {
			return;
		}
		double distance = squaredDistanceTo(target);
		if (phase >= 1 && novaCooldown <= 0 && distance < NOVA_RADIUS * NOVA_RADIUS) {
			novaCooldown = phase >= 2 ? NOVA_COOLDOWN_P2 : NOVA_COOLDOWN;
			Telegraph.windup(this, NOVA_WINDUP, "attack.nova", VampireSounds.VAMPIRE_BOSS_NOVA_WINDUP, BLOOD, NOVA_RADIUS, this::nova);
		} else if (phase >= 2 && mistCooldown <= 0 && distance > 9) {
			mistCooldown = MIST_COOLDOWN;
			Telegraph.windup(this, MIST_WINDUP, "misc.mist", VampireSounds.VAMPIRE_BOSS_MIST, ParticleTypes.SMOKE, 0, this::mistStep);
		} else if (leapCooldown <= 0 && distance > 25 && distance < 256) {
			leapCooldown = phase >= 1 ? LEAP_COOLDOWN_LATE : LEAP_COOLDOWN;
			startLeap(target.getPos());
		}
	}

	/** Blood Leap: crouches with rings marking {@code landing}, then leaps there. */
	public void startLeap(Vec3d landing) {
		Telegraph.windup(this, LEAP_WINDUP, "attack.leap", VampireSounds.VAMPIRE_BOSS_LEAP, BLOOD, 0, () -> leap(landing));
		markLanding(landing);
	}

	private void markLanding(Vec3d landing) {
		if (getWorld() instanceof ServerWorld world) {
			Telegraph.ring(world, landing, LEAP_RADIUS, BLOOD);
			Telegraph.ring(world, landing, LEAP_RADIUS * 0.5, BLOOD);
		}
	}

	private void leap(Vec3d landing) {
		Vec3d delta = landing.subtract(getPos());
		double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
		// Roughly 0.7 s in the air whatever the distance; an arc a bit higher than the target.
		setVelocity(delta.x / 14, 0.75 + Math.max(0, delta.y) * 0.08, delta.z / 14);
		velocityModified = true;
		leapTarget = landing;
		leapAirTicks = 0;
		if (horizontal > 0.01) {
			setYaw((float) (Math.atan2(delta.z, delta.x) * 180 / Math.PI) - 90);
		}
	}

	private void tickLeap(ServerWorld world) {
		leapAirTicks++;
		if (leapAirTicks % 3 == 0) {
			markLanding(leapTarget);
		}
		if ((isOnGround() && leapAirTicks > 4) || leapAirTicks > 40) {
			leapTarget = null;
			world.playSound(null, getX(), getY(), getZ(), VampireSounds.VAMPIRE_BOSS_LAND, SoundCategory.HOSTILE, 1.0f, 1.3f);
			world.spawnParticles(BLOOD, getX(), getY() + 0.3, getZ(), 40, LEAP_RADIUS / 2, 0.2, LEAP_RADIUS / 2, 0);
			CombatFx.impact(world, getPos(), 0.7f, (float) LEAP_RADIUS, 24, CombatFx.BLOOD);
			for (PlayerEntity player : playersWithin(world, LEAP_RADIUS)) {
				player.damage(getDamageSources().mobAttack(this), LEAP_DAMAGE);
				player.addVelocity(0, 0.5, 0);
				player.velocityModified = true;
			}
		}
	}

	private void nova() {
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		world.playSound(null, getX(), getY(), getZ(), VampireSounds.VAMPIRE_BOSS_NOVA, SoundCategory.HOSTILE, 1.5f, 1.4f);
		world.spawnParticles(BLOOD, getX(), getY() + 1, getZ(), 120, NOVA_RADIUS / 2, 0.8, NOVA_RADIUS / 2, 0);
		float dealt = 0;
		for (PlayerEntity player : playersWithin(world, NOVA_RADIUS)) {
			float before = player.getHealth();
			if (player.damage(getDamageSources().mobAttack(this), NOVA_DAMAGE)) {
				dealt += Math.max(0, before - player.getHealth());
				Vec3d away = player.getPos().subtract(getPos()).normalize();
				player.addVelocity(away.x * 0.8, 0.3, away.z * 0.8);
				player.velocityModified = true;
			}
		}
		if (dealt > 0) {
			heal(dealt * NOVA_HEAL);
			world.spawnParticles(ParticleTypes.HEART, getX(), getY() + 2.2, getZ(), 3, 0.4, 0.2, 0.4, 0);
		}
	}

	private void mistStep() {
		LivingEntity target = getTarget();
		if (!(getWorld() instanceof ServerWorld world) || target == null || !target.isAlive()) {
			return;
		}
		world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 30, 0.4, 0.8, 0.4, 0.02);
		Vec3d behind = target.getPos().subtract(target.getRotationVector().multiply(1, 0, 1).normalize().multiply(2));
		if (teleport(behind.x, target.getY(), behind.z, false)) {
			world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 30, 0.4, 0.8, 0.4, 0.02);
			world.playSound(null, getX(), getY(), getZ(), VampireSounds.VAMPIRE_BOSS_MIST_STEP, SoundCategory.HOSTILE, 1.5f, 0.8f);
			lookAtEntity(target, 360, 360);
			tryAttack(target);
		}
	}

	private Iterable<PlayerEntity> playersWithin(ServerWorld world, double radius) {
		return world.getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(radius, 2, radius),
				p -> p.squaredDistanceTo(this) <= radius * radius && !p.isSpectator() && !p.isCreative());
	}

	/** Called by {@link VampireFight} each tick: the next phase starts at its health threshold. */
	void updatePhase() {
		float[] thresholds = phaseThresholds();
		float fraction = getHealth() / getMaxHealth();
		while (phase < thresholds.length && fraction <= thresholds[phase]) {
			phase++;
			onPhaseChange(phase);
		}
	}

	@Override
	public float[] phaseThresholds() {
		return new float[]{0.6f, 0.3f};
	}

	@Override
	public void onPhaseChange(int phase) {
		getWorld().playSound(null, getX(), getY(), getZ(), VampireSounds.VAMPIRE_BOSS_PHASE, SoundCategory.HOSTILE, 2.0f, 1.4f);
		if (getWorld() instanceof ServerWorld world) {
			world.spawnParticles(BLOOD, getX(), getY() + 1, getZ(), 50, 0.5, 1.0, 0.5, 0);
		}
		if (phase == 2) {
			getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.36);
		}
	}

	@Override
	public void playAnimation(String name) {
		triggerAnim("moves", name);
	}

	@Override
	public Identifier music() {
		return MUSIC;
	}

	/** Leaves in smoke (nobody beat it, or the night ended). */
	void vanish() {
		if (getWorld() instanceof ServerWorld world) {
			world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 40, 0.5, 1.0, 0.5, 0.03);
		}
		discard();
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		// Fall damage would punish its own leaps.
		return !source.isOf(DamageTypes.FALL) && super.damage(source, amount);
	}

	@Override
	public boolean isFireImmune() {
		return true;
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	@Override
	public boolean cannotDespawn() {
		return true;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("Phase", phase);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		phase = nbt.getInt("Phase");
		if (phase >= 2) {
			getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.36);
		}
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(DefaultAnimations.genericWalkIdleController(this));
		controllers.add(DefaultAnimations.genericAttackAnimation(this, DefaultAnimations.ATTACK_SWING));
		controllers.add(new AnimationController<>(this, "moves", 0, state -> PlayState.STOP)
				.triggerableAnim("attack.leap", RawAnimation.begin().thenPlay("attack.leap"))
				.triggerableAnim("attack.nova", RawAnimation.begin().thenPlay("attack.nova"))
				.triggerableAnim("misc.mist", RawAnimation.begin().thenPlay("misc.mist")));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animations;
	}
}
