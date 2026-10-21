package com.embervoid.stats;

import com.embervoid.Embervoid;
import com.embervoid.item.HealingFlaskItem;
import com.embervoid.player.StaminaManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * Applies stat effects (DESIGN §5.2, §10.10). Vanilla attributes get modifiers with fixed UUIDs, so other mods that
 * read attributes see them: VIT → max health, DEX → attack speed, END → knockback resistance, RES → armor toughness,
 * FOR → luck. Our own values: END → stamina max and regen ({@link StaminaManager}), KIN → flask healing, RES → fire
 * damage reduction, STR/DEX → weapon damage ({@link WeaponScaling}), the last two applied when damage is dealt.
 *
 * <p>Modifiers are temporary (not saved): they are recomputed on join, respawn and every stat change, so the
 * saved stats are the only source of truth. Because of that, vanilla clamps the saved health to 20 on load, before
 * the modifiers exist; the health read from the save is restored after applying them.
 */
public final class StatEffects {
	private static final UUID MAX_HEALTH = UUID.fromString("5d3f7c0e-6a3b-4a4e-9a51-0c1e5b7d2a01");
	private static final UUID ATTACK_SPEED = UUID.fromString("5d3f7c0e-6a3b-4a4e-9a51-0c1e5b7d2a02");
	private static final UUID KNOCKBACK_RESISTANCE = UUID.fromString("5d3f7c0e-6a3b-4a4e-9a51-0c1e5b7d2a03");
	private static final UUID ARMOR_TOUGHNESS = UUID.fromString("5d3f7c0e-6a3b-4a4e-9a51-0c1e5b7d2a04");
	private static final UUID LUCK = UUID.fromString("5d3f7c0e-6a3b-4a4e-9a51-0c1e5b7d2a05");

	/** Recomputes the player's attribute modifiers and stamina from their stats. Keeps health within the new max. */
	public static void apply(ServerPlayerEntity player) {
		DerivedStats derived = PlayerStats.derived(player);
		set(player, EntityAttributes.GENERIC_MAX_HEALTH, MAX_HEALTH, "vitality",
				derived.maxHealth() - DerivedStats.BASE_HEALTH, EntityAttributeModifier.Operation.ADDITION);
		set(player, EntityAttributes.GENERIC_ATTACK_SPEED, ATTACK_SPEED, "dexterity",
				derived.attackSpeedBonus(), EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
		set(player, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE, "endurance",
				derived.knockbackResistance(), EntityAttributeModifier.Operation.ADDITION);
		set(player, EntityAttributes.GENERIC_ARMOR_TOUGHNESS, ARMOR_TOUGHNESS, "resilience",
				derived.armorToughness(), EntityAttributeModifier.Operation.ADDITION);
		set(player, EntityAttributes.GENERIC_LUCK, LUCK, "fortune",
				derived.luck(), EntityAttributeModifier.Operation.ADDITION);
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
		// The stamina pool may have changed; clamp and resend it.
		StaminaManager.set(player, StaminaManager.get(player));
	}

	private static void set(LivingEntity entity, EntityAttribute attribute, UUID id, String name, double value,
			EntityAttributeModifier.Operation operation) {
		EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
		if (instance == null) {
			return;
		}
		instance.removeModifier(id);
		if (value != 0) {
			instance.addTemporaryModifier(new EntityAttributeModifier(id, Embervoid.MOD_ID + ":" + name, value, operation));
		}
	}

	/**
	 * Stat changes to damage, called from {@code LivingEntityMixin} at the start of {@code LivingEntity.damage}:
	 * the attacker's weapon scaling, then the target's fire reduction.
	 */
	public static float modifyDamage(LivingEntity target, DamageSource source, float amount) {
		if (target.getWorld().isClient) {
			return amount;
		}
		if (source.getAttacker() instanceof ServerPlayerEntity attacker) {
			WeaponScaling.Scaling scaling = WeaponScaling.forDamage(attacker, source);
			if (scaling != WeaponScaling.Scaling.NONE) {
				DerivedStats derived = PlayerStats.derived(attacker);
				float bonus = scaling.strength() * derived.strengthDamageBonus() + scaling.dexterity() * derived.dexterityDamageBonus();
				amount *= Math.max(0, 1 + bonus);
			}
		}
		if (target instanceof ServerPlayerEntity player && source.isIn(DamageTypeTags.IS_FIRE)) {
			amount *= 1 - PlayerStats.derived(player).fireDamageReduction();
		}
		return amount;
	}

	public static void register() {
		StatsConfig.register();
		HealingFlaskItem.setHealMultiplier(player -> PlayerStats.derived(player).flaskHealMultiplier());
		WeaponScaling.register();

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			apply(player);
			float saved = ((LoadedHealth) player).embervoid$takeLoadedHealth();
			if (saved > 0) {
				player.setHealth(saved);
			}
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			apply(newPlayer);
			// After death: full health at the new max. Leaving the End: vanilla copied the old health but clamped
			// it to 20 before our modifiers existed on the new entity.
			newPlayer.setHealth(alive ? oldPlayer.getHealth() : newPlayer.getMaxHealth());
		});
	}

	private StatEffects() {}
}
