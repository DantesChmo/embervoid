package com.embervoid.boss;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

public final class BossHits {
	/**
	 * An unblockable hit that breaks the shield (DESIGN §4.5, Crypt Keeper's overhead): a blocking player's shield goes
	 * on cooldown as if hit by an axe while sprinting (always), then the hit lands in full. Returns whether it hurt.
	 */
	public static boolean shieldBreaking(LivingEntity attacker, LivingEntity target, float amount) {
		if (target instanceof PlayerEntity player && player.isBlocking()) {
			player.disableShield(true);
		}
		return target.damage(attacker.getDamageSources().mobAttack(attacker), amount);
	}

	/**
	 * A damage source of any type with the boss as attacker (fire, magic, ...), so its level scales the damage and
	 * deaths are blamed on it.
	 */
	public static DamageSource source(Entity attacker, RegistryKey<DamageType> type) {
		return new DamageSource(attacker.getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(type), attacker);
	}

	private BossHits() {}
}
