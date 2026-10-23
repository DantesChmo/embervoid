package com.embervoid.reach.gear;

import com.embervoid.fx.CombatFx;
import com.embervoid.fx.particle.EmberParticles;
import com.embervoid.item.ability.SpecialAbility;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;

/** The realm weapons' right-click special attacks (DESIGN §4.5, contract §8.1). Numbers in {@code gear.weapons}. */
public final class GearAbilities {
	/** Ashen Scythe: a 360° spin hitting everything around at the weapon's damage; stamina back per enemy caught. */
	public static final SpecialAbility HARVEST = new SpecialAbility() {
		@Override
		public String id() {
			return "harvest";
		}

		@Override
		public float staminaCost() {
			return GearConfig.get().weapons().harvestStamina();
		}

		@Override
		public int cooldownTicks() {
			return GearConfig.get().weapons().harvestCooldown();
		}

		@Override
		public void perform(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
			float radius = GearConfig.get().weapons().harvestRadius();
			float damage = (float) player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
			// The spin is the swing: the swing packet that follows the use is no reach attack (the Scythe reaches 3.5).
			GearReach.noteAttack(player);
			int caught = 0;
			for (LivingEntity target : around(world, player, radius)) {
				if (target.damage(world.getDamageSources().playerAttack(player), damage)) {
					caught++;
				}
				Vec3d away = target.getPos().subtract(player.getPos());
				target.takeKnockback(0.5, -away.x, -away.z);
			}
			AshenScytheItem.refundStamina(player, caught);
			// The spin seen: a ring of ash thrown out, a shockwave on the ground, souls drawn in from what it caught.
			Vec3d feet = player.getPos();
			EmberParticles.shockwave(world, feet.add(0, 0.05, 0), radius, WeaponFx.ASH);
			EmberParticles.ringOut(world, EmberParticles.ASH_FLAKE, feet.add(0, 1.0, 0), 28, 0.3, 0.06);
			EmberParticles.ringOut(world, EmberParticles.EMBER, feet.add(0, 0.9, 0), 10, 0.25, 0.08);
			for (LivingEntity target : around(world, player, radius)) {
				AshenScytheItem.reaped(world, target, 6);
			}
			if (caught > 0) {
				EmberParticles.souls(world, feet.add(0, 1.2, 0).add(player.getRotationVector().multiply(1.5)),
						player.getEyePos().add(0, -0.4, 0), Math.min(6, caught * 2));
			}
			world.playSound(null, player.getX(), player.getY(), player.getZ(), GearSounds.HARVEST,
					SoundCategory.PLAYERS, 1.2f, 1.0f);
		}
	};

	/** Warden's Scourge: the whip pulls the first mob within its reach toward the player. */
	public static final SpecialAbility LASH = new SpecialAbility() {
		@Override
		public String id() {
			return "lash";
		}

		@Override
		public float staminaCost() {
			return GearConfig.get().weapons().lashStamina();
		}

		@Override
		public int cooldownTicks() {
			return GearConfig.get().weapons().lashCooldown();
		}

		@Override
		public void perform(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
			world.playSound(null, player.getX(), player.getY(), player.getZ(), GearSounds.LASH,
					SoundCategory.PLAYERS, 1.0f, 1.0f);
			// The cast is the swing: the swing packet that follows the use is no crack.
			GearReach.noteAttack(player);
			double reach = stack.getItem() instanceof GearWeaponItem weapon && weapon.spec().reach() > 0
					? weapon.spec().reach() : GearReach.VANILLA_REACH;
			LivingEntity target = GearReach.raycast(player, reach);
			if (target == null) {
				return;
			}
			double resist = Math.min(1, Math.max(0, target.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE)));
			Vec3d toward = player.getPos().subtract(target.getPos()).multiply(1, 0, 1);
			double pull = Math.min(GearConfig.get().weapons().lashPull(), Math.max(0, toward.length() - 1.5));
			Vec3d velocity = toward.normalize().multiply(WeaponRules.slideSpeed(pull) * (1 - resist));
			target.setVelocity(target.getVelocity().add(velocity.x, 0.3 * (1 - resist), velocity.z));
			target.velocityModified = true;
			// The lash runs hot along the pull, and the mob leaves ash as it is dragged.
			Vec3d from = player.getEyePos().add(0, -0.3, 0);
			Vec3d grip = target.getPos().add(0, target.getHeight() / 2, 0);
			EmberParticles.line(world, EmberParticles.WHIP_LINE, from, grip, 14, 0.05);
			EmberParticles.spawn(world, EmberParticles.CINDER_SPARK, grip, 6, 0.2, 0.15);
			EmberParticles.throwAll(world, EmberParticles.ASH_FLAKE, grip, 10, velocity.multiply(-0.3), 0.08);
			EmberParticles.burst(world, grip, 0.6f, 0xFFE8B0);
		}
	};

	/** Royal Ash Rapier: a dash forward ending in a thrust that bleeds twice and burns. */
	public static final SpecialAbility LUNGE = new SpecialAbility() {
		@Override
		public String id() {
			return "lunge";
		}

		@Override
		public float staminaCost() {
			return GearConfig.get().weapons().lungeStamina();
		}

		@Override
		public int cooldownTicks() {
			return GearConfig.get().weapons().lungeCooldown();
		}

		@Override
		public void perform(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
			float dash = GearConfig.get().weapons().lungeDash();
			Vec3d look = player.getRotationVector().multiply(1, 0, 1).normalize();
			// The thrust reaches the end of the dash plus the rapier's reach; it lands at once.
			double reach = stack.getItem() instanceof GearWeaponItem weapon && weapon.spec().reach() > 0
					? weapon.spec().reach() : GearReach.VANILLA_REACH;
			LivingEntity target = GearReach.raycast(player, dash + reach);
			// The thrust is the swing: the swing packet that follows is neither a second hit nor a miss for the ramp.
			GearReach.noteAttack(player);
			player.setVelocity(look.multiply(WeaponRules.slideSpeed(dash)).add(0, 0.1, 0));
			player.velocityModified = true;
			// The dash seen: an amber streak and embers left along the way, soot kicked up at the start.
			Vec3d start = player.getPos();
			EmberParticles.spawn(world, EmberParticles.SOOT, start.add(0, 0.2, 0), 5, 0.3, 0.02);
			WeaponFx.unfold(world, 4, step -> {
				Vec3d at = start.add(look.multiply(dash * (step + 1) / 4.0)).add(0, 1.0, 0);
				EmberParticles.spawn(world, EmberParticles.AMBER_MOTE, at, 4, 0.25, 0.02);
				EmberParticles.spawn(world, EmberParticles.EMBER, at, 2, 0.2, 0.03);
			});
			world.playSound(null, player.getX(), player.getY(), player.getZ(), GearSounds.LUNGE,
					SoundCategory.PLAYERS, 1.0f, 1.0f);
			if (target == null) {
				return;
			}
			float damage = (float) player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
			if (target.damage(world.getDamageSources().playerAttack(player), damage)) {
				int bleed = stack.getItem() instanceof GearWeaponItem weapon ? weapon.spec().bleed() : 1;
				Bleeding.apply(target, 2 * Math.max(1, bleed), player);
				if (stack.getItem() instanceof GearWeaponItem weapon && weapon.spec().igniteSeconds() > 0) {
					target.setOnFireFor(weapon.spec().igniteSeconds());
				}
				Vec3d struck = target.getPos().add(0, target.getHeight() * 0.6, 0);
				EmberParticles.burst(world, struck, 0.9f, WeaponFx.AMBER);
				EmberParticles.throwAll(world, EmberParticles.BLOOD_DROP, struck, 8, new Vec3d(0, 0.15, 0), 0.2);
				EmberParticles.spawn(world, EmberParticles.EMBER, struck, 6, 0.3, 0.05);
				CombatFx.shake(player, 0.3f, 5);
			}
		}
	};

	static Iterable<LivingEntity> around(ServerWorld world, ServerPlayerEntity player, double radius) {
		return world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(radius, 1, radius),
				e -> e != player && e.isAlive() && !player.isTeammate(e)
						&& e.getPos().subtract(player.getPos()).multiply(1, 0, 1).lengthSquared() <= radius * radius);
	}

	private GearAbilities() {}
}
