package com.embervoid.reach.mob;

import com.embervoid.fx.particle.EmberParticles;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Necropolis elite (DESIGN §4.5): the bell-ringer. A heavy cudgel swing (8 damage, 12-tick windup), and every
 * {@link #BELL_INTERVAL} ticks in a fight it raises its bell for {@link #BELL_WINDUP} ticks and rings it: every bone
 * pile within {@link #BELL_RANGE} re-forms, two Restless Bones rise if there are none around (at most
 * {@link #MAX_RAISED} of its own alive), and players within {@link #STAGGER_RANGE} stagger (Slowness IV, 2 s). The
 * first ring comes {@link #FIRST_BELL} ticks after it finds a target.
 */
public class GraveWardenEntity extends ReachMob {
	public static final int BELL_INTERVAL = 240;
	public static final int FIRST_BELL = 100;
	public static final int BELL_WINDUP = 20;
	public static final double BELL_RANGE = 12;
	public static final double STAGGER_RANGE = 4;
	public static final int RAISED_PER_RING = 2;
	public static final int MAX_RAISED = 4;
	public static final int STAGGER_TICKS = 40;
	public static final int SWING_WINDUP = 12;
	/** Ticks the body stays for {@code misc.death}: to its knees, the bell tolling once, then forward onto the mask, 2.2 s. */
	public static final int DEATH_TICKS = 44;
	static final Move MELEE = new Move("attack.swing", SWING_WINDUP, 40, ReachMobSounds.GRAVE_WARDEN_WINDUP, 1.0f);
	static final Move BELL = new Move("attack.bell", BELL_WINDUP, 0, ReachMobSounds.GRAVE_WARDEN_RAISE_BELL, 1.0f);

	private int bellTimer = -1;
	private final List<UUID> raised = new ArrayList<>();

	public GraveWardenEntity(EntityType<? extends GraveWardenEntity> type, World world) {
		super(type, world);
		experiencePoints = 40;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 29)
				.add(EntityAttributes.GENERIC_ARMOR, 4)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.22)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24);
	}

	/** Ticks until the next ring (-1 out of a fight). */
	public int bellTimer() {
		return bellTimer;
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			return;
		}
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive()) {
			bellTimer = -1;
			return;
		}
		if (bellTimer < 0) {
			bellTimer = FIRST_BELL;
		}
		if (bellTimer > 0) {
			bellTimer--;
		} else if (startBell()) {
			bellTimer = BELL_INTERVAL;
		}
	}

	/** Raises the bell now if no other move is running. */
	public boolean startBell() {
		resetMoveCooldown();
		return startMove(BELL);
	}

	@Override
	protected boolean rootedDuringWindup() {
		return true;
	}

	@Override
	protected void performMove(Move landed) {
		if (landed == BELL) {
			ring();
		} else {
			super.performMove(landed);
		}
	}

	private void ring() {
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		world.playSound(null, getX(), getY(), getZ(), ReachMobSounds.GRAVE_WARDEN_BELL, getSoundCategory(), 3.0f, 1.0f);
		com.embervoid.block.setpiece.SetPieces.tollBellsNear(world, getBlockPos(), BELL_RANGE); // the chapel's great bell answers (PLAN 20.2)
		world.spawnParticles(ParticleTypes.SONIC_BOOM, getX(), getBodyY(0.8), getZ(), 1, 0, 0, 0, 0);
		// The toll rolls out over the ground as a cold ring, and dust rises off the graves it wakes.
		EmberParticles.shockwave(world, getPos(), (float) STAGGER_RANGE, 0x9FD4FF);
		EmberParticles.ringOut(world, EmberParticles.ASH_FLAKE, getPos(), 16, 0.2, 0.15);
		List<RestlessBonesEntity> bones = world.getEntitiesByClass(RestlessBonesEntity.class, getBoundingBox().expand(BELL_RANGE),
				b -> b.isAlive() && b.squaredDistanceTo(this) <= BELL_RANGE * BELL_RANGE);
		bones.forEach(RestlessBonesEntity::reform);
		if (bones.isEmpty()) {
			raise(world);
		}
		for (PlayerEntity player : world.getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(STAGGER_RANGE),
				p -> p.isAlive() && !p.isSpectator() && p.squaredDistanceTo(this) <= STAGGER_RANGE * STAGGER_RANGE)) {
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, STAGGER_TICKS, 3), this);
		}
	}

	/** Raises up to {@link #RAISED_PER_RING} Restless Bones around it, keeping at most {@link #MAX_RAISED} alive. */
	private void raise(ServerWorld world) {
		raised.removeIf(id -> !(world.getEntity(id) instanceof RestlessBonesEntity b) || !b.isAlive());
		for (int i = 0; i < RAISED_PER_RING && raised.size() < MAX_RAISED; i++) {
			RestlessBonesEntity bones = ReachMobs.RESTLESS_BONES.create(world);
			if (bones == null) {
				return;
			}
			BlockPos spot = groundNear(world);
			bones.refreshPositionAndAngles(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360, 0);
			bones.initialize(world, world.getLocalDifficulty(spot), SpawnReason.MOB_SUMMONED, null, null);
			bones.setTarget(getTarget());
			world.spawnEntityAndPassengers(bones);
			world.spawnParticles(ParticleTypes.SOUL, bones.getX(), bones.getY() + 0.2, bones.getZ(), 12, 0.3, 0.1, 0.3, 0.02);
			raised.add(bones.getUuid());
		}
	}

	/** A free spot on the ground within 3 blocks, or its own feet. */
	private BlockPos groundNear(ServerWorld world) {
		for (int tries = 0; tries < 10; tries++) {
			BlockPos candidate = getBlockPos().add(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
			for (int dy = 2; dy >= -2; dy--) {
				BlockPos pos = candidate.up(dy);
				if (world.getBlockState(pos.down()).isSolidBlock(world, pos.down())
						&& world.isSpaceEmpty(ReachMobs.RESTLESS_BONES.createSimpleBoundingBox(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
					return pos;
				}
			}
		}
		return getBlockPos();
	}

	@Override
	public int deathTicks() {
		return DEATH_TICKS;
	}

	/** The bell tolls once more as it goes down on its knees, and the mask hits the stone. */
	@Override
	protected void tickDeath(ServerWorld world, int deathTime) {
		if (deathTime == 14) {
			world.playSound(null, getX(), getY(), getZ(), ReachMobSounds.GRAVE_WARDEN_BELL, getSoundCategory(), 1.5f, 0.8f);
			EmberParticles.shockwave(world, getPos(), 2.0f, 0x9FD4FF);
		} else if (deathTime == 30) {
			EmberParticles.spawn(world, EmberParticles.SOOT, getPos().add(0, 0.3, 0), 8, 0.8, 0.03);
			EmberParticles.ringOut(world, EmberParticles.ASH_FLAKE, getPos(), 10, 0.1, 0.1);
		}
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		NbtList list = new NbtList();
		raised.forEach(id -> list.add(NbtHelper.fromUuid(id)));
		nbt.put("Raised", list);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		raised.clear();
		nbt.getList("Raised", NbtElement.INT_ARRAY_TYPE).forEach(element -> raised.add(NbtHelper.toUuid(element)));
	}

	@Override
	public Move meleeMove() {
		return MELEE;
	}

	@Override
	protected List<String> actionAnimations() {
		return List.of("attack.swing", "attack.bell");
	}

	@Override
	public EntityGroup getGroup() {
		return EntityGroup.UNDEAD;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ReachMobSounds.GRAVE_WARDEN_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ReachMobSounds.GRAVE_WARDEN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ReachMobSounds.GRAVE_WARDEN_DEATH;
	}

	@Override
	protected SoundEvent stepSound() {
		return ReachMobSounds.GRAVE_WARDEN_STEP;
	}

	@Override
	protected float stepVolume() {
		return 0.4f;
	}
}
