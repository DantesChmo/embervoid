package com.embervoid.reach.boss;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A lantern hanging from the Grieving Tree's branches (DESIGN §4.5, PLAN 12.5). It follows its tree at a fixed angle,
 * distance and height. Any projectile breaks it; a melee hit only while its branch is lowered after a branch sweep.
 * A broken lantern stuns the tree ({@link GrievingTreeEntity#onLanternBroken}). Not saved: a tree that is gone takes
 * its lanterns with it.
 */
public class TreeLanternEntity extends Entity {
	private static final TrackedData<Boolean> LOWERED = DataTracker.registerData(TreeLanternEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	/** Height of a lowered lantern above the tree's feet: within a sword's reach. */
	public static final double LOWERED_HEIGHT = 1.6;

	@Nullable
	private UUID tree;
	private double angle;
	private double distance;
	private double height;
	private int loweredTicks;

	public TreeLanternEntity(EntityType<? extends TreeLanternEntity> type, World world) {
		super(type, world);
		noClip = true;
		setNoGravity(true);
	}

	/** Hangs this lantern on {@code tree}'s branch at {@code angle} (radians), {@code distance} out, {@code height} up. */
	public void hangOn(GrievingTreeEntity tree, double angle, double distance, double height) {
		this.tree = tree.getUuid();
		this.angle = angle;
		this.distance = distance;
		this.height = height;
		follow(tree);
	}

	@Override
	protected void initDataTracker() {
		dataTracker.startTracking(LOWERED, false);
	}

	public boolean isLowered() {
		return dataTracker.get(LOWERED);
	}

	/** Lowers the lantern's branch to reach height for {@code ticks} ticks. */
	public void lower(int ticks) {
		loweredTicks = ticks;
		dataTracker.set(LOWERED, true);
	}

	@Nullable
	public GrievingTreeEntity tree() {
		return tree != null && getWorld() instanceof ServerWorld world && world.getEntity(tree) instanceof GrievingTreeEntity t ? t : null;
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			return;
		}
		GrievingTreeEntity owner = tree();
		if (owner == null || !owner.isAlive()) {
			discard();
			return;
		}
		if (loweredTicks > 0 && --loweredTicks == 0) {
			dataTracker.set(LOWERED, false);
		}
		follow(owner);
		if (age % 10 == 0 && getWorld() instanceof ServerWorld world) {
			world.spawnParticles(ParticleTypes.SMALL_FLAME, getX(), getY() + 0.3, getZ(), 1, 0.05, 0.05, 0.05, 0);
		}
	}

	private void follow(GrievingTreeEntity owner) {
		double y = isLowered() ? LOWERED_HEIGHT : height;
		refreshPositionAndAngles(owner.getX() + Math.cos(angle) * distance, owner.getY() + y, owner.getZ() + Math.sin(angle) * distance, 0, 0);
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (getWorld().isClient || isRemoved() || isInvulnerableTo(source)) {
			return false;
		}
		boolean shot = source.getSource() instanceof ProjectileEntity;
		boolean struck = isLowered() && source.getSource() instanceof PlayerEntity;
		if (!shot && !struck) {
			return false;
		}
		shatter();
		return true;
	}

	/** Breaks: glass, flame, and the tree is stunned. */
	public void shatter() {
		if (isRemoved()) {
			return;
		}
		if (getWorld() instanceof ServerWorld world) {
			world.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1.5f, 1.0f);
			world.spawnParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 20, 0.2, 0.2, 0.2, 0.05);
		}
		GrievingTreeEntity owner = tree();
		discard();
		if (owner != null) {
			owner.onLanternBroken(this);
		}
	}

	@Override
	public boolean canHit() {
		return !isRemoved();
	}

	@Override
	public boolean isAttackable() {
		return true;
	}

	@Override
	public boolean shouldSave() {
		return false;
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
	}
}
