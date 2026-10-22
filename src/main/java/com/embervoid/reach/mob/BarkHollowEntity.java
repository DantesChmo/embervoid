package com.embervoid.reach.mob;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.List;

/**
 * Woods tank (DESIGN §4.5). It stands dormant like a dead tree (no AI, unlit eyes) until a player comes within
 * {@link #WAKE_RANGE} or it is hit; then it creaks awake over {@link #AWAKEN_TICKS} and fights. Its attack is a
 * 3-block sweep in front of it after a {@link #SWEEP_WINDUP}-tick windup with its feet planted. Axes (in a melee
 * hit) and fire hurt it ×{@link #WEAK_FACTOR}. With nothing to fight for {@link #SETTLE_TICKS} it goes dormant again.
 * Every Bark Hollow starts dormant (they are placed in the park, glades and around the lodge).
 */
public class BarkHollowEntity extends ReachMob {
	public static final double WAKE_RANGE = 6;
	public static final int AWAKEN_TICKS = 20;
	public static final int SWEEP_WINDUP = 20;
	public static final double SWEEP_RANGE = 3;
	/** cos(60°): the sweep covers 120° in front. */
	private static final double SWEEP_CONE_COS = 0.5;
	public static final float WEAK_FACTOR = 1.5f;
	static final int SETTLE_TICKS = 300;
	static final Move SWEEP = new Move("attack.sweep", SWEEP_WINDUP, 50, SoundEvents.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.5f);
	private static final byte STATE_DORMANT = 1;
	private static final byte STATE_AWAKENING = 2;
	private static final RawAnimation DORMANT = RawAnimation.begin().thenLoop("misc.dormant");

	private int awakenTicks;
	private int idleTicks;

	public BarkHollowEntity(EntityType<? extends BarkHollowEntity> type, World world) {
		super(type, world);
		experiencePoints = 30;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 50)
				.add(EntityAttributes.GENERIC_ARMOR, 8)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.18)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24);
	}

	@Override
	public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason reason,
			@Nullable EntityData data, @Nullable NbtCompound nbt) {
		EntityData result = super.initialize(world, difficulty, reason, data, nbt);
		goDormant();
		return result;
	}

	@Override
	protected void onPlaced() {
		goDormant();
	}

	/** Stands still as a dead tree until woken. */
	public void goDormant() {
		cancelMove();
		setTarget(null);
		getNavigation().stop();
		setState(STATE_DORMANT);
		idleTicks = 0;
	}

	public boolean isDormant() {
		return getState() == STATE_DORMANT;
	}

	public boolean isAwakening() {
		return getState() == STATE_AWAKENING;
	}

	/** Creaks awake, turning on {@code waker} (if any). */
	public void wake(@Nullable LivingEntity waker) {
		if (!isDormant()) {
			return;
		}
		setState(STATE_AWAKENING);
		awakenTicks = AWAKEN_TICKS;
		if (waker != null) {
			setTarget(waker);
		}
		triggerAnim(ACTION, "misc.awaken");
		playSound(SoundEvents.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.2f, 0.4f);
		playSound(SoundEvents.BLOCK_WOOD_BREAK, 1.0f, 0.5f);
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			return;
		}
		if (isDormant()) {
			PlayerEntity player = getWorld().getClosestPlayer(getX(), getY(), getZ(), WAKE_RANGE,
					entity -> entity instanceof PlayerEntity p && p.isAlive() && !p.isSpectator() && !p.isCreative());
			if (player != null) {
				wake(player);
			}
		} else if (isAwakening()) {
			if (--awakenTicks <= 0) {
				setState(STATE_NONE);
			}
		} else if (getTarget() == null || !getTarget().isAlive()) {
			BlockPos home = home();
			if (++idleTicks >= SETTLE_TICKS && (home == null || home.isWithinDistance(getPos(), 2))) {
				goDormant();
			}
		} else {
			idleTicks = 0;
		}
	}

	@Override
	protected void mobTick() {
		if (isDormant() || isAwakening()) {
			getNavigation().stop();
			return;
		}
		super.mobTick();
	}

	@Override
	protected boolean canStartMove(Move next) {
		return super.canStartMove(next) && !isDormant() && !isAwakening();
	}

	@Override
	protected boolean rootedDuringWindup() {
		return true;
	}

	@Override
	protected void performMove(Move landed) {
		if (landed != SWEEP) {
			super.performMove(landed);
			return;
		}
		swingHand(Hand.MAIN_HAND);
		Vec3d facing = Vec3d.fromPolar(0, getYaw());
		double reach = SWEEP_RANGE + getWidth() / 2;
		for (LivingEntity victim : getWorld().getEntitiesByClass(LivingEntity.class, getBoundingBox().expand(reach, 1, reach),
				entity -> entity != this && entity.isAlive() && !(entity instanceof BarkHollowEntity))) {
			Vec3d to = victim.getPos().subtract(getPos()).multiply(1, 0, 1);
			double distance = to.length() - victim.getWidth() / 2;
			if (distance <= reach && (to.lengthSquared() < 1.0E-4 || to.normalize().dotProduct(facing) >= SWEEP_CONE_COS)) {
				tryAttack(victim);
			}
		}
		if (getWorld() instanceof ServerWorld world) {
			Vec3d front = getPos().add(facing.multiply(1.8));
			world.spawnParticles(ParticleTypes.SWEEP_ATTACK, front.x, getY() + 1.2, front.z, 3, 0.8, 0.2, 0.8, 0);
		}
		playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.5f);
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (!getWorld().isClient && isWeakTo(source)) {
			amount *= WEAK_FACTOR;
		}
		boolean hurt = super.damage(source, amount);
		if (hurt && isDormant()) {
			wake(source.getAttacker() instanceof LivingEntity attacker ? attacker : null);
		}
		return hurt;
	}

	/** Fire, or a melee hit with an axe. */
	public static boolean isWeakTo(DamageSource source) {
		if (source.isIn(DamageTypeTags.IS_FIRE)) {
			return true;
		}
		Entity attacker = source.getAttacker();
		return attacker instanceof LivingEntity living && source.getSource() == attacker
				&& living.getMainHandStack().getItem() instanceof AxeItem;
	}

	@Override
	public boolean isPushable() {
		return !isDormant() && super.isPushable();
	}

	@Override
	public boolean isSilent() {
		return isDormant() || super.isSilent();
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.BLOCK_WOOD_STEP, 0.6f, 0.5f);
		if (getWorld() instanceof ServerWorld world && random.nextInt(4) == 0) {
			world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), getX(), getY(), getZ(), 4, 0.4, 0, 0.4, 0);
		}
	}

	@Override
	public List<String> toggledBones() {
		return List.of("eyes");
	}

	/** The eyes light when it wakes. */
	@Override
	public boolean showsBone(String bone) {
		return !isDormant();
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("Dormant", isDormant());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.getBoolean("Dormant")) {
			goDormant();
		}
	}

	@Nullable
	@Override
	protected RawAnimation stateAnimation(byte state) {
		return state == STATE_DORMANT ? DORMANT : null;
	}

	@Override
	public Move meleeMove() {
		return SWEEP;
	}

	@Override
	protected List<String> actionAnimations() {
		return List.of("attack.sweep", "misc.awaken");
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.BLOCK_WOOD_HIT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.BLOCK_WOOD_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.BLOCK_WOOD_BREAK;
	}

	@Override
	public float getSoundPitch() {
		return super.getSoundPitch() * 0.6f;
	}
}
