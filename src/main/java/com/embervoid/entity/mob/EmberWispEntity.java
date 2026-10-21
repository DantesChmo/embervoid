package com.embervoid.entity.mob;

import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.FlyGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import net.minecraft.world.LightType;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

/**
 * Small cave flyer below y=0 (DESIGN §3.2, bestiary in plans/track-mobs.md). It dives at players in the dark, sets
 * them on fire and lights fires around them, and flees from light: a torch is the counter.
 */
public class EmberWispEntity extends HostileEntity implements GeoEntity {
	/** Block light at or above which the wisp flees and will not attack. */
	static final int LIGHT_LIMIT = 7;
	/** Far enough to find any source that can light a spot to {@link #LIGHT_LIMIT} (luminance 15 − distance 8). */
	private static final int FIRE_SCAN_RADIUS = 15 - LIGHT_LIMIT;
	private static final int BURN_SECONDS = 4;
	private static final int ATTACK_COOLDOWN_TICKS = 20;

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
	private int fireCooldown = 160;
	/** {@link #isBright} answers for the current tick; it is asked several times a tick for the same spots. */
	private final Long2BooleanOpenHashMap brightCache = new Long2BooleanOpenHashMap();
	private long brightCacheTime = -1;

	public EmberWispEntity(EntityType<? extends EmberWispEntity> type, World world) {
		super(type, world);
		moveControl = new FlightMoveControl(this, 20, true);
		experiencePoints = 6;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 8)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.5)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16);
	}

	/** Natural spawns: dark caves below y=0 only (no open sky, so not on a superflat surface either). */
	public static boolean canSpawn(EntityType<? extends HostileEntity> type, ServerWorldAccess world, SpawnReason reason,
			BlockPos pos, Random random) {
		return (reason != SpawnReason.NATURAL || pos.getY() < 0 && !world.isSkyVisible(pos))
				&& HostileEntity.canSpawnInDark(type, world, reason, pos, random);
	}

	/**
	 * Whether the wisp shuns the light at {@code pos}: block light {@link #LIGHT_LIMIT} or more that does not come
	 * from fire, so torches drive it off but the fires it lights itself do not.
	 */
	boolean isBright(BlockPos pos) {
		World world = getWorld();
		int light = world.getLightLevel(LightType.BLOCK, pos);
		if (light < LIGHT_LIMIT) {
			return false;
		}
		long key = pos.asLong();
		long now = world.getTime();
		if (brightCacheTime == now && brightCache.containsKey(key)) {
			return brightCache.get(key);
		}
		if (brightCacheTime != now) {
			brightCache.clear();
			brightCacheTime = now;
		}
		boolean bright = isBrightIgnoringFire(world, pos, light);
		brightCache.put(key, bright);
		return bright;
	}

	/**
	 * Estimates each nearby light source's contribution as luminance minus Manhattan distance (how block light
	 * spreads, ignoring walls). If fire alone explains the light here, it counts only when another source would
	 * reach {@link #LIGHT_LIMIT} on its own.
	 */
	static boolean isBrightIgnoringFire(World world, BlockPos pos, int light) {
		int fireLight = 0;
		int otherLight = 0;
		BlockPos.Mutable cursor = new BlockPos.Mutable();
		int r = FIRE_SCAN_RADIUS;
		for (int dx = -r; dx <= r; dx++) {
			for (int dy = -r; dy <= r; dy++) {
				for (int dz = -r; dz <= r; dz++) {
					int distance = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
					if (distance > 15) {
						continue;
					}
					BlockState state = world.getBlockState(cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz));
					int luminance = state.getLuminance();
					if (luminance == 0) {
						continue;
					}
					int reach = luminance - distance;
					if (state.isIn(BlockTags.FIRE)) {
						fireLight = Math.max(fireLight, reach);
					} else {
						otherLight = Math.max(otherLight, reach);
					}
				}
			}
		}
		return light > fireLight || otherLight >= LIGHT_LIMIT;
	}

	@Override
	protected EntityNavigation createNavigation(World world) {
		BirdNavigation navigation = new BirdNavigation(this, world);
		navigation.setCanPathThroughDoors(false);
		navigation.setCanSwim(false);
		navigation.setCanEnterOpenDoors(true);
		return navigation;
	}

	@Override
	protected void initGoals() {
		goalSelector.add(1, new FleeLightGoal(this));
		goalSelector.add(2, new DiveGoal(this));
		goalSelector.add(6, new FlyGoal(this, 0.8));
		goalSelector.add(8, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this));
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false,
				player -> !isBright(player.getBlockPos())));
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		if (getTarget() != null && --fireCooldown <= 0) {
			fireCooldown = 160 + random.nextInt(81);
			if (getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
				lightFireNearby();
			}
		}
	}

	/** Sets fire to one spot near the wisp, if any of a few random tries can hold fire. */
	private void lightFireNearby() {
		World world = getWorld();
		for (int i = 0; i < 6; i++) {
			BlockPos pos = getBlockPos().add(random.nextInt(5) - 2, random.nextInt(3) - 2, random.nextInt(5) - 2);
			if (world.getBlockState(pos).isAir() && AbstractFireBlock.canPlaceAt(world, pos, Direction.UP)) {
				world.setBlockState(pos, AbstractFireBlock.getState(world, pos));
				playSound(SoundEvents.ITEM_FIRECHARGE_USE, 0.6f, 1.4f);
				return;
			}
		}
	}

	@Override
	public boolean tryAttack(Entity target) {
		boolean hit = super.tryAttack(target);
		if (hit) {
			target.setOnFireFor(BURN_SECONDS);
		}
		return hit;
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		if (getWorld().isClient && random.nextInt(3) == 0) {
			getWorld().addParticle(ParticleTypes.SMALL_FLAME, getParticleX(0.3), getRandomBodyY(), getParticleZ(0.3), 0, 0.01, 0);
		}
	}

	@Override
	public boolean hurtByWater() {
		return true;
	}

	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
		return false;
	}

	@Override
	protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
	}

	@Override
	public float getBrightnessAtEyes() {
		return 1.0f;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_BLAZE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_BLAZE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_BLAZE_DEATH;
	}

	@Override
	protected float getSoundVolume() {
		return 0.4f;
	}

	@Override
	public float getSoundPitch() {
		return super.getSoundPitch() * 1.8f;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(DefaultAnimations.genericFlyIdleController(this));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animations;
	}

	/** In bright light: drop the target and fly to the darkest of a few nearby spots. */
	static class FleeLightGoal extends Goal {
		private final EmberWispEntity wisp;

		FleeLightGoal(EmberWispEntity wisp) {
			this.wisp = wisp;
			setControls(EnumSet.of(Control.MOVE));
		}

		@Override
		public boolean canStart() {
			return wisp.isBright(wisp.getBlockPos());
		}

		@Override
		public boolean shouldContinue() {
			return !wisp.getNavigation().isIdle() || canStart();
		}

		@Override
		public void start() {
			wisp.setTarget(null);
			BlockPos darkest = null;
			int darkestLight = Integer.MAX_VALUE;
			for (int i = 0; i < 16; i++) {
				BlockPos pos = wisp.getBlockPos().add(wisp.random.nextInt(21) - 10, wisp.random.nextInt(9) - 4,
						wisp.random.nextInt(21) - 10);
				if (!wisp.getWorld().getBlockState(pos).isAir()) {
					continue;
				}
				int light = wisp.getWorld().getLightLevel(LightType.BLOCK, pos);
				if (light < darkestLight) {
					darkest = pos;
					darkestLight = light;
				}
			}
			if (darkest != null && darkestLight < LIGHT_LIMIT) {
				wisp.getNavigation().startMovingTo(darkest.getX() + 0.5, darkest.getY() + 0.5, darkest.getZ() + 0.5, 1.5);
			}
		}

		@Override
		public void tick() {
			if (wisp.getNavigation().isIdle() && canStart()) {
				start();
			}
		}
	}

	/** Flies straight at the target and hits it on contact, then pulls back briefly before the next dive. */
	static class DiveGoal extends Goal {
		private final EmberWispEntity wisp;
		private int cooldown;

		DiveGoal(EmberWispEntity wisp) {
			this.wisp = wisp;
			setControls(EnumSet.of(Control.MOVE, Control.LOOK));
		}

		@Override
		public boolean canStart() {
			LivingEntity target = wisp.getTarget();
			return target != null && target.isAlive() && !wisp.isBright(target.getBlockPos());
		}

		@Override
		public boolean shouldContinue() {
			return canStart();
		}

		@Override
		public boolean shouldRunEveryTick() {
			return true;
		}

		@Override
		public void stop() {
			wisp.getNavigation().stop();
		}

		@Override
		public void tick() {
			LivingEntity target = wisp.getTarget();
			if (target == null) {
				return;
			}
			wisp.getLookControl().lookAt(target, 30, 30);
			if (cooldown > 0) {
				cooldown--;
				// Hang back above the target until the next dive.
				Vec3d hover = target.getPos().add(0, 2.5, 0);
				wisp.getMoveControl().moveTo(hover.x, hover.y, hover.z, 0.8);
				return;
			}
			Vec3d aim = target.getEyePos().subtract(0, 0.3, 0);
			wisp.getMoveControl().moveTo(aim.x, aim.y, aim.z, 1.3);
			if (wisp.getBoundingBox().expand(0.3).intersects(target.getBoundingBox())) {
				wisp.tryAttack(target);
				cooldown = getTickCount(ATTACK_COOLDOWN_TICKS) + wisp.random.nextInt(20);
			}
		}
	}
}
