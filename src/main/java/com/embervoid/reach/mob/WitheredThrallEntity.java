package com.embervoid.reach.mob;

import net.minecraft.block.BlockState;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.List;

/**
 * Woods fodder (DESIGN §4.5): a hanged poacher. Natural ones walk and claw (10-tick windup). Placed ones with the
 * extra tag {@link #CAGE_TAG} hang in a hanging cage (no gravity, no AI) until a player passes under them: the
 * chain rattles for {@link #RATTLE_TICKS}, then the thrall drops (opening a trapdoor under it, if the cage has one),
 * hits a player it lands on, and fights on. Shot or hit while it hangs, it drops at once.
 */
public class WitheredThrallEntity extends ReachMob {
	/** Tag a structure adds (with {@link ReachMob#PLACED_TAG}) to a thrall in a hanging cage. */
	public static final String CAGE_TAG = "embervoid.placed.cage";
	public static final int RATTLE_TICKS = 10;
	/** A player this close horizontally, anywhere below within {@link #DROP_HEIGHT}, is "under" the cage. */
	public static final double UNDER_RADIUS = 1.5;
	public static final double DROP_HEIGHT = 16;
	static final Move MELEE = new Move("attack.swing", 10, 30, SoundEvents.ENTITY_ZOMBIE_VILLAGER_AMBIENT, 0.7f);
	private static final byte STATE_HANGING = 1;
	private static final byte STATE_RATTLING = 2;
	private static final byte STATE_FALLING = 3;
	private static final RawAnimation HANG = RawAnimation.begin().thenLoop("misc.hang");

	private int rattleTicks;

	public WitheredThrallEntity(EntityType<? extends WitheredThrallEntity> type, World world) {
		super(type, world);
		experiencePoints = 8;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 18)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.5)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.24)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32);
	}

	@Override
	protected void onPlaced() {
		if (getCommandTags().contains(CAGE_TAG)) {
			hang();
		}
	}

	/** Hangs where it is, waiting for a player to pass under. */
	public void hang() {
		setState(STATE_HANGING);
		setNoGravity(true);
		setVelocity(0, 0, 0);
		setTarget(null);
		rattleTicks = 0;
	}

	public boolean isHanging() {
		return getState() == STATE_HANGING || getState() == STATE_RATTLING;
	}

	public boolean isRattling() {
		return getState() == STATE_RATTLING;
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			return;
		}
		byte state = getState();
		if (state == STATE_HANGING) {
			setVelocity(0, 0, 0);
			PlayerEntity below = playerBelow();
			if (below != null) {
				setState(STATE_RATTLING);
				rattleTicks = RATTLE_TICKS;
				setTarget(below);
			}
		} else if (state == STATE_RATTLING) {
			setVelocity(0, 0, 0);
			if (rattleTicks % 3 == 0) {
				playSound(SoundEvents.BLOCK_CHAIN_HIT, 1.0f, 0.6f + random.nextFloat() * 0.3f);
			}
			if (--rattleTicks <= 0) {
				drop();
			}
		} else if (state == STATE_FALLING && isOnGround()) {
			setState(STATE_NONE);
			if (getTarget() != null && getBoundingBox().expand(0.6, 0.5, 0.6).intersects(getTarget().getBoundingBox())) {
				swingHand(Hand.MAIN_HAND);
				tryAttack(getTarget());
			}
		}
	}

	@Nullable
	private PlayerEntity playerBelow() {
		Box under = new Box(getX() - UNDER_RADIUS, getY() - DROP_HEIGHT, getZ() - UNDER_RADIUS,
				getX() + UNDER_RADIUS, getY(), getZ() + UNDER_RADIUS);
		List<PlayerEntity> players = getWorld().getEntitiesByClass(PlayerEntity.class, under,
				player -> player.isAlive() && !player.isSpectator() && !player.isCreative());
		return players.isEmpty() ? null : players.get(0);
	}

	private void drop() {
		setState(STATE_FALLING);
		setNoGravity(false);
		BlockPos below = getBlockPos().down();
		BlockState floor = getWorld().getBlockState(below);
		if (floor.getBlock() instanceof TrapdoorBlock && !floor.get(TrapdoorBlock.OPEN)) {
			getWorld().setBlockState(below, floor.with(TrapdoorBlock.OPEN, true));
			playSound(SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN, 1.0f, 0.8f);
		}
		triggerAnim(ACTION, "attack.drop");
		playSound(SoundEvents.ENTITY_ZOMBIE_VILLAGER_AMBIENT, 1.0f, 0.6f);
	}

	/** Shot or hit in its cage, it drops at once. */
	@Override
	public boolean damage(DamageSource source, float amount) {
		boolean hurt = super.damage(source, amount);
		if (hurt && isHanging() && isAlive() && !getWorld().isClient) {
			if (source.getAttacker() instanceof LivingEntity attacker) {
				setTarget(attacker);
			}
			drop();
		}
		return hurt;
	}

	@Override
	protected void mobTick() {
		if (isHanging() || getState() == STATE_FALLING) {
			return;
		}
		super.mobTick();
	}

	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
		// A caged thrall's drop is its attack, not a fall.
		return getState() != STATE_FALLING && super.handleFallDamage(fallDistance, damageMultiplier, source);
	}

	@Override
	public boolean isPushable() {
		return !isHanging() && super.isPushable();
	}

	@Override
	protected void pushAway(Entity entity) {
		if (!isHanging()) {
			super.pushAway(entity);
		}
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("Hanging", isHanging());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.getBoolean("Hanging")) {
			hang();
		}
	}

	@Nullable
	@Override
	protected RawAnimation stateAnimation(byte state) {
		return state == STATE_HANGING || state == STATE_RATTLING ? HANG : null;
	}

	@Override
	public Move meleeMove() {
		return MELEE;
	}

	@Override
	protected List<String> actionAnimations() {
		return List.of("attack.swing", "attack.drop");
	}

	@Override
	public EntityGroup getGroup() {
		return EntityGroup.UNDEAD;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return isHanging() ? null : SoundEvents.ENTITY_ZOMBIE_VILLAGER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_ZOMBIE_VILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_ZOMBIE_VILLAGER_DEATH;
	}

	@Override
	public float getSoundPitch() {
		return super.getSoundPitch() * 0.8f;
	}
}
