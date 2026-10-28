package com.embervoid.reach.mob;

import com.embervoid.fx.CombatFx;
import com.embervoid.fx.particle.EmberParticles;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
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
	public static final int OPEN_TICKS = SentinelShield.OPEN_TICKS;
	public static final float BASH_DAMAGE = 6;
	public static final float THRUST_DAMAGE = 7;
	/** Bashes only a target this close. */
	public static final double BASH_RANGE = 2.5;
	/** Ticks the body stays for {@code misc.death}: to a knee on the spear, then over onto its shield side, 2.2 s. */
	public static final int DEATH_TICKS = 44;
	/** The spear reaches this far past its own half width. */
	private static final double SPEAR_REACH = 3.0;
	static final Move THRUST = new Move("attack.thrust", THRUST_WINDUP, 40, ReachMobSounds.HOLLOW_SENTINEL_DRAW_SPEAR, 1.0f);
	static final Move BASH = new Move("attack.bash", BASH_WINDUP, 50, ReachMobSounds.HOLLOW_SENTINEL_PLANT_SHIELD, 1.0f);
	private static final byte STATE_GUARD = 1;
	private static final byte STATE_OPEN = 2;
	private static final RawAnimation GUARD = RawAnimation.begin().thenLoop("misc.guard");
	private static final RawAnimation OPEN = RawAnimation.begin().thenLoop("misc.open");

	private int openTicks;

	public HollowSentinelEntity(EntityType<? extends HollowSentinelEntity> type, World world) {
		super(type, world);
		experiencePoints = 22;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 28)
				.add(EntityAttributes.GENERIC_ARMOR, 5)
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
			playSound(ReachMobSounds.HOLLOW_SENTINEL_BASH, 1.0f, 1.0f);
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
		SentinelShield.Stance stance = isOpen() ? SentinelShield.Stance.OPEN
				: getState() == STATE_GUARD ? SentinelShield.Stance.GUARD : SentinelShield.Stance.LOWERED;
		if (stance == SentinelShield.Stance.OPEN) {
			openTicks--;
		}
		SentinelShield.Stance next = SentinelShield.next(stance, openTicks, getTarget() != null);
		setState(switch (next) {
			case OPEN -> STATE_OPEN;
			case GUARD -> STATE_GUARD;
			case LOWERED -> STATE_NONE;
		});
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (!getWorld().isClient && blocks(source)) {
			playSound(ReachMobSounds.HOLLOW_SENTINEL_BLOCK, 1.0f, 1.0f);
			return false;
		}
		return super.damage(source, amount);
	}

	/** Melee or a projectile from the front half, shield up. */
	public boolean blocks(DamageSource source) {
		boolean melee = source.getAttacker() != null && source.getSource() == source.getAttacker()
				&& !source.isIn(DamageTypeTags.IS_EXPLOSION) && !source.isIn(DamageTypeTags.IS_FIRE);
		Vec3d from = source.getPosition();
		if (from == null) {
			return false;
		}
		Vec3d facing = Vec3d.fromPolar(0, bodyYaw);
		return SentinelShield.blocks(isOpen(), source.isIn(DamageTypeTags.BYPASSES_SHIELD),
				melee || source.isIn(DamageTypeTags.IS_PROJECTILE), from.x - getX(), from.z - getZ(), facing.x, facing.z);
	}

	/** Hit while the shield hangs open (the bash window), it staggers; a hit it took elsewhere only makes it flinch. */
	@Override
	protected String hurtAnimation(DamageSource source, float amount) {
		return isOpen() ? STAGGER : HURT;
	}

	@Override
	public int deathTicks() {
		return DEATH_TICKS;
	}

	/** The plate hits the stone when it goes over: a clang of dust. */
	@Override
	protected void tickDeath(ServerWorld world, int deathTime) {
		if (deathTime == 30) {
			EmberParticles.spawn(world, EmberParticles.SOOT, getPos().add(0, 0.3, 0), 8, 0.8, 0.03);
			EmberParticles.ringOut(world, EmberParticles.ASH_FLAKE, getPos(), 10, 0.1, 0.1);
			CombatFx.shake(world, getPos(), 0.25f, CombatFx.shakeTicks(0.25f), 8);
		}
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
		return ReachMobSounds.HOLLOW_SENTINEL_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ReachMobSounds.HOLLOW_SENTINEL_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ReachMobSounds.HOLLOW_SENTINEL_DEATH;
	}

	@Override
	protected SoundEvent stepSound() {
		return ReachMobSounds.HOLLOW_SENTINEL_STEP;
	}

	@Override
	protected float stepVolume() {
		return 0.4f;
	}
}
