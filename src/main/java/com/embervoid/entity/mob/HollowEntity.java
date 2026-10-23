package com.embervoid.entity.mob;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * Armed undead swordsman (DESIGN §3.2, bestiary in plans/track-mobs.md): slow, hard-hitting, telegraphed.
 *
 * <p>Its attack has a wind-up: the swing animation starts, and the hit lands {@link #WINDUP_TICKS} later only if
 * the target is still in reach. Between attacks it may raise its guard, which blocks most melee damage and deflects
 * projectiles from the front. An axe hit breaks the guard and staggers it. Hits from behind are never blocked.
 */
public class HollowEntity extends HostileEntity implements GeoEntity {
	private static final TrackedData<Byte> STANCE = DataTracker.registerData(HollowEntity.class, TrackedDataHandlerRegistry.BYTE);
	private static final byte STANCE_NONE = 0;
	private static final byte STANCE_GUARD = 1;
	private static final byte STANCE_STAGGERED = 2;

	private static final UUID STANCE_SLOW_ID = UUID.fromString("6c3f6f0a-3b5e-4f7e-9d8e-2f4a1c7b9e01");
	static final int WINDUP_TICKS = 8;
	private static final int ATTACK_INTERVAL_TICKS = 40;
	private static final int STAGGER_TICKS = 60;
	private static final double GUARD_RANGE = 5;
	/** cos(60°): the guard covers a 120° cone in front. */
	private static final double GUARD_CONE_COS = 0.5;
	private static final float GUARDED_MELEE_FACTOR = 0.2f;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	/** Ticks until the swing lands, or -1 when not winding up. */
	private int windup = -1;
	/**
	 * Ticks until it may start the next swing. Kept here, not in the goal: vanilla's MeleeAttackGoal always resets
	 * its own cooldown to 20 ticks and clears it whenever the goal restarts.
	 */
	private int attackCooldown;
	/** Ticks left in the current guard or stagger. */
	private int stanceTicks;

	public HollowEntity(EntityType<? extends HollowEntity> type, World world) {
		super(type, world);
		experiencePoints = 20;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 30)
				.add(EntityAttributes.GENERIC_ARMOR, 6)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2)
				// All of its damage comes from the weapon (DESIGN §3.2).
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 0)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24);
	}

	/** Natural spawns need open sky (rare surface spawns at night); structures and spawners place them anywhere. */
	public static boolean canSpawn(EntityType<? extends HostileEntity> type, ServerWorldAccess world, SpawnReason reason,
			BlockPos pos, Random random) {
		return HostileEntity.canSpawnInDark(type, world, reason, pos, random)
				&& (reason != SpawnReason.NATURAL || world.isSkyVisible(pos));
	}

	@Override
	protected void initDataTracker() {
		super.initDataTracker();
		dataTracker.startTracking(STANCE, STANCE_NONE);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(2, new HollowAttackGoal(this));
		goalSelector.add(6, new WanderAroundFarGoal(this, 0.8));
		goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8));
		goalSelector.add(8, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this));
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
		targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
	}

	@Override
	public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason reason,
			@Nullable EntityData data, @Nullable NbtCompound nbt) {
		EntityData result = super.initialize(world, difficulty, reason, data, nbt);
		initEquipment(world.getRandom(), difficulty);
		return result;
	}

	@Override
	protected void initEquipment(Random random, LocalDifficulty difficulty) {
		equipStack(EquipmentSlot.MAINHAND, new ItemStack(random.nextFloat() < 0.7f ? Items.IRON_SWORD : Items.STONE_SWORD));
	}

	public boolean isGuarding() {
		return dataTracker.get(STANCE) == STANCE_GUARD;
	}

	public boolean isStaggered() {
		return dataTracker.get(STANCE) == STANCE_STAGGERED;
	}

	private void setStance(byte stance, int ticks) {
		dataTracker.set(STANCE, stance);
		stanceTicks = ticks;
		EntityAttributeInstance speed = getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
		speed.removeModifier(STANCE_SLOW_ID);
		if (stance == STANCE_GUARD) {
			playSound(MobSounds.HOLLOW_GUARD, 0.8f, 1.0f);
		}
		if (stance != STANCE_NONE) {
			speed.addTemporaryModifier(new EntityAttributeModifier(STANCE_SLOW_ID, "Hollow stance",
					stance == STANCE_GUARD ? -0.5 : -0.6, EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
		}
	}

	/** Starts a swing if it can attack now; called by the attack goal when the target is in reach. */
	boolean startSwing() {
		if (windup >= 0 || attackCooldown > 0 || isStaggered()) {
			return false;
		}
		if (isGuarding()) {
			setStance(STANCE_NONE, 0);
		}
		windup = WINDUP_TICKS;
		attackCooldown = ATTACK_INTERVAL_TICKS;
		triggerAnim("attack", "swing");
		playSound(MobSounds.HOLLOW_SWING, 0.6f, 0.6f);
		return true;
	}

	double getSquaredReach(LivingEntity target) {
		return getWidth() * 2.0f * getWidth() * 2.0f + target.getWidth();
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		if (attackCooldown > 0) {
			attackCooldown--;
		}
		LivingEntity target = getTarget();
		if (windup >= 0 && --windup < 0 && target != null && target.isAlive()
				// A little extra reach so a target backing off at the last moment still needs to commit.
				&& squaredDistanceTo(target) <= getSquaredReach(target) * 1.3) {
			swingHand(Hand.MAIN_HAND);
			tryAttack(target);
		}

		if (stanceTicks > 0 && --stanceTicks == 0) {
			setStance(STANCE_NONE, 0);
		}
		if (dataTracker.get(STANCE) == STANCE_NONE && windup < 0 && target != null
				&& squaredDistanceTo(target) <= GUARD_RANGE * GUARD_RANGE && random.nextInt(10) == 0) {
			setStance(STANCE_GUARD, 30 + random.nextInt(31));
		}
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (!getWorld().isClient && isGuarding() && !source.isIn(DamageTypeTags.BYPASSES_SHIELD) && isInFront(source)) {
			if (source.isIn(DamageTypeTags.IS_PROJECTILE)) {
				playSound(MobSounds.HOLLOW_BLOCK, 1.0f, 0.8f);
				return false;
			}
			if (source.getAttacker() instanceof LivingEntity attacker && attacker.getMainHandStack().getItem() instanceof AxeItem) {
				playSound(MobSounds.HOLLOW_GUARD_BREAK, 1.0f, 0.8f);
				setStance(STANCE_STAGGERED, STAGGER_TICKS);
				windup = -1;
			} else {
				playSound(MobSounds.HOLLOW_BLOCK, 1.0f, 0.8f);
				amount *= GUARDED_MELEE_FACTOR;
			}
		}
		return super.damage(source, amount);
	}

	private boolean isInFront(DamageSource source) {
		Vec3d from = source.getPosition();
		if (from == null) {
			return false;
		}
		Vec3d toSource = from.subtract(getPos()).multiply(1, 0, 1);
		if (toSource.lengthSquared() < 1.0E-4) {
			return false;
		}
		Vec3d facing = Vec3d.fromPolar(0, bodyYaw);
		return toSource.normalize().dotProduct(facing) >= GUARD_CONE_COS;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putByte("Stance", dataTracker.get(STANCE));
		nbt.putInt("StanceTicks", stanceTicks);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.getInt("StanceTicks") > 0) {
			setStance(nbt.getByte("Stance"), nbt.getInt("StanceTicks"));
		}
	}

	@Override
	public EntityGroup getGroup() {
		return EntityGroup.UNDEAD;
	}

	@Override
	public boolean canPickUpLoot() {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return MobSounds.HOLLOW_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return MobSounds.HOLLOW_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return MobSounds.HOLLOW_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(MobSounds.HOLLOW_STEP, 0.3f, 1.4f);
	}

	@Override
	public float getSoundPitch() {
		return super.getSoundPitch() * 0.7f;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(DefaultAnimations.genericWalkIdleController(this));
		controllers.add(new AnimationController<>(this, "guard", 4, state ->
				isGuarding() ? state.setAndContinue(DefaultAnimations.ATTACK_BLOCK) : PlayState.STOP));
		controllers.add(new AnimationController<>(this, "attack", 0, state -> PlayState.STOP)
				.triggerableAnim("swing", DefaultAnimations.ATTACK_SWING));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animations;
	}

	/**
	 * Chases like a zombie; when in reach, starts the Hollow's wind-up swing instead of hitting. The cooldown between
	 * swings is the entity's ({@link #startSwing}).
	 */
	static class HollowAttackGoal extends MeleeAttackGoal {
		private final HollowEntity hollow;

		HollowAttackGoal(HollowEntity hollow) {
			super(hollow, 1.0, true);
			this.hollow = hollow;
		}

		@Override
		protected void attack(LivingEntity target, double squaredDistance) {
			if (squaredDistance <= getSquaredMaxAttackDistance(target)) {
				hollow.startSwing();
			}
		}

		@Override
		protected double getSquaredMaxAttackDistance(LivingEntity target) {
			return hollow.getSquaredReach(target);
		}
	}
}
