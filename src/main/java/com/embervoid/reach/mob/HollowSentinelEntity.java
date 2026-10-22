package com.embervoid.reach.mob;

import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.List;

/**
 * Necropolis tank (DESIGN §4.5): a spearman behind a tower shield. From the front (180°) melee hits and projectiles
 * do nothing, axes included; magic, fire and explosions still hurt. It thrusts its spear (7 damage, {@link
 * #THRUST_WINDUP}-tick draw, longer reach) and, against a target close in, plants its shield for {@link
 * #BASH_WINDUP} ticks and bashes (6 damage + strong knockback). After a bash the shield hangs open for {@link
 * #OPEN_TICKS}: that is the window to hit it from the front.
 */
public class HollowSentinelEntity extends ReachMob {
	public static final int THRUST_WINDUP = 12;
	public static final int BASH_WINDUP = 15;
	public static final int OPEN_TICKS = 30;
	public static final float BASH_DAMAGE = 6;
	public static final float THRUST_DAMAGE = 7;
	/** Bashes only a target this close. */
	public static final double BASH_RANGE = 2.5;
	/** The spear reaches this far past its own half width. */
	private static final double SPEAR_REACH = 3.0;
	static final Move THRUST = new Move("attack.thrust", THRUST_WINDUP, 40, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f);
	static final Move BASH = new Move("attack.bash", BASH_WINDUP, 50, SoundEvents.ITEM_SHIELD_BLOCK, 0.5f);
	private static final byte STATE_GUARD = 1;
	private static final byte STATE_OPEN = 2;
	private static final RawAnimation GUARD = RawAnimation.begin().thenLoop("misc.guard");
	private static final RawAnimation OPEN = RawAnimation.begin().thenLoop("misc.open");

	private int openTicks;

	public HollowSentinelEntity(EntityType<? extends HollowSentinelEntity> type, World world) {
		super(type, world);
		experiencePoints = 30;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 45)
				.add(EntityAttributes.GENERIC_ARMOR, 10)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.9)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, THRUST_DAMAGE)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24);
	}

	/** Shield open after a bash: front hits land. */
	public boolean isOpen() {
		return getState() == STATE_OPEN;
	}

	/** Bash a target close in, else thrust. */
	@Override
	public Move meleeMove() {
		LivingEntity target = getTarget();
		return target != null && distanceTo(target) - target.getWidth() / 2 <= BASH_RANGE && random.nextBoolean() ? BASH : THRUST;
	}

	/** Plants the shield for a bash at the current target. */
	public boolean startBash() {
		return getTarget() != null && startMove(BASH);
	}

	@Override
	public double squaredMeleeReach(LivingEntity target) {
		double reach = getWidth() / 2 + SPEAR_REACH + target.getWidth() / 2;
		return reach * reach;
	}

	@Override
	protected boolean rootedDuringWindup() {
		return true;
	}

	@Override
	protected void performMove(Move landed) {
		if (landed != BASH) {
			super.performMove(landed);
			return;
		}
		LivingEntity target = getTarget();
		if (target != null && target.isAlive() && distanceTo(target) - target.getWidth() / 2 <= BASH_RANGE + 0.5) {
			swingHand(Hand.OFF_HAND);
			float damage = (float) getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) * BASH_DAMAGE / THRUST_DAMAGE;
			if (target.damage(getDamageSources().mobAttack(this), damage)) {
				Vec3d push = target.getPos().subtract(getPos()).multiply(1, 0, 1);
				if (push.lengthSquared() > 1.0E-4) {
					push = push.normalize();
					target.takeKnockback(1.6, -push.x, -push.z);
					target.velocityModified = true;
				}
			}
			playSound(SoundEvents.ITEM_SHIELD_BREAK, 1.0f, 0.6f);
		}
		setState(STATE_OPEN);
		openTicks = OPEN_TICKS;
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			return;
		}
		if (isOpen()) {
			if (--openTicks <= 0) {
				setState(STATE_NONE);
			}
		} else {
			setState(getTarget() != null ? STATE_GUARD : STATE_NONE);
		}
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (!getWorld().isClient && blocks(source)) {
			playSound(SoundEvents.ITEM_SHIELD_BLOCK, 1.0f, 0.7f);
			return false;
		}
		return super.damage(source, amount);
	}

	/** Melee or a projectile from the front half, shield up. */
	public boolean blocks(DamageSource source) {
		if (isOpen() || source.isIn(DamageTypeTags.BYPASSES_SHIELD)) {
			return false;
		}
		boolean melee = source.getAttacker() != null && source.getSource() == source.getAttacker()
				&& !source.isIn(DamageTypeTags.IS_EXPLOSION) && !source.isIn(DamageTypeTags.IS_FIRE);
		if (!melee && !source.isIn(DamageTypeTags.IS_PROJECTILE)) {
			return false;
		}
		Vec3d from = source.getPosition();
		if (from == null) {
			return false;
		}
		Vec3d toSource = from.subtract(getPos()).multiply(1, 0, 1);
		return toSource.lengthSquared() < 1.0E-4 || toSource.normalize().dotProduct(Vec3d.fromPolar(0, bodyYaw)) >= 0;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("OpenTicks", isOpen() ? openTicks : 0);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.getInt("OpenTicks") > 0) {
			setState(STATE_OPEN);
			openTicks = nbt.getInt("OpenTicks");
		}
	}

	@Nullable
	@Override
	protected RawAnimation stateAnimation(byte state) {
		return state == STATE_OPEN ? OPEN : null;
	}

	@Override
	protected PlayState movementAnimation(AnimationState<ReachMob> state) {
		if (getState() == STATE_GUARD && !state.isMoving()) {
			return state.setAndContinue(GUARD);
		}
		return super.movementAnimation(state);
	}

	@Override
	protected List<String> actionAnimations() {
		return List.of("attack.thrust", "attack.bash");
	}

	@Override
	public EntityGroup getGroup() {
		return EntityGroup.UNDEAD;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_SKELETON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_WITHER_SKELETON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_WITHER_SKELETON_DEATH;
	}

	@Override
	public float getSoundPitch() {
		return super.getSoundPitch() * 0.7f;
	}
}
