package com.embervoid.reach.mob;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The Lantern Hag's fire flask (DESIGN §4.5): thrown in an arc, it shatters on whatever it hits for {@link #DAMAGE}
 * to living things within {@link #BURST_RADIUS}, and leaves a fire patch of radius {@link #PATCH_RADIUS} that sets
 * anything standing in it alight while it lasts ({@link #PATCH_TICKS}), its thrower excepted. The patch is the same
 * entity, resting where it broke (invisible, so the renderer draws nothing); no fire blocks are placed.
 */
public class FireFlaskEntity extends ThrownItemEntity {
	public static final float DAMAGE = 4;
	public static final double BURST_RADIUS = 1.2;
	public static final double PATCH_RADIUS = 1.5;
	public static final int PATCH_TICKS = 60;
	private int patchTicks = -1;

	public FireFlaskEntity(EntityType<? extends FireFlaskEntity> type, World world) {
		super(type, world);
	}

	public FireFlaskEntity(World world, LivingEntity owner) {
		super(ReachMobs.FIRE_FLASK, owner, world);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.SPLASH_POTION;
	}

	/** Whether it has broken and is now a fire patch. */
	public boolean isPatch() {
		return patchTicks >= 0;
	}

	@Override
	protected void onCollision(HitResult hit) {
		if (isPatch()) {
			return;
		}
		super.onCollision(hit);
		if (getWorld() instanceof ServerWorld world) {
			Vec3d at = hit.getPos();
			setPosition(at.x, at.y, at.z);
			Entity owner = getOwner();
			for (LivingEntity victim : world.getEntitiesByClass(LivingEntity.class, new Box(at, at).expand(BURST_RADIUS),
					entity -> entity.isAlive() && entity != owner)) {
				victim.damage(getDamageSources().thrown(this, owner), DAMAGE);
			}
			world.playSound(null, at.x, at.y, at.z, ReachMobSounds.FIRE_FLASK_SHATTER, SoundCategory.HOSTILE, 1.0f, 1.0f);
			world.playSound(null, at.x, at.y, at.z, ReachMobSounds.FIRE_FLASK_IGNITE, SoundCategory.HOSTILE, 1.0f, 1.0f);
			world.spawnParticles(ParticleTypes.FLAME, at.x, at.y + 0.2, at.z, 30, 0.6, 0.2, 0.6, 0.05);
			patchTicks = PATCH_TICKS;
			setVelocity(Vec3d.ZERO);
			setNoGravity(true);
			setInvisible(true);
		}
	}

	@Override
	public void tick() {
		if (!isPatch()) {
			super.tick();
			return;
		}
		// A patch: no flight, it only burns what stands in it.
		baseTick();
		if (getWorld() instanceof ServerWorld world) {
			world.spawnParticles(ParticleTypes.FLAME, getX(), getY() + 0.1, getZ(), 4, PATCH_RADIUS * 0.6, 0.05, PATCH_RADIUS * 0.6, 0.01);
			Entity owner = getOwner();
			for (LivingEntity victim : world.getEntitiesByClass(LivingEntity.class,
					getBoundingBox().expand(PATCH_RADIUS, 0.5, PATCH_RADIUS),
					entity -> entity.isAlive() && entity != owner
							&& entity.squaredDistanceTo(getX(), entity.getY(), getZ()) <= PATCH_RADIUS * PATCH_RADIUS)) {
				if (!victim.isFireImmune()) {
					victim.setOnFireFor(3);
				}
			}
			if (--patchTicks <= 0) {
				discard();
			}
		}
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("PatchTicks", patchTicks);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		patchTicks = nbt.contains("PatchTicks") ? nbt.getInt("PatchTicks") : -1;
	}
}
