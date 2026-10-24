package com.embervoid.reach.mob;

import com.embervoid.fx.particle.EmberParticles;
import net.minecraft.block.AbstractCandleBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.TorchBlock;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Necropolis harasser (DESIGN §4.5). Drawn to light: it flies to the nearest light source within
 * {@link #LIGHT_RANGE} (block luminance ≥ {@link #MIN_LUMINANCE}) and, with {@code mobGriefing} on, snuffs placed
 * torches (broken, they drop) and lit candles. A target within {@link #BURST_RADIUS} gets the pollen burst: its wings
 * glow for {@link #BURST_WINDUP} ticks, then players around it are blinded for 3 s. Otherwise it bites.
 */
public class CryptMothEntity extends FlyingReachMob {
	public static final int LIGHT_RANGE = 12;
	public static final int MIN_LUMINANCE = 10;
	public static final int BURST_WINDUP = 15;
	public static final double BURST_RADIUS = 3;
	public static final int BLINDNESS_TICKS = 60;
	/** Ticks the body stays for {@code misc.death}: it drops out of the air and turns onto its back, 1.6 s. */
	public static final int DEATH_TICKS = 32;
	/** Close enough to a light to snuff it. */
	private static final double SNUFF_RANGE = 1.8;
	/** A target this close draws it away from the light. */
	private static final double FIGHT_RANGE = 4;
	static final Move BITE = new Move("attack.bite", 6, 25, null, 1.0f);
	static final Move BURST = new Move("attack.burst", BURST_WINDUP, 100, ReachMobSounds.CRYPT_MOTH_GLOW, 1.0f);
	private static final byte STATE_GLOWING = 1;
	private static final List<String> GLOW_BONES = List.of("glow_left", "glow_right");
	private static final DustParticleEffect POLLEN = new DustParticleEffect(new Vector3f(0.86f, 1f, 0.63f), 1.2f);

	@Nullable
	private BlockPos light;
	private int lightScan;

	public CryptMothEntity(EntityType<? extends CryptMothEntity> type, World world) {
		super(type, world);
		experiencePoints = 8;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 12)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.5)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16);
	}

	@Override
	protected void initAttackGoals() {
		goalSelector.add(2, new MothGoal(this));
	}

	/** The light it is drawn to, or null. */
	@Nullable
	public BlockPos light() {
		return light;
	}

	/** Starts a pollen burst now if no move is running. */
	public boolean startBurst() {
		return startMove(BURST);
	}

	static boolean isLight(BlockState state) {
		return state.getLuminance() >= MIN_LUMINANCE && state.getFluidState().isEmpty();
	}

	static boolean isSnuffable(BlockState state) {
		return state.getBlock() instanceof TorchBlock
				|| state.getBlock() instanceof AbstractCandleBlock && state.get(AbstractCandleBlock.LIT);
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			return;
		}
		if (light != null && !isLight(getWorld().getBlockState(light)) && !isSnuffable(getWorld().getBlockState(light))) {
			light = null;
		}
		if (--lightScan <= 0) {
			lightScan = 40;
			Optional<BlockPos> found = BlockPos.findClosest(getBlockPos(), LIGHT_RANGE, LIGHT_RANGE / 2,
					pos -> isLight(getWorld().getBlockState(pos)));
			light = found.map(BlockPos::toImmutable).orElse(null);
		}
		if (light != null && light.toCenterPos().isInRange(getPos(), SNUFF_RANGE)) {
			snuff(light);
		}
	}

	private void snuff(BlockPos pos) {
		BlockState state = getWorld().getBlockState(pos);
		if (!isSnuffable(state) || !getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
			return;
		}
		if (state.getBlock() instanceof TorchBlock) {
			getWorld().breakBlock(pos, true, this);
		} else {
			getWorld().setBlockState(pos, state.with(AbstractCandleBlock.LIT, false));
		}
		playSound(ReachMobSounds.CRYPT_MOTH_SNUFF, 1.0f, 1.0f);
		light = null;
		lightScan = 0;
	}

	@Override
	protected void onMoveStarted(Move started) {
		if (started == BURST) {
			setState(STATE_GLOWING);
		}
	}

	@Override
	protected void performMove(Move landed) {
		if (landed != BURST) {
			super.performMove(landed);
			return;
		}
		setState(STATE_NONE);
		for (PlayerEntity player : getWorld().getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(BURST_RADIUS),
				player -> player.isAlive() && !player.isSpectator() && player.squaredDistanceTo(this) <= BURST_RADIUS * BURST_RADIUS)) {
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, BLINDNESS_TICKS), this);
		}
		if (getWorld() instanceof ServerWorld world) {
			world.spawnParticles(POLLEN, getX(), getBodyY(0.5), getZ(), 60, BURST_RADIUS / 2, 1, BURST_RADIUS / 2, 0.02);
		}
		playSound(ReachMobSounds.CRYPT_MOTH_BURST, 1.2f, 1.0f);
	}

	@Override
	public void cancelMove() {
		super.cancelMove();
		if (getState() == STATE_GLOWING && !getWorld().isClient) {
			setState(STATE_NONE);
		}
	}

	@Override
	public int deathTicks() {
		return DEATH_TICKS;
	}

	/** Killed in the air, the body drops. */
	@Override
	public void onDeath(DamageSource source) {
		super.onDeath(source);
		if (!getWorld().isClient) {
			setState(STATE_NONE);
			setNoGravity(false);
		}
	}

	/** Pollen dust puffs off the wings as it tumbles down. */
	@Override
	protected void tickDeath(ServerWorld world, int deathTime) {
		if (deathTime % 4 == 0) {
			world.spawnParticles(POLLEN, getX(), getBodyY(0.5), getZ(), 2, 0.3, 0.2, 0.3, 0.01);
			EmberParticles.spawn(world, EmberParticles.ASH_FLAKE, getPos().add(0, 0.2, 0), 1, 0.3, 0.02);
		}
	}

	@Override
	public List<String> toggledBones() {
		return GLOW_BONES;
	}

	/** The wings glow while a pollen burst winds up. */
	@Override
	public boolean showsBone(String bone) {
		return getState() == STATE_GLOWING;
	}

	@Override
	public Move meleeMove() {
		return BITE;
	}

	@Override
	protected List<String> actionAnimations() {
		return List.of("attack.bite", "attack.burst");
	}

	@Override
	public EntityGroup getGroup() {
		return EntityGroup.ARTHROPOD;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ReachMobSounds.CRYPT_MOTH_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ReachMobSounds.CRYPT_MOTH_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ReachMobSounds.CRYPT_MOTH_DEATH;
	}

	/**
	 * A target close by: burst when it can, bite in reach. Otherwise to the light; otherwise after the target. With
	 * neither, the wander goal takes over.
	 */
	static class MothGoal extends Goal {
		private final CryptMothEntity moth;

		MothGoal(CryptMothEntity moth) {
			this.moth = moth;
			setControls(EnumSet.of(Control.MOVE, Control.LOOK));
		}

		@Override
		public boolean canStart() {
			return moth.light != null || moth.getTarget() != null && moth.getTarget().isAlive();
		}

		@Override
		public boolean shouldRunEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity target = moth.getTarget();
			if (target != null && target.isAlive() && moth.squaredDistanceTo(target) <= FIGHT_RANGE * FIGHT_RANGE) {
				moth.getLookControl().lookAt(target, 30, 30);
				Vec3d at = target.getEyePos();
				moth.getMoveControl().moveTo(at.x, at.y, at.z, 1.0);
				if (moth.squaredDistanceTo(target) <= BURST_RADIUS * BURST_RADIUS && moth.startMove(BURST)) {
					return;
				}
				if (moth.squaredDistanceTo(target) <= moth.squaredMeleeReach(target)) {
					moth.startMove(BITE);
				}
			} else if (moth.light != null) {
				Vec3d at = moth.light.toCenterPos().add(0, 0.6, 0);
				moth.getMoveControl().moveTo(at.x + (moth.random.nextDouble() - 0.5), at.y, at.z + (moth.random.nextDouble() - 0.5), 1.0);
			} else if (target != null && target.isAlive()) {
				Vec3d at = target.getEyePos();
				moth.getMoveControl().moveTo(at.x, at.y, at.z, 1.0);
			}
		}
	}
}
