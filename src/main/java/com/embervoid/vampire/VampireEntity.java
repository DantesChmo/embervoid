package com.embervoid.vampire;

import com.embervoid.entity.mob.MobAnimations;
import com.embervoid.fx.particle.EmberParticles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A Vampire of the blood new moon in its friendly form (DESIGN §3.3). It strolls near where it appeared,
 * watches the players and talks when spoken to. It cannot be hurt: the first player hit on any Vampire provokes it
 * instead ({@link VampireNight#provoke}), and it turns into the boss after a short transformation.
 *
 * <p>Its life is owned by {@link VampireNight}: a Vampire that does not belong to the current night (loaded later
 * from an unloaded chunk, after a restart, after the others vanished) disappears on its next tick.
 *
 * <p>Animations ({@link MobAnimations}): the blow that provokes it shows {@code misc.hurt} under the
 * transformation; blood drops fall off it while it turns. Only {@code /kill} and the void end it: then
 * {@code misc.death} folds it to the ground and the body stays {@link #DEATH_TICKS} ticks.
 */
public class VampireEntity extends PathAwareEntity implements GeoEntity {
	/** Length of the transformation, matching the {@code misc.transform} animation. */
	public static final int TURN_TICKS = 50;
	private static final DustParticleEffect BLOOD = new DustParticleEffect(new Vector3f(0.6f, 0.02f, 0.05f), 1.4f);
	private static final RawAnimation TRANSFORM = RawAnimation.begin().thenPlayAndHold("misc.transform");
	private static final int TALK_LINES = 4;
	/** How long the body stays after a kill: the length of {@code misc.death} (2 s). */
	public static final int DEATH_TICKS = 40;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	/** The blood night this Vampire belongs to ({@link VampireState#nightId}). */
	private int nightId;
	/** Ticks left of the transformation, 0 while friendly. */
	private int turnTicks;

	public VampireEntity(EntityType<? extends VampireEntity> type, World world) {
		super(type, world);
		setPersistent();
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 40)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.22)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 12, 0.8f));
		goalSelector.add(2, new WanderAroundGoal(this, 0.6, 160));
		goalSelector.add(3, new LookAroundGoal(this));
	}

	void setNightId(int nightId) {
		this.nightId = nightId;
	}

	int nightId() {
		return nightId;
	}

	public boolean isTurning() {
		return turnTicks > 0;
	}

	/** Starts the transformation; {@link VampireNight#onTurned} runs when it is over. */
	void startTurning() {
		if (isTurning()) {
			return;
		}
		turnTicks = TURN_TICKS;
		setAiDisabled(true);
		getNavigation().stop();
		triggerAnim("transform", "transform");
		getWorld().playSound(null, getX(), getY(), getZ(), VampireSounds.VAMPIRE_TRANSFORM, SoundCategory.HOSTILE, 2.0f, 0.6f);
	}

	/** Leaves without a trace but a puff of smoke (the others, once one is provoked). */
	void vanish() {
		if (getWorld() instanceof ServerWorld world) {
			world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 20, 0.3, 0.6, 0.3, 0.02);
			world.playSound(null, getX(), getY(), getZ(), VampireSounds.VAMPIRE_VANISH, SoundCategory.NEUTRAL, 1.0f, 0.7f);
		}
		discard();
	}

	// tick(), not mobTick(): mobTick does not run while the AI is off during the transformation.
	@Override
	public void tick() {
		super.tick();
		if (!(getWorld() instanceof ServerWorld world) || isRemoved()) {
			return;
		}
		if (!VampireNight.belongs(this)) {
			vanish();
			return;
		}
		if (turnTicks > 0) {
			turnTicks--;
			// Blood rises around it, faster towards the end, and drops fall off it as it lifts.
			int count = 2 + (TURN_TICKS - turnTicks) / 8;
			world.spawnParticles(BLOOD, getX(), getY() + 0.2, getZ(), count, 0.5, 0.1, 0.5, 0);
			world.spawnParticles(ParticleTypes.SMOKE, getX(), getY() + 1.2, getZ(), 1, 0.3, 0.5, 0.3, 0.01);
			EmberParticles.throwAll(world, EmberParticles.BLOOD_DROP, getPos().add(0, 1.0 + (TURN_TICKS - turnTicks) * 0.01, 0), 1 + (TURN_TICKS - turnTicks) / 16,
					new Vec3d(0, -0.02, 0), 0.08);
			if (turnTicks == 0) {
				world.spawnParticles(BLOOD, getX(), getY() + 1, getZ(), 60, 0.6, 1.0, 0.6, 0);
				EmberParticles.ringOut(world, EmberParticles.BLOOD_DROP, getPos().add(0, 1.2, 0), 24, 0.25, 0.15);
				EmberParticles.burst(world, getPos().add(0, 1.2, 0), 1.6f, 0xb0141e);
				world.playSound(null, getX(), getY(), getZ(), VampireSounds.VAMPIRE_TURNED, SoundCategory.HOSTILE, 1.5f, 1.2f);
				VampireNight.onTurned(this);
			}
		}
	}

	@Override
	protected ActionResult interactMob(PlayerEntity player, Hand hand) {
		if (hand != Hand.MAIN_HAND || isTurning()) {
			return ActionResult.PASS;
		}
		if (player instanceof ServerPlayerEntity serverPlayer) {
			getLookControl().lookAt(player, 30, 30);
			int line = random.nextInt(TALK_LINES);
			serverPlayer.sendMessage(Text.translatable("embervoid.vampire.talk",
					getDisplayName(), Text.translatable("embervoid.vampire.talk." + line)).formatted(Formatting.GRAY), false);
		}
		return ActionResult.success(getWorld().isClient);
	}

	/** A player's hit provokes it (and does no damage); nothing else harms it but the void and {@code /kill}. */
	@Override
	public boolean damage(DamageSource source, float amount) {
		if (getWorld().isClient) {
			return false;
		}
		if (source.isOf(DamageTypes.OUT_OF_WORLD) || source.isOf(DamageTypes.GENERIC_KILL)) {
			return super.damage(source, amount);
		}
		Entity attacker = source.getAttacker();
		if (attacker instanceof ServerPlayerEntity player && !player.isSpectator()) {
			if (!isTurning()) {
				// The jolt of the blow, under the transformation that follows at once.
				triggerAnim(MobAnimations.HURT_CONTROLLER, MobAnimations.HURT);
			}
			VampireNight.provoke(this, player);
		}
		return false;
	}

	@Override
	public void onDeath(DamageSource source) {
		boolean firstDeath = !dead;
		super.onDeath(source);
		if (firstDeath && !getWorld().isClient) {
			triggerAnim(MobAnimations.DEATH_CONTROLLER, MobAnimations.DEATH);
		}
	}

	public int deathTicks() {
		return DEATH_TICKS;
	}

	/** It sinks to its knees and over (misc.death), blood dropping from it, then goes in smoke. */
	@Override
	protected void updatePostDeath() {
		deathTime++;
		if (getWorld() instanceof ServerWorld world && !isRemoved()) {
			if (deathTime % 3 == 0) {
				EmberParticles.spawn(world, EmberParticles.BLOOD_DROP, getPos().add(0, 0.5, 0), 1, 0.3, 0.01);
			}
			if (deathTime == deathTicks() - 1) {
				world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.6, getZ(), 20, 0.4, 0.4, 0.4, 0.02);
			}
		}
		MobAnimations.tickDeath(this, deathTime, deathTicks());
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
	public boolean canBeLeashedBy(PlayerEntity player) {
		return false;
	}

	@Override
	public boolean isFireImmune() {
		return true;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("NightId", nightId);
		nbt.putInt("TurnTicks", turnTicks);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		nightId = nbt.getInt("NightId");
		turnTicks = nbt.getInt("TurnTicks");
	}

	/** Walk / idle, the jolt, the transformation (held), the death last: later controllers win on the bones they animate. */
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(DefaultAnimations.genericWalkIdleController(this));
		controllers.add(MobAnimations.hurtController(this, MobAnimations.HURT));
		controllers.add(new AnimationController<>(this, "transform", 0, state -> PlayState.STOP)
				.triggerableAnim("transform", TRANSFORM));
		controllers.add(MobAnimations.deathController(this));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animations;
	}
}
