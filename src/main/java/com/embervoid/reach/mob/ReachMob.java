package com.embervoid.reach.mob;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.GoToWalkTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * Shared base of the Realm 1 mobs (DESIGN §4.5, PLAN 10.1): telegraphed moves, the "placed" state and the
 * GeckoLib controllers.
 *
 * <p><b>Moves.</b> Every attack is a {@link Move}: {@link #startMove} triggers its animation and sound (the
 * telegraph the player reads), and {@link #performMove} runs {@link Move#windupTicks} later, server side, unless
 * {@link #cancelMove} stops it first (stagger, death). The windup is ticked in {@link #tick}, so it also runs with
 * the AI off (GameTests drive moves directly). The cooldown starts with the move, windup included.
 *
 * <p><b>Placed.</b> Structures place tanks, elites and caged mobs at worldgen (PLAN 9.2). An entity that comes with
 * the command tag {@link #PLACED_TAG} (or is marked with {@link #markPlaced}) is persistent and guards its spot: it
 * wanders and returns within {@link #homeRadius} of it once it has no target. Subclasses react in
 * {@link #onPlaced}.
 *
 * <p><b>Animations.</b> Controller {@code movement} plays walk/idle (or {@link #movementAnimation}); controller
 * {@code state} plays the looping animation of the synced {@link #getState state} byte ({@link #stateAnimation}),
 * and pauses the movement controller while it does; controller {@link #ACTION} holds the one-shot move animations
 * listed by {@link #actionAnimations}.
 */
public abstract class ReachMob extends HostileEntity implements GeoEntity {
	/** Command tag of a mob placed by a structure. */
	public static final String PLACED_TAG = "embervoid.placed";
	/** The controller of one-shot move animations. */
	public static final String ACTION = "action";
	/** {@link #getState} value with no looping state animation. */
	public static final byte STATE_NONE = 0;

	private static final TrackedData<Byte> STATE = DataTracker.registerData(ReachMob.class, TrackedDataHandlerRegistry.BYTE);
	/** A target backing off at the last moment still has to commit: reach × this when a melee move lands. */
	private static final double LANDING_REACH_FACTOR = 1.3;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	@Nullable
	private Move move;
	private int moveTicksLeft;
	private int moveCooldown;
	private boolean placed;
	@Nullable
	private BlockPos home;

	/**
	 * A telegraphed move.
	 *
	 * @param animation     triggered on the {@link #ACTION} controller when the windup starts (it contains the windup)
	 * @param windupTicks   ticks from the start of the telegraph until the move lands
	 * @param cooldownTicks ticks from the start until the next move may start
	 * @param sound         played at the start of the windup, or null
	 * @param pitch         of that sound
	 */
	public record Move(String animation, int windupTicks, int cooldownTicks, @Nullable SoundEvent sound, float pitch) {}

	protected ReachMob(EntityType<? extends ReachMob> type, World world) {
		super(type, world);
	}

	@Override
	protected void initDataTracker() {
		super.initDataTracker();
		dataTracker.startTracking(STATE, STATE_NONE);
	}

	/**
	 * Swim, return home (placed mobs), wander, look around; revenge and players as targets. Subclasses add their
	 * attack goals in {@link #initAttackGoals}, or replace all of it (flyers).
	 */
	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(5, new GoToWalkTargetGoal(this, 1.0));
		goalSelector.add(6, new WanderAroundFarGoal(this, 0.8));
		goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8));
		goalSelector.add(8, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this));
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
		initAttackGoals();
	}

	/** Adds the attack goals; the default chases and starts {@link #meleeMove} in reach. */
	protected void initAttackGoals() {
		goalSelector.add(2, new ReachMeleeGoal(this, 1.0));
	}

	/** The plain melee move started by {@link ReachMeleeGoal}; null for a mob without one. */
	@Nullable
	public abstract Move meleeMove();

	// --- Moves ---------------------------------------------------------------------------------------------------

	/** Starts a move's telegraph if no move is running and the cooldown is over (server side). */
	public boolean startMove(Move next) {
		if (getWorld().isClient || move != null || moveCooldown > 0 || !canStartMove(next)) {
			return false;
		}
		move = next;
		moveTicksLeft = next.windupTicks();
		moveCooldown = next.cooldownTicks();
		triggerAnim(ACTION, next.animation());
		if (next.sound() != null) {
			playSound(next.sound(), 1.0f, next.pitch());
		}
		onMoveStarted(next);
		return true;
	}

	/** Whether {@code next} may start now (cooldown and running moves are already checked). */
	protected boolean canStartMove(Move next) {
		return isAlive();
	}

	/** Called when a move's telegraph starts. */
	protected void onMoveStarted(Move started) {}

	/** Called every tick of a windup but its last, with the ticks left until the move lands. */
	protected void tickWindup(Move running, int ticksLeft) {}

	/** Lands a move after its windup. The default is {@link #strike}. */
	protected void performMove(Move landed) {
		strike();
	}

	/** Stops the running move before it lands; its cooldown keeps running. */
	public void cancelMove() {
		if (move != null) {
			move = null;
			stopTriggeredAnimation(ACTION, null);
		}
	}

	/** The move whose windup is running, or null. */
	@Nullable
	public Move currentMove() {
		return move;
	}

	/** Ticks until the running move lands (0 with no move). */
	public int moveTicksLeft() {
		return move == null ? 0 : moveTicksLeft;
	}

	public boolean isWindingUp() {
		return move != null;
	}

	/** Lets the next move start right away (tests, and moves that chain). */
	public void resetMoveCooldown() {
		moveCooldown = 0;
	}

	/** Whether the mob stands still while a windup runs. Heavy mobs plant their feet. */
	protected boolean rootedDuringWindup() {
		return false;
	}

	/** A plain melee hit on the target if it is still in reach; true if it hit. */
	protected boolean strike() {
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive() || squaredDistanceTo(target) > squaredMeleeReach(target) * LANDING_REACH_FACTOR) {
			return false;
		}
		swingHand(Hand.MAIN_HAND);
		return tryAttack(target);
	}

	/** Squared melee reach, as vanilla's melee goal measures it. */
	public double squaredMeleeReach(LivingEntity target) {
		return getWidth() * 2.0f * getWidth() * 2.0f + target.getWidth();
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			return;
		}
		if (!placed && getCommandTags().contains(PLACED_TAG)) {
			markPlaced();
		}
		if (moveCooldown > 0) {
			moveCooldown--;
		}
		if (move != null) {
			if (!isAlive()) {
				move = null;
			} else if (--moveTicksLeft <= 0) {
				Move landed = move;
				move = null;
				performMove(landed);
			} else {
				if (rootedDuringWindup()) {
					getNavigation().stop();
				}
				tickWindup(move, moveTicksLeft);
			}
		}
	}

	// --- Sounds --------------------------------------------------------------------------------------------------

	/** The mob's footsteps ({@link ReachMobSounds}), or null for the block's own step sound. */
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
			playSound(step, stepVolume(), 1.0f);
		}
	}

	/** Volume of {@link #stepSound}: vanilla's small-mob 0.15, more for heavy mobs. */
	protected float stepVolume() {
		return 0.15f;
	}

	// --- Placed --------------------------------------------------------------------------------------------------

	/** Whether a structure placed this mob (it never despawns and guards its spot). */
	public boolean isPlaced() {
		return placed;
	}

	/** Marks the mob as placed at its current position (worldgen processor, commands, tests). */
	public void markPlaced() {
		boolean first = !placed;
		placed = true;
		addCommandTag(PLACED_TAG);
		setPersistent();
		home = getBlockPos();
		setPositionTarget(home, homeRadius());
		if (first) {
			onPlaced();
		}
	}

	/** The spot a placed mob guards, or null. */
	@Nullable
	public BlockPos home() {
		return home;
	}

	/** How far a placed mob wanders from its spot when it has no target. */
	protected int homeRadius() {
		return 10;
	}

	/** Called once when the mob becomes placed (not again after a reload). */
	protected void onPlaced() {}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		if (placed && home != null) {
			nbt.putBoolean("Placed", true);
			nbt.put("Home", NbtHelper.fromBlockPos(home));
		}
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.getBoolean("Placed")) {
			placed = true;
			home = NbtHelper.toBlockPos(nbt.getCompound("Home"));
			setPositionTarget(home, homeRadius());
		}
	}

	// --- Animation -----------------------------------------------------------------------------------------------

	/** Synced state byte for the looping state animation (buried, dormant, bone pile, ...). */
	public byte getState() {
		return dataTracker.get(STATE);
	}

	protected void setState(byte state) {
		dataTracker.set(STATE, state);
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);
		if (STATE.equals(data)) {
			onStateChanged();
		}
	}

	/** Called on both sides when the state byte changes (e.g. to resize the hitbox). */
	protected void onStateChanged() {}

	/** The looping animation for a state, or null for none (then walk/idle plays). */
	@Nullable
	protected RawAnimation stateAnimation(byte state) {
		return null;
	}

	/** Model bones shown only at times (glowing wings, lit eyes); the renderer asks {@link #showsBone} each frame. */
	public List<String> toggledBones() {
		return List.of();
	}

	/** Whether a bone of {@link #toggledBones} is drawn now (client side, from synced state). */
	public boolean showsBone(String bone) {
		return true;
	}

	/** The one-shot animations of this mob's moves (and other events), all on the {@link #ACTION} controller. */
	protected abstract List<String> actionAnimations();

	/** Walk while moving, idle otherwise. Flyers override this. */
	protected PlayState movementAnimation(AnimationState<ReachMob> state) {
		return state.setAndContinue(state.isMoving() ? DefaultAnimations.WALK : DefaultAnimations.IDLE);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<ReachMob>(this, "movement", 4, state ->
				stateAnimation(getState()) != null ? PlayState.STOP : movementAnimation(state)));
		controllers.add(new AnimationController<ReachMob>(this, "state", 4, state -> {
			RawAnimation animation = stateAnimation(getState());
			return animation == null ? PlayState.STOP : state.setAndContinue(animation);
		}));
		AnimationController<ReachMob> action = new AnimationController<>(this, ACTION, 0, state -> PlayState.STOP);
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
	public boolean canPickUpLoot() {
		return false;
	}

	/**
	 * Chases like a zombie; in reach, starts the mob's {@link #meleeMove} instead of hitting at once. The hit lands
	 * when the windup ends ({@link #performMove}); the cooldown is the move's.
	 */
	public static class ReachMeleeGoal extends MeleeAttackGoal {
		private final ReachMob mob;

		public ReachMeleeGoal(ReachMob mob, double speed) {
			super(mob, speed, true);
			this.mob = mob;
		}

		@Override
		protected void attack(LivingEntity target, double squaredDistance) {
			Move melee = mob.meleeMove();
			if (melee != null && squaredDistance <= getSquaredMaxAttackDistance(target)) {
				mob.startMove(melee);
			}
		}

		@Override
		protected double getSquaredMaxAttackDistance(LivingEntity target) {
			return mob.squaredMeleeReach(target);
		}
	}
}
