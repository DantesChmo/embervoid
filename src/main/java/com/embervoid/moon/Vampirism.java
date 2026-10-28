package com.embervoid.moon;

import com.embervoid.boss.ArenaBoss;
import com.embervoid.moon.api.MoonNight;
import com.embervoid.moon.api.MoonNights;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.joml.Vector3f;

/**
 * Vampirism: on the blood new moon, an overworld hostile that damages a player heals
 * {@code moon.blood.vampirism} of the damage the player actually took, with red particles. Arrows and other
 * projectiles heal their owner. Bosses are left out.
 */
public final class Vampirism {
	private static final DustParticleEffect BLOOD = new DustParticleEffect(new Vector3f(0.75f, 0.02f, 0.02f), 1.2f);

	/** Called after a player took {@code dealt} damage (health and absorption lost). */
	public static void onPlayerDamaged(PlayerEntity player, DamageSource source, float dealt) {
		if (!(player.getWorld() instanceof ServerWorld world) || world.getRegistryKey() != World.OVERWORLD
				|| !(source.getAttacker() instanceof LivingEntity attacker) || !(attacker instanceof Monster)
				|| attacker instanceof ArenaBoss || !attacker.isAlive()
				|| MoonNights.current(world.getServer()) != MoonNight.BLOOD_NEW_MOON) {
			return;
		}
		float heal = dealt * MoonConfig.get().blood().vampirism();
		if (heal <= 0 || attacker.getWorld() != world) {
			return;
		}
		attacker.heal(heal);
		world.spawnParticles(BLOOD, attacker.getX(), attacker.getBodyY(0.6), attacker.getZ(),
				8, attacker.getWidth() * 0.4, attacker.getHeight() * 0.3, attacker.getWidth() * 0.4, 0.02);
	}

	private Vampirism() {}
}
