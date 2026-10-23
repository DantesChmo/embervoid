package com.embervoid.reach.gear;

import com.embervoid.item.ability.SpecialAbility;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
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
			for (int i = 0; i < 16; i++) {
				double angle = i * Math.PI * 2 / 16;
				world.spawnParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + Math.cos(angle) * radius * 0.6,
						player.getBodyY(0.5), player.getZ() + Math.sin(angle) * radius * 0.6, 1, 0, 0, 0, 0);
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
			Vec3d from = player.getEyePos();
			Vec3d step = target.getPos().add(0, target.getHeight() / 2, 0).subtract(from).multiply(1 / 8.0);
			for (int i = 1; i <= 8; i++) {
				Vec3d at = from.add(step.multiply(i));
				world.spawnParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 1, 0, 0, 0, 0);
			}
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
			world.spawnParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(), 6, 0.3, 0.1, 0.3, 0.02);
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
				world.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.6), target.getZ(), 10, 0.3, 0.3, 0.3, 0.2);
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
