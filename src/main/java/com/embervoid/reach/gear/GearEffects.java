package com.embervoid.reach.gear;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/** Set bonuses and unique-piece effects that are not damage changes (those are in {@link GearDamage}). */
public final class GearEffects {
	private static final UUID REAPER_SPEED = UUID.fromString("5d0b1f0e-3b1a-4c55-9f0c-6a1f2b7e4a01");
	private static final UUID WARDEN_KNOCKBACK = UUID.fromString("5d0b1f0e-3b1a-4c55-9f0c-6a1f2b7e4a02");

	/** Re-checks the attribute set bonuses; runs whenever an entity's equipment changes (also once after it loads). */
	static void updateSetBonuses(LivingEntity entity) {
		GearConfig.Sets sets = GearConfig.get().sets();
		toggle(entity, EntityAttributes.GENERIC_MOVEMENT_SPEED, REAPER_SPEED, "reaper_set",
				GearArmor.fullSet(entity, GearArmor.REAPER), sets.reaperSpeed(), EntityAttributeModifier.Operation.MULTIPLY_BASE);
		toggle(entity, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, WARDEN_KNOCKBACK, "warden_set",
				GearArmor.fullSet(entity, GearArmor.WARDEN), sets.wardenKnockbackResist(), EntityAttributeModifier.Operation.ADDITION);
	}

	private static void toggle(LivingEntity entity, EntityAttribute attribute, UUID id, String name, boolean on, double amount,
			EntityAttributeModifier.Operation operation) {
		EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
		if (instance == null) {
			return;
		}
		boolean has = instance.getModifier(id) != null;
		if (on && !has) {
			instance.addTemporaryModifier(new EntityAttributeModifier(id, "embervoid:" + name, amount, operation));
		} else if (!on && has) {
			instance.removeModifier(id);
		}
	}

	/** Huntsman's Greaves: a burst of speed after every kill. */
	static void onKill(LivingEntity killer) {
		if (GearArmor.wears(killer, GearArmor.HUNTSMAN_GREAVES_ITEM)) {
			killer.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, GearConfig.get().uniques().greavesSpeedTicks(),
					0, false, false, true));
		}
	}

	/**
	 * Called from {@code GearLivingEntityMixin} after {@code LivingEntity.damage} hurt the entity. Furnace Heart
	 * Cuirass: below 30 % health a fire nova throws back and burns everything around, then the cuirass cools down (the
	 * vanilla item cooldown, shown on the chestplate).
	 */
	public static void afterDamage(LivingEntity entity) {
		if (!(entity instanceof ServerPlayerEntity player) || !player.isAlive()
				|| !GearArmor.wears(player, GearArmor.FURNACE_HEART_CUIRASS)) {
			return;
		}
		GearConfig.Uniques config = GearConfig.get().uniques();
		if (player.getHealth() >= player.getMaxHealth() * config.novaHealthFraction()
				|| player.getItemCooldownManager().isCoolingDown(GearArmor.FURNACE_HEART_CUIRASS)) {
			return;
		}
		player.getItemCooldownManager().set(GearArmor.FURNACE_HEART_CUIRASS, config.novaCooldownTicks());
		fireNova(player.getServerWorld(), player, config.novaRadius(), config.novaDamage());
	}

	/** Ember damage and knockback to every living entity within {@code radius} of the player, except teammates. */
	static void fireNova(ServerWorld world, ServerPlayerEntity player, float radius, float damage) {
		for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(radius),
				e -> e != player && e.isAlive() && !player.isTeammate(e) && e.squaredDistanceTo(player) <= radius * radius)) {
			target.damage(GearDamage.ember(player, null), damage);
			Vec3d away = target.getPos().subtract(player.getPos());
			target.takeKnockback(1.0, -away.x, -away.z);
			target.velocityModified = true;
		}
		for (int i = 0; i < 32; i++) {
			double angle = i * Math.PI * 2 / 32;
			world.spawnParticles(ParticleTypes.FLAME, player.getX() + Math.cos(angle) * radius, player.getY() + 0.3,
					player.getZ() + Math.sin(angle) * radius, 1, 0, 0.1, 0, 0.01);
		}
		world.spawnParticles(ParticleTypes.LAVA, player.getX(), player.getY() + 1, player.getZ(), 12, 0.5, 0.5, 0.5, 0);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_BLAZE_SHOOT,
				SoundCategory.PLAYERS, 1.2f, 0.6f);
	}

	static void register() {
		ServerEntityEvents.EQUIPMENT_CHANGE.register((entity, slot, from, to) -> {
			if (slot.getType() == EquipmentSlot.Type.ARMOR) {
				updateSetBonuses(entity);
			}
		});
		ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, killed) -> {
			if (killer instanceof LivingEntity living) {
				onKill(living);
			}
		});
	}

	private GearEffects() {}
}
