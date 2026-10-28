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
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.world.ServerWorld;
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
 * Shared base of the Realm 1 mobs (DESIGN §4.5): telegraphed moves, the "placed" state and the
 * GeckoLib controllers.
 *
 * <p><b>Moves.</b> Every attack is a {@link Move}: {@link #startMove} triggers its animation and sound (the
 * telegraph the player reads), and {@link #performMove} runs {@link Move#windupTicks} later, server side, unless
 * {@link #cancelMove} stops it first (stagger, death). The windup is ticked in {@link #tick}, so it also runs with
 * the AI off (GameTests drive moves directly). The cooldown starts with the move, windup included.
 *
 * <p><b>Placed.</b> Structures place tanks, elites and caged mobs at worldgen. An entity that comes with
 * the command tag {@link #PLACED_TAG} (or is marked with {@link #markPlaced}) is persistent and guards its spot: it
 * wanders and returns within {@link #homeRadius} of it once it has no target. Subclasses react in
 * {@link #onPlaced}.
 *
 * <p><b>Animations.</b> Controller {@code movement} plays walk/idle (or {@link #movementAnimation}); controller
 * {@code state} plays the looping animation of the synced {@link #getState state} byte ({@link #stateAnimation}),
 * and pauses the movement controller while it does; controller {@link #HURT_CONTROLLER} holds the hit reactions;
 * controller {@link #ACTION} holds the one-shot move animations listed by {@link #actionAnimations}. Later
 * controllers win on the bones they animate, so a move's animation beats a flinch.
 *
 * <p><b>Hurt, death and stagger</b> (the same names and approach as {@code ReachBoss}): a damaging hit
 * outside a windup plays {@link #HURT} (or {@link #STAGGER} where {@link #hurtAnimation} says so), never over a windup
 * (the tell stays readable); {@link #DEATH} plays on death and the body stays {@link #deathTicks} ticks (the length
 * of its fall) before it goes, with no vanilla death tilt (the renderer turns it off).
 */
public abstract class ReachMob extends HostileEntity implements GeoEntity {
	/** Command tag of a mob placed by a structure. */
	public static final String PLACED_TAG = "embervoid.placed";
	/** The controller of one-shot move animations. */
	public static final String ACTION = "action";
	/** The controller of the hit reactions, under the move animations. */
	public static final String HURT_CONTROLLER = "hurt";
	/** The flinch on a hit between moves, a heavier stagger, the fall on death. */
	public static final String HURT = "misc.hurt";
	public static final String STAGGER = "misc.stagger";
	public static final String DEATH = "misc.death";
	/** Ticks between two flinches. */
	public static final int HURT_COOLDOWN = 10;
	/** A hit of this share of the max health staggers a mob with no stagger state of its own. */
	public static final float HEAVY_HIT_SHARE = 0.35f;
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
	private int lastHurtAnimation = -100;
	private int hurtFlinches;

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

	@Override
	public boolean damage(DamageSource source, float amount) {
		boolean hurt = super.damage(source, amount);
		// A flinch between moves; never over a windup (the tell must stay readable).
		if (hurt && amount > 0 && isAlive() && !getWorld().isClient && !isWindingUp() && age - lastHurtAnimation > HURT_COOLDOWN) {
			lastHurtAnimation = age;
			hurtFlinches++;
			triggerAnim(HURT_CONTROLLER, hurtAnimation(source, amount));
		}
		return hurt;
	}

	/**
	 * The hit reaction to a damaging hit outside a windup: {@link #HURT}, or {@link #STAGGER} for a heavy hit
	 * ({@link #HEAVY_HIT_SHARE} of the max health); mobs with a stagger state of their own (the Sentinel's open
	 * shield, the Thrall knocked off its cage, the Bones kicked as a pile) decide there. Visual only: it never
	 * changes what the hit does.
	 */
	protected String hurtAnimation(DamageSource source, float amount) {
		return amount >= getMaxHealth() * HEAVY_HIT_SHARE ? STAGGER : HURT;
	}

	/** The hit reactions of this mob, on the {@link #HURT_CONTROLLER}. */
	protected List<String> hurtAnimations() {
		return List.of(HURT, STAGGER);
	}

	/** How many flinches and staggers the mob has shown (tests: a hit during a windup shows none). */
	public int hurtFlinches() {
		return hurtFlinches;
	}

	/**
	 * How long the body stays after death for its death animation ({@link #DEATH}), in ticks: the animation's length.
	 * 0: vanilla's death. The body removes itself in {@link #updatePostDeath} once they are over.
	 */
	public int deathTicks() {
		return 40;
	}

	/** Particles and sounds while the body lies there, {@code deathTime} ticks after the death (server side). */
	protected void tickDeath(ServerWorld world, int deathTime) {
	}

	@Override
	public void onDeath(DamageSource source) {
		super.onDeath(source);
		if (getWorld().isClient) {
			return;
		}
		cancelMove();
		if (deathTicks() > 0) {
			triggerAnim(ACTION, DEATH);
		}
	}

	@Override
	protected void updatePostDeath() {
		if (deathTicks() <= 0) {
			super.updatePostDeath();
			return;
		}
		deathTime++;
		if (!(getWorld() instanceof ServerWorld world) || isRemoved()) {
			return;
		}
		tickDeath(world, deathTime);
		if (deathTime >= deathTicks()) {
			world.sendEntityStatus(this, (byte) 60);
			remove(RemovalReason.KILLED);
		}
	}

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

	/** Plays a one-shot animation of {@link #actionAnimations} (server side: GeckoLib syncs it). */
	public void playAnimation(String name) {
		triggerAnim(ACTION, name);
	}

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
		AnimationController<ReachMob> hurt = new AnimationController<>(this, HURT_CONTROLLER, 0, state -> PlayState.STOP);
		for (String name : hurtAnimations()) {
			hurt.triggerableAnim(name, RawAnimation.begin().thenPlay(name));
		}
		controllers.add(hurt);
		AnimationController<ReachMob> action = new AnimationController<>(this, ACTION, 0, state -> PlayState.STOP);
		for (String name : actionAnimations()) {
			action.triggerableAnim(name, RawAnimation.begin().thenPlay(name));
		}
		// The fall is held on its last frame while the body lies there.
		action.triggerableAnim(DEATH, RawAnimation.begin().thenPlayAndHold(DEATH));
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
