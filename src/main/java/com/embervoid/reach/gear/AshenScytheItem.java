package com.embervoid.reach.gear;

import com.embervoid.fx.particle.EmberParticles;
import com.embervoid.player.StaminaManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;

/**
 * Ashen Scythe: every hit sweeps the mobs around the target at 75 % of its damage; a fully charged hit also sends an
 * ash wave forward (ember damage). Each enemy a sweep or Harvest catches gives stamina back (DESIGN §6.1).
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
		float sweep = WeaponRules.sweepDamage(player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE), charge, config.sweepShare());
		float radius = config.sweepRadius();
		int caught = 0;
		for (LivingEntity other : world.getEntitiesByClass(LivingEntity.class, target.getBoundingBox().expand(radius, 0.5, radius),
				e -> e != target && e != player && e.isAlive() && !player.isTeammate(e) && WeaponRules.inSweep(e.squaredDistanceTo(target), radius))) {
			if (other.damage(world.getDamageSources().playerAttack(player), sweep)) {
				caught++;
				reaped(world, other, 5);
			}
		}
		if (player instanceof ServerPlayerEntity serverPlayer) {
			refundStamina(serverPlayer, caught);
		}
		reaped(world, target, 10);
		if (caught > 0) {
			// The stamina the sweep gives back, seen: ash drawn from what it caught into the reaper.
			EmberParticles.souls(world, target.getPos().add(0, target.getHeight() * 0.5, 0), player.getEyePos().add(0, -0.4, 0), Math.min(4, caught));
		}
		if (WeaponRules.fullyCharged(charge)) {
			ashWave(world, player, target, config);
		}
	}

	/** The blade through a mob: ash flakes off it, a little soot. */
	static void reaped(ServerWorld world, LivingEntity target, int flakes) {
		Vec3d at = target.getPos().add(0, target.getHeight() * 0.55, 0);
		EmberParticles.spawn(world, EmberParticles.ASH_FLAKE, at, flakes, 0.35, 0.06);
		EmberParticles.spawn(world, EmberParticles.SOOT, at, 1, 0.1, 0.01);
	}

	/** {@code gear.weapons.sweepStamina} back for each of {@code caught} enemies, up to the pool. */
	static void refundStamina(ServerPlayerEntity player, int caught) {
		if (caught > 0) {
			StaminaManager.set(player, StaminaManager.get(player) + WeaponRules.staminaRefund(caught, GearConfig.get().weapons().sweepStamina()));
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
		// The wave seen: a front of ash and embers rolling forward over eight ticks, a scorch where it dies.
		Vec3d start = player.getPos();
		double halfWidth = config.waveWidth() / 2;
		int ticks = 8;
		EmberParticles.shockwave(world, start.add(0, 0.05, 0), 1.6f, WeaponFx.ASH);
		WeaponFx.unfold(world, ticks, step -> {
			double along = length * (step + 1) / ticks;
			Vec3d at = start.add(look.multiply(along));
			for (int i = -3; i <= 3; i++) {
				Vec3d p = at.add(side.multiply(i * halfWidth / 3));
				world.spawnParticles(EmberParticles.ASH_FLAKE, p.x, p.y + 0.4, p.z, 3, 0.2, 0.3, 0.2, 0.02);
				if ((i + step) % 2 == 0) {
					world.spawnParticles(EmberParticles.EMBER, p.x, p.y + 0.3, p.z, 1, 0.15, 0.2, 0.15, 0.02);
				}
			}
			world.spawnParticles(EmberParticles.SOOT, at.x, at.y + 0.5, at.z, 2, halfWidth * 0.6, 0.2, halfWidth * 0.6, 0.01);
			if (step == ticks - 1) {
				EmberParticles.scorch(world, at.add(0, 0.02, 0), 1.4f);
			}
		});
		world.playSound(null, player.getX(), player.getY(), player.getZ(), GearSounds.ASH_WAVE,
				SoundCategory.PLAYERS, 0.8f, 1.0f);
	}
}
