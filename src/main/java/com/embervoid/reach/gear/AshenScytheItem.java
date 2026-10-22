package com.embervoid.reach.gear;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;

/**
 * Ashen Scythe: every hit sweeps the mobs around the target at 75 % of its damage; a fully charged hit also sends an
 * ash wave forward (ember damage).
 */
public class AshenScytheItem extends GearWeaponItem {
	public AshenScytheItem(Settings settings, Spec spec) {
		super(settings, spec, GearAbilities.HARVEST);
	}

	@Override
	protected void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (!(attacker instanceof PlayerEntity player) || !(player.getWorld() instanceof ServerWorld world)) {
			return;
		}
		GearConfig.Weapons config = GearConfig.get().weapons();
		float charge = GearReach.lastCharge(player);
		float sweep = (float) player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)
				* WeaponRules.chargeFactor(charge) * config.sweepShare();
		float radius = config.sweepRadius();
		for (LivingEntity other : world.getEntitiesByClass(LivingEntity.class, target.getBoundingBox().expand(radius, 0.5, radius),
				e -> e != target && e != player && e.isAlive() && !player.isTeammate(e) && e.squaredDistanceTo(target) <= radius * radius)) {
			other.damage(world.getDamageSources().playerAttack(player), sweep);
		}
		world.spawnParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getBodyY(0.5), target.getZ(), 1, 0, 0, 0, 0);
		if (WeaponRules.fullyCharged(charge)) {
			ashWave(world, player, target, config);
		}
	}

	private static void ashWave(ServerWorld world, PlayerEntity player, LivingEntity struck, GearConfig.Weapons config) {
		Vec3d look = player.getRotationVector().multiply(1, 0, 1).normalize();
		Vec3d side = new Vec3d(-look.z, 0, look.x);
		double length = config.waveLength();
		for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(length, 2, length),
				e -> e != player && e != struck && e.isAlive() && !player.isTeammate(e))) {
			Vec3d offset = target.getPos().subtract(player.getPos());
			if (WeaponRules.inWave(offset.dotProduct(look), offset.dotProduct(side), length, config.waveWidth())
					&& Math.abs(offset.y) <= 2) {
				target.damage(GearDamage.ember(player, null), config.waveDamage());
			}
		}
		for (int i = 1; i <= (int) length * 2; i++) {
			Vec3d at = player.getPos().add(look.multiply(i * 0.5));
			world.spawnParticles(ParticleTypes.WHITE_ASH, at.x, at.y + 0.3, at.z, 6, side.x * 0.8, 0.2, side.z * 0.8, 0.01);
			world.spawnParticles(ParticleTypes.SMALL_FLAME, at.x, at.y + 0.2, at.z, 1, 0.3, 0.1, 0.3, 0.01);
		}
		world.playSound(null, player.getX(), player.getY(), player.getZ(), GearSounds.ASH_WAVE,
				SoundCategory.PLAYERS, 0.8f, 1.0f);
	}
}
