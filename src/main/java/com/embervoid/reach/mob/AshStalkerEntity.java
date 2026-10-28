package com.embervoid.reach.mob;

import com.embervoid.fx.particle.EmberParticles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Fields ambusher (DESIGN §4.5). It lies buried in a furrow: invisible, invulnerable, not solid. A player within
 * {@link #TRIGGER_RANGE} sets it off: the ash boils and it hisses while it rises for {@link #BOIL_TICKS} (still
 * invulnerable), then it leaps out at that player and fights with a clawing swing. Left alone without a target for
 * {@link #REBURY_TICKS} it digs in again. Natural and placed ones start buried.
 *
 * <p>Players matching an {@link #addAmbushImmunity ambush immunity} (the Scarecrow's Mask) do not
 * set it off.
 */
public class AshStalkerEntity extends ReachMob {
	public static final int SWING_WINDUP = 8;
	/** Ticks the body stays for {@code misc.death}: it crumples onto its side for 1.8 s. */
	public static final int DEATH_TICKS = 36;
	static final Move MELEE = new Move("attack.swing", SWING_WINDUP, 25, ReachMobSounds.ASH_STALKER_WINDUP, 1.0f);
	public static final double TRIGGER_RANGE = 5;
	public static final int BOIL_TICKS = 20;
	static final int REBURY_TICKS = 200;
	/** After the leap it hits the first time it touches its target within this many ticks. */
	private static final int LEAP_HIT_TICKS = 15;
	private static final byte STATE_BURIED = 1;
	private static final byte STATE_BOILING = 2;
	private static final RawAnimation BURIED = RawAnimation.begin().thenLoop("misc.buried");
	private static final List<Predicate<PlayerEntity>> AMBUSH_IMMUNE = new CopyOnWriteArrayList<>();

	private int boilTicks;
	private int leapTicks;
	private int idleTicks;
	@Nullable
	private PlayerEntity prey;

	public AshStalkerEntity(EntityType<? extends AshStalkerEntity> type, World world) {
		super(type, world);
		experiencePoints = 8;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 14)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24);
	}

	/** Players matching {@code immune} never set off a buried Ash Stalker (e.g. the Scarecrow's Mask). */
	public static void addAmbushImmunity(Predicate<PlayerEntity> immune) {
		AMBUSH_IMMUNE.add(immune);
	}

	@Override
	public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason reason,
			@Nullable EntityData data, @Nullable NbtCompound nbt) {
		EntityData result = super.initialize(world, difficulty, reason, data, nbt);
		if (reason == SpawnReason.NATURAL || reason == SpawnReason.STRUCTURE || reason == SpawnReason.CHUNK_GENERATION) {
			bury();
		}
		return result;
	}

	@Override
	protected void onPlaced() {
		bury();
	}

	/** Digs in: invisible, invulnerable, waiting for a player to come close. */
	public void bury() {
		cancelMove();
		setTarget(null);
		getNavigation().stop();
		setState(STATE_BURIED);
		boilTicks = 0;
		leapTicks = 0;
		idleTicks = 0;
		prey = null;
	}

	/** Buried or boiling: not out of the ground yet. */
	public boolean isBuried() {
		return getState() == STATE_BURIED || getState() == STATE_BOILING;
	}

	public boolean isBoiling() {
		return getState() == STATE_BOILING;
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			return;
		}
		if (getState() == STATE_BURIED) {
			PlayerEntity player = getWorld().getClosestPlayer(getX(), getY(), getZ(), TRIGGER_RANGE, this::canAmbush);
			if (player != null) {
				startBoiling(player);
			}
		} else if (getState() == STATE_BOILING) {
			boil();
		} else {
			tickOutOfGround();
		}
	}

	private boolean canAmbush(Entity entity) {
		return entity instanceof PlayerEntity player && player.isAlive() && !player.isSpectator() && !player.isCreative()
				&& AMBUSH_IMMUNE.stream().noneMatch(immune -> immune.test(player));
	}

	private void startBoiling(PlayerEntity player) {
		prey = player;
		boilTicks = BOIL_TICKS;
		setState(STATE_BOILING);
		// From here it is seen rising out of the ash (the emerge animation), still invulnerable until it is out.
		triggerAnim(ACTION, "misc.emerge");
		playSound(ReachMobSounds.ASH_STALKER_HISS, 1.0f, 1.0f);
		playSound(ReachMobSounds.ASH_STALKER_BOIL, 1.0f, 1.0f);
	}

	private void boil() {
		if (getWorld() instanceof ServerWorld world) {
			// The ash boils: flakes thrown up, soot, the furrow's own block, ash flakes rising with the body.
			EmberParticles.throwAll(world, EmberParticles.ASH_FLAKE, getPos().add(0, 0.1, 0), 4, new Vec3d(0, 0.12, 0), 0.08);
			world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, getSteppingBlockState()),
					getX(), getY() + 0.1, getZ(), 4, 0.3, 0.05, 0.3, 0.1);
			if (boilTicks % 8 == 0) {
				EmberParticles.spawn(world, EmberParticles.SOOT, getPos().add(0, 0.2, 0), 3, 0.3, 0.02);
			}
		}
		if (--boilTicks > 0) {
			return;
		}
		setState(STATE_NONE);
		PlayerEntity target = prey != null && prey.isAlive() ? prey : null;
		prey = null;
		if (target != null) {
			leapAt(target);
		}
	}

	/** Out of the ground at {@code target}: a leap that hits on contact. */
	private void leapAt(LivingEntity target) {
		setTarget(target);
		Vec3d toTarget = target.getPos().subtract(getPos()).multiply(1, 0, 1);
		Vec3d horizontal = toTarget.lengthSquared() > 1.0E-4 ? toTarget.normalize().multiply(Math.min(1.1, 0.25 * toTarget.length() + 0.3)) : Vec3d.ZERO;
		setVelocity(horizontal.x, 0.55, horizontal.z);
		velocityModified = true;
		lookAtEntity(target, 360, 360);
		leapTicks = LEAP_HIT_TICKS;
		triggerAnim(ACTION, "attack.leap");
		playSound(ReachMobSounds.ASH_STALKER_LEAP, 1.0f, 1.0f);
		if (getWorld() instanceof ServerWorld world) {
			// It bursts out: a ring of ash flakes thrown outward and soot off the body.
			EmberParticles.ringOut(world, EmberParticles.ASH_FLAKE, getPos(), 24, 0.18, 0.2);
			EmberParticles.spawn(world, EmberParticles.SOOT, getPos().add(0, 0.6, 0), 8, 0.4, 0.04);
		}
	}

	@Override
	public int deathTicks() {
		return DEATH_TICKS;
	}

	/** The body falls apart into ash: flakes drift off it while it lies there. */
	@Override
	protected void tickDeath(ServerWorld world, int deathTime) {
		if (deathTime == 16) {
			EmberParticles.ringOut(world, EmberParticles.ASH_FLAKE, getPos(), 14, 0.1, 0.12);
		} else if (deathTime % 4 == 0) {
			EmberParticles.spawn(world, EmberParticles.ASH_FLAKE, getPos().add(0, 0.3, 0), 2, 0.5, 0.02);
		}
	}

	private void tickOutOfGround() {
		LivingEntity target = getTarget();
		if (leapTicks > 0) {
			leapTicks--;
			if (target != null && target.isAlive() && getBoundingBox().expand(0.4).intersects(target.getBoundingBox())) {
				leapTicks = 0;
				swingHand(Hand.MAIN_HAND);
				tryAttack(target);
			}
		}
		if (target == null || !target.isAlive()) {
			if (++idleTicks >= REBURY_TICKS && isOnGround()) {
				bury();
			}
		} else {
			idleTicks = 0;
		}
	}

	@Override
	protected boolean canStartMove(Move next) {
		return super.canStartMove(next) && !isBuried();
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		return isBuried() && !source.isOf(DamageTypes.OUT_OF_WORLD) || super.isInvulnerableTo(source);
	}

	/** Unseen while it lies buried (not the invisibility flag: vanilla resets that from status effects). */
	@Override
	public boolean isInvisible() {
		return getState() == STATE_BURIED || super.isInvisible();
	}

	@Override
	public boolean isPushable() {
		return !isBuried() && super.isPushable();
	}

	@Override
	public boolean canHit() {
		return !isBuried() && super.canHit();
	}

	@Override
	public boolean isSilent() {
		return getState() == STATE_BURIED || super.isSilent();
	}

	@Override
	protected void mobTick() {
		if (isBuried()) {
			getNavigation().stop();
			return;
		}
		super.mobTick();
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("Buried", isBuried());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.getBoolean("Buried")) {
			bury();
		}
	}

	@Nullable
	@Override
	protected RawAnimation stateAnimation(byte state) {
		return state == STATE_BURIED ? BURIED : null;
	}

	@Override
	public Move meleeMove() {
		return MELEE;
	}

	@Override
	protected List<String> actionAnimations() {
		return List.of("attack.swing", "misc.emerge", "attack.leap");
	}

	@Override
	public EntityGroup getGroup() {
		return EntityGroup.UNDEAD;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ReachMobSounds.ASH_STALKER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ReachMobSounds.ASH_STALKER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ReachMobSounds.ASH_STALKER_DEATH;
	}

	@Override
	protected SoundEvent stepSound() {
		return ReachMobSounds.ASH_STALKER_STEP;
	}
}
