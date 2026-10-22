package com.embervoid.reach.boss;

import com.embervoid.boss.ArenaBoss;
import com.embervoid.boss.ArenaManager;
import com.embervoid.boss.BossFight;
import com.embervoid.boss.Telegraph;
import com.embervoid.boss.TelegraphShape;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * Shared base of the Ashen Reach bosses (DESIGN §4.5, PLAN 12.3–12.5, 13.4): an {@link ArenaBoss} run by its
 * {@link BossFight}, GeckoLib animations, a synced phase for the client, and a simple brain — walk to the target,
 * pick a move when the cooldown is over, stand still while a move winds up ({@link Telegraph}). Moves hit the players
 * standing in the {@link TelegraphShape} they marked.
 *
 * <p>Controllers: {@code movement} (walk/idle) and {@link #ACTION} (one-shot move animations from
 * {@link #actionAnimations}; {@link #playAnimation} triggers them, as {@link Telegraph} does at a windup's start).
 */
public abstract class ReachBoss extends HostileEntity implements ArenaBoss, GeoEntity {
	/** The controller of one-shot move animations. */
	public static final String ACTION = "action";
	/** Placeholder for the shared zone-boss theme (real music is a release task, R40.3). */
	public static final Identifier ZONE_BOSS_MUSIC = new Identifier("music.nether.basalt_deltas");

	private static final TrackedData<Byte> PHASE = DataTracker.registerData(ReachBoss.class, TrackedDataHandlerRegistry.BYTE);

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	/** Ticks until the brain may pick the next move. */
	protected int moveCooldown = 40;

	protected ReachBoss(EntityType<? extends ReachBoss> type, World world) {
		super(type, world);
		setPersistent();
	}

	@Override
	protected void initDataTracker() {
		super.initDataTracker();
		dataTracker.startTracking(PHASE, (byte) 0);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 24));
		goalSelector.add(8, new LookAroundGoal(this));
		targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
	}

	/** The phase the fight is in (synced to clients for looks). */
	public int phase() {
		return dataTracker.get(PHASE);
	}

	@Override
	public void onPhaseChange(int phase) {
		dataTracker.set(PHASE, (byte) phase);
	}

	/** This boss's running fight, or null (client, a leftover after a restart). */
	@Nullable
	public BossFight fight() {
		return BossFight.of(this);
	}

	@Override
	public void tick() {
		super.tick();
		if (!getWorld().isClient && !isRemoved()) {
			ArenaManager.tickBoss(this);
			if (!isRemoved()) {
				tickBoss();
			}
		}
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		if (isRemoved()) {
			return;
		}
		if (Telegraph.isWindingUp(this) || isBusy()) {
			getNavigation().stop();
			return;
		}
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive()) {
			return;
		}
		getLookControl().lookAt(target, 30, 30);
		double distance = squaredDistanceTo(target);
		if (moveCooldown > 0) {
			moveCooldown--;
		} else {
			int cooldown = chooseMove(target, distance);
			if (cooldown >= 0) {
				moveCooldown = cooldown;
				getNavigation().stop();
				return;
			}
		}
		if (distance > keepDistance() * keepDistance()) {
			getNavigation().startMovingTo(target, 1.0);
		} else {
			getNavigation().stop();
		}
	}

	/** Keeps the brain from picking a move for {@code ticks} ticks (tests drive moves themselves). */
	public void holdMoves(int ticks) {
		moveCooldown = ticks;
	}

	/** Every server tick, AI or not (phase effects, hazards); moves are picked in the AI tick after it. */
	protected void tickBoss() {
	}

	/** True while something other than a windup keeps the boss in place (a leap in the air, a stun). */
	protected boolean isBusy() {
		return false;
	}

	/** How close the boss walks up to its target. */
	protected double keepDistance() {
		return 3;
	}

	/**
	 * Picks and starts a move against {@code target} ({@code squaredDistance} away) when the cooldown is over.
	 * Returns the cooldown until the next pick, or −1 if no move fits (the boss keeps walking).
	 */
	protected abstract int chooseMove(LivingEntity target, double squaredDistance);

	// --- Move helpers ---

	/** Turns the boss to face a point at once (moves aim where it faces at the start of their windup). */
	protected void face(Vec3d point) {
		double dx = point.x - getX();
		double dz = point.z - getZ();
		float yaw = (float) (MathHelper.atan2(dz, dx) * 180 / Math.PI) - 90;
		setYaw(yaw);
		setHeadYaw(yaw);
		setBodyYaw(yaw);
	}

	/** Horizontal direction from the boss to a point (or its facing if the point is on top of it). */
	protected Vec3d directionTo(Vec3d point) {
		Vec3d d = new Vec3d(point.x - getX(), 0, point.z - getZ());
		return d.lengthSquared() < 1e-4 ? getRotationVector().multiply(1, 0, 1).normalize() : d.normalize();
	}

	/** A windup marking {@code shape}; when it ends, {@code hit} runs for every player still inside. */
	protected void telegraph(TelegraphShape shape, int ticks, @Nullable String animation, @Nullable SoundEvent sound, ParticleEffect particle,
			HitAction hit) {
		Telegraph.windup(this, shape, ticks, animation, sound, particle, () -> {
			for (PlayerEntity player : Telegraph.playersIn(this, shape)) {
				hit.hit(player);
			}
			if (animation != null) {
				afterStrike(animation);
			}
		});
	}

	/** Called after a telegraphed move lands (chained moves start the next part here). */
	protected void afterStrike(String animation) {
	}

	/** Pushes a player away from the boss. */
	protected void knockAway(PlayerEntity player, double strength, double up) {
		Vec3d away = directionTo(player.getPos());
		player.addVelocity(away.x * strength, up, away.z * strength);
		player.velocityModified = true;
	}

	/** What a move does to one player standing in its shape. */
	@FunctionalInterface
	protected interface HitAction {
		void hit(PlayerEntity player);
	}

	// --- Sounds ---

	/** The boss's footsteps ({@link ReachBossSounds}), or null for the block's own step sound. */
	@Nullable
	protected SoundEvent stepSound() {
		return null;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		SoundEvent step = stepSound();
		if (step == null) {
			super.playStepSound(pos, state);
		} else {
			playSound(step, 1.0f, 1.0f);
		}
	}

	/** Giants: their ambient, hurt and death sounds carry across the arena. */
	@Override
	protected float getSoundVolume() {
		return 2.0f;
	}

	// --- Entity ---

	@Override
	public boolean damage(DamageSource source, float amount) {
		// Bosses are heavy and walk their arena: no falls, no drowning, no being crushed in a wall.
		if (source.isOf(DamageTypes.FALL) || source.isOf(DamageTypes.DROWN) || source.isOf(DamageTypes.IN_WALL)) {
			return false;
		}
		return super.damage(source, amount);
	}

	@Override
	public boolean isPushable() {
		return false;
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
	public boolean canPickUpLoot() {
		return false;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putByte("Phase", dataTracker.get(PHASE));
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		dataTracker.set(PHASE, nbt.getByte("Phase"));
	}

	// --- Animation ---

	/** Model bones shown only at times (a lantern, an exposed core); the renderer asks {@link #showsBone} each frame. */
	public List<String> toggledBones() {
		return List.of();
	}

	/** Whether a bone of {@link #toggledBones} is drawn now (client side, from synced state). */
	public boolean showsBone(String bone) {
		return true;
	}

	/** Every one-shot animation of this boss (moves, phase changes), on the {@link #ACTION} controller. */
	protected abstract List<String> actionAnimations();

	@Override
	public void playAnimation(String name) {
		triggerAnim(ACTION, name);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(DefaultAnimations.genericWalkIdleController(this));
		AnimationController<ReachBoss> action = new AnimationController<>(this, ACTION, 0, state -> PlayState.STOP);
		for (String name : actionAnimations()) {
			action.triggerableAnim(name, RawAnimation.begin().thenPlay(name));
		}
		controllers.add(action);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animations;
	}

	@Override
	public Identifier music() {
		return ZONE_BOSS_MUSIC;
	}
}
