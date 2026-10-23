package com.embervoid.reach.gear;

import com.embervoid.fx.particle.EmberParticles;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;

/**
 * Royal Ash Rapier: a thrust whose full-charge hits on one target ramp ({@link RapierRamp}); Lunge. The ramp is seen:
 * every hit leaves a thread of amber motes from the hand to the wound, more and an ember or two as the ramp climbs,
 * and the peak rings out with a burst.
 */
public class RoyalAshRapierItem extends GearWeaponItem {
	public RoyalAshRapierItem(Settings settings, Spec spec) {
		super(settings, spec, GearAbilities.LUNGE);
	}

	@Override
	protected void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (!(attacker instanceof PlayerEntity player) || !(player.getWorld() instanceof ServerWorld world)) {
			return;
		}
		int before = RapierRamp.hitsOn(player, target);
		RapierRamp.onHit(player, target, GearReach.lastCharge(player));
		int hits = RapierRamp.hitsOn(player, target);
		GearConfig.Weapons config = GearConfig.get().weapons();
		int peak = config.rampPerHit() <= 0 ? 0 : (int) Math.ceil(config.rampMax() / config.rampPerHit());
		Vec3d wound = target.getPos().add(0, target.getHeight() * 0.6, 0);
		Vec3d hand = player.getEyePos().add(player.getRotationVector().multiply(0.4)).add(0, -0.35, 0);
		EmberParticles.line(world, EmberParticles.AMBER_MOTE, hand, wound, 6 + Math.min(hits, 8), 0.04);
		EmberParticles.throwAll(world, EmberParticles.BLOOD_DROP, wound, 3, new Vec3d(0, 0.1, 0), 0.12);
		if (hits > 0) {
			EmberParticles.spawn(world, EmberParticles.EMBER, wound, Math.min(hits, 6), 0.2, 0.04);
		}
		if (peak > 0 && hits >= peak && before < peak) {
			EmberParticles.burst(world, wound, 0.9f, WeaponFx.AMBER);
			world.playSound(null, target.getX(), target.getY(), target.getZ(), GearSounds.RAMP_PEAK, SoundCategory.PLAYERS, 0.8f, 1.2f);
		}
	}
}
