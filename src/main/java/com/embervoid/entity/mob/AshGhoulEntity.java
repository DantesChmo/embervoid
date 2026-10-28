package com.embervoid.entity.mob;

import com.embervoid.fx.particle.EmberParticles;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Fast, weak undead that hunts in packs of 3–5 at night and underground (DESIGN §3.2). When one finds a target the
 * rest of the pack nearby joins in. Burns in daylight. On death it bursts into a cloud of ash that blinds whoever
 * stands in it, so killing the first ghoul of a pack blinds you for the next ones.
 *
 * <p>Animations ({@link MobAnimations}): a flinch on a hit with a cooldown, and on death the body collapses
 * ({@code misc.death}) and lies there {@link #DEATH_TICKS} ticks, ash flaking off it, before it goes; the blinding
 * cloud bursts at the moment of death.
 */
public class AshGhoulEntity extends HostileEntity implements GeoEntity {
	/** How far the pack call reaches when a ghoul picks a target. */
	private static final double PACK_RANGE = 16;
	private static final float CLOUD_RADIUS = 2.5f;
	private static final int CLOUD_TICKS = 80;
	private static final int BLINDNESS_TICKS = 60;
	/** Pack members spawn within this many blocks of the leader, horizontally... */
	private static final int PACK_RADIUS = 3;
	/** ...and this many blocks above or below its feet. */
	private static final int PACK_STEP = 2;
	private static final int PACK_TRIES_PER_MEMBER = 10;
	/** How long the body lies there after death: the length of {@code misc.death} (1.8 s). */
	public static final int DEATH_TICKS = 36;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private int lastHurtAnimation = -100;
	private int hurtFlinches;

	public AshGhoulEntity(EntityType<? extends AshGhoulEntity> type, World world) {
		super(type, world);
		experiencePoints = 5;
	}

	/**
	 * A natural spawn is the pack leader: it brings 2–4 more ghouls around it. Vanilla's own group spawning scatters
	 * a group over a few random spots and drops the ones that fail, so it rarely produces a real pack; the biome
	 * spawn entry is therefore a group of 1 and the pack comes from here.
	 */
	@Override
	public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason reason,
			@Nullable EntityData data, @Nullable NbtCompound nbt) {
		EntityData result = super.initialize(world, difficulty, reason, data, nbt);
		if (reason == SpawnReason.NATURAL) {
			spawnPack(world, difficulty, 2 + random.nextInt(3));
		}
		return result;
	}

	private void spawnPack(ServerWorldAccess world, LocalDifficulty difficulty, int count) {
		EntityType<?> type = getType();
		int blockLightLimit = world.getDimension().monsterSpawnBlockLightLimit();
		BlockPos leader = getBlockPos();
		Set<BlockPos> taken = new HashSet<>();
		taken.add(leader);
		for (int tries = 0; tries < count * PACK_TRIES_PER_MEMBER && count > 0; tries++) {
			int x = leader.getX() + random.nextInt(2 * PACK_RADIUS + 1) - PACK_RADIUS;
			int z = leader.getZ() + random.nextInt(2 * PACK_RADIUS + 1) - PACK_RADIUS;
			// Ground level near the leader's feet: the leader already passed the darkness roll, so members only need
			// a real place to stand, out of fluids and out of block light.
			OptionalInt y = PackPlacement.findStandingY(leader.getY(), PACK_STEP, candidate -> {
				BlockPos pos = new BlockPos(x, candidate, z);
				return !taken.contains(pos)
						&& SpawnHelper.canSpawn(SpawnRestriction.Location.ON_GROUND, world, pos, type)
						&& world.getLightLevel(LightType.BLOCK, pos) <= blockLightLimit
						&& world.isSpaceEmpty(type.createSimpleBoundingBox(x + 0.5, candidate, z + 0.5));
			});
			if (y.isEmpty()) {
				continue;
			}
			BlockPos pos = new BlockPos(x, y.getAsInt(), z);
			AshGhoulEntity member = (AshGhoulEntity) type.create(world.toServerWorld());
			if (member == null) {
				return;
			}
			member.refreshPositionAndAngles(x + 0.5, pos.getY(), z + 0.5, random.nextFloat() * 360, 0);
			// Not NATURAL, so a pack member does not bring a pack of its own.
			member.initialize(world, difficulty, SpawnReason.REINFORCEMENT, null, null);
			world.spawnEntityAndPassengers(member);
			taken.add(pos);
			count--;
		}
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 12)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.33)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.5)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(2, new MeleeAttackGoal(this, 1.1, false));
		goalSelector.add(6, new WanderAroundFarGoal(this, 0.9));
		goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8));
		goalSelector.add(8, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this).setGroupRevenge());
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
		targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
	}

	@Override
	public void setTarget(@Nullable LivingEntity target) {
		boolean fresh = target != null && target != getTarget();
		super.setTarget(target);
		if (fresh && !getWorld().isClient) {
			callPack(target);
		}
	}

	/** Gives idle ghouls nearby the same target. */
	private void callPack(LivingEntity target) {
		for (AshGhoulEntity other : getWorld().getEntitiesByClass(AshGhoulEntity.class,
				getBoundingBox().expand(PACK_RANGE), ghoul -> ghoul != this && ghoul.getTarget() == null)) {
			other.setTargetQuietly(target);
		}
	}

	/** Sets the target without calling the pack again (no chain reaction across the whole map). */
	private void setTargetQuietly(LivingEntity target) {
		super.setTarget(target);
	}

	@Override
	public void tickMovement() {
		if (isAlive() && isAffectedByDaylight()) {
			setOnFireFor(8);
		}
		super.tickMovement();
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		boolean hurt = super.damage(source, amount);
		if (MobAnimations.mayFlinch(this, hurt, amount, lastHurtAnimation)) {
			lastHurtAnimation = age;
			hurtFlinches++;
			triggerAnim(MobAnimations.HURT_CONTROLLER, MobAnimations.HURT);
		}
		return hurt;
	}

	/** Flinches shown so far (tests). */
	public int hurtFlinches() {
		return hurtFlinches;
	}

	@Override
	public void onDeath(DamageSource source) {
		boolean firstDeath = !dead;
		super.onDeath(source);
		if (firstDeath && getWorld() instanceof ServerWorld world) {
			burstIntoAsh(world);
			triggerAnim(MobAnimations.DEATH_CONTROLLER, MobAnimations.DEATH);
		}
	}

	public int deathTicks() {
		return DEATH_TICKS;
	}

	/** The body lies there for {@link #deathTicks}, ash flaking off it and a last ember or two, then goes. */
	@Override
	protected void updatePostDeath() {
		deathTime++;
		if (getWorld() instanceof ServerWorld world && !isRemoved()) {
			if (deathTime % 4 == 0) {
				EmberParticles.spawn(world, EmberParticles.ASH_FLAKE, getPos().add(0, 0.4, 0), 2, 0.5, 0.02);
			}
			if (deathTime == 19) {
				// It hits the ground (misc.death lands at 0.95 s): a puff of ash and sparks off the embers.
				EmberParticles.spawn(world, EmberParticles.SOOT, getPos().add(0, 0.2, 0), 6, 0.5, 0.02);
				EmberParticles.throwAll(world, EmberParticles.CINDER_SPARK, getPos().add(0, 0.3, 0), 5, new Vec3d(0, 0.15, 0), 0.12);
			}
		}
		MobAnimations.tickDeath(this, deathTime, deathTicks());
	}

	private void burstIntoAsh(ServerWorld world) {
		AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(world, getX(), getY(), getZ());
		cloud.setOwner(this);
		cloud.setParticleType(ParticleTypes.LARGE_SMOKE);
		cloud.setRadius(CLOUD_RADIUS);
		cloud.setDuration(CLOUD_TICKS);
		cloud.setWaitTime(0);
		cloud.setRadiusGrowth(-CLOUD_RADIUS / (CLOUD_TICKS * 2));
		cloud.addEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, BLINDNESS_TICKS));
		world.spawnEntity(cloud);
		world.spawnParticles(ParticleTypes.ASH, getX(), getBodyY(0.5), getZ(), 60, 1.0, 0.8, 1.0, 0.02);
		world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getBodyY(0.5), getZ(), 20, 0.6, 0.5, 0.6, 0.02);
		// The mod's own ash flakes drift out of the cloud, with a few embers thrown up from the body.
		EmberParticles.spawn(world, EmberParticles.ASH_FLAKE, getPos().add(0, 0.8, 0), 40, 1.2, 0.03);
		EmberParticles.ringOut(world, EmberParticles.SOOT, getPos().add(0, 0.3, 0), 10, 0.08, 0.04);
		EmberParticles.throwAll(world, EmberParticles.EMBER, getPos().add(0, 1.0, 0), 8, new Vec3d(0, 0.12, 0), 0.08);
		world.playSound(null, getX(), getY(), getZ(), MobSounds.ASH_GHOUL_BURST, SoundCategory.HOSTILE, 1.0f, 0.6f);
	}

	@Override
	public EntityGroup getGroup() {
		return EntityGroup.UNDEAD;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return MobSounds.ASH_GHOUL_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return MobSounds.ASH_GHOUL_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return MobSounds.ASH_GHOUL_DEATH;
	}

	@Override
	public float getSoundPitch() {
		return super.getSoundPitch() * 1.3f;
	}

	@Override
	public int getLimitPerChunk() {
		return 5;
	}

	/** Walk / idle, the flinch, the claw rake, the death last: later controllers win on the bones they animate. */
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(DefaultAnimations.genericWalkIdleController(this));
		controllers.add(MobAnimations.hurtController(this, MobAnimations.HURT));
		controllers.add(DefaultAnimations.genericAttackAnimation(this, DefaultAnimations.ATTACK_SWING));
		controllers.add(MobAnimations.deathController(this));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animations;
	}
}
