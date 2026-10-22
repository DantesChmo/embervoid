package com.embervoid.reach.gear;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/** A thrown Ash Bomb: bursts on impact into a ring of fire that burns and scorches every mob around (no fire blocks). */
public class AshBombEntity extends ThrownItemEntity {
	public AshBombEntity(EntityType<? extends AshBombEntity> type, World world) {
		super(type, world);
	}

	public AshBombEntity(World world, LivingEntity owner) {
		super(GearConsumables.ASH_BOMB_ENTITY, owner, world);
	}

	@Override
	protected Item getDefaultItem() {
		return GearConsumables.ASH_BOMB;
	}

	@Override
	protected void onCollision(HitResult hit) {
		super.onCollision(hit);
		if (!getWorld().isClient()) {
			burst();
			discard();
		}
	}

	/** Ember damage and fire to every living entity within the radius, except the thrower's teammates. */
	public void burst() {
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		GearConfig.Consumables config = GearConfig.get().consumables();
		float radius = config.bombRadius();
		Entity owner = getOwner();
		for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, getBoundingBox().expand(radius),
				e -> e.isAlive() && e.squaredDistanceTo(this) <= radius * radius && (owner == null || !owner.isTeammate(e)))) {
			target.damage(owner == null ? world.getDamageSources().inFire() : GearDamage.ember(owner, this), config.bombDamage());
			target.setOnFireFor(config.bombFireSeconds());
		}
		for (int i = 0; i < 24; i++) {
			double angle = i * Math.PI * 2 / 24;
			world.spawnParticles(ParticleTypes.FLAME, getX() + Math.cos(angle) * radius, getY() + 0.1, getZ() + Math.sin(angle) * radius,
					2, 0.05, 0.1, 0.05, 0.01);
		}
		world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.3, getZ(), 10, 0.6, 0.3, 0.6, 0.02);
		world.spawnParticles(ParticleTypes.WHITE_ASH, getX(), getY() + 0.5, getZ(), 30, 1.2, 0.5, 1.2, 0.02);
		world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.6f, 1.6f);
	}
}
