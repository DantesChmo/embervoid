package com.embervoid.sanctum.gear;

import com.embervoid.item.ability.SpecialAbility;
import com.embervoid.reach.gear.GearWeaponItem;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * The Sanctum weapons' right-click special attacks (DESIGN §10.10, contract §8.1). Numbers in {@code sanctumGear}.
 * Every hit is a player attack through {@code damage()} on a target the player can see (no hits through walls).
 */
public final class SanctumAbilities {
	/** Sanctum Longsword: one wide swing hitting everything in the arc in front at 1.25× the weapon's hit. */
	public static final SpecialAbility WIDE_CLEAVE = new SpecialAbility() {
		@Override
		public String id() {
			return "wide_cleave";
		}

		@Override
		public float staminaCost() {
			return SanctumGearConfig.get().cleaveStamina();
		}

		@Override
		public int cooldownTicks() {
			return SanctumGearConfig.get().cleaveCooldown();
		}

		@Override
		public void perform(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
			SanctumGearConfig config = SanctumGearConfig.get();
			float damage = weaponHit(player, stack) * config.cleaveMultiplier();
			Vec3d look = player.getRotationVector().multiply(1, 0, 1).normalize();
			Vec3d side = new Vec3d(-look.z, 0, look.x);
			for (LivingEntity target : targets(world, player, config.cleaveRange())) {
				Vec3d offset = target.getPos().subtract(player.getPos());
				if (SanctumWeaponRules.inArc(offset.dotProduct(look), offset.dotProduct(side), config.cleaveRange(), config.cleaveArc())) {
					target.damage(world.getDamageSources().playerAttack(player), damage);
				}
			}
			for (int i = -2; i <= 2; i++) {
				Vec3d at = player.getPos().add(look.multiply(1.6)).add(side.multiply(i * 0.6)).add(0, player.getHeight() * 0.6, 0);
				world.spawnParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 1, 0, 0, 0, 0);
			}
			world.playSound(null, player.getX(), player.getY(), player.getZ(), SanctumGearSounds.WIDE_CLEAVE,
					SoundCategory.PLAYERS, 1.2f, 0.8f);
		}
	};

	/** Ashbearer's Mace: a slam into the ground hitting everything around at 0.75×, knocking back and slowing. */
	public static final SpecialAbility GROUND_SLAM = new SpecialAbility() {
		@Override
		public String id() {
			return "ground_slam";
		}

		@Override
		public float staminaCost() {
			return SanctumGearConfig.get().slamStamina();
		}

		@Override
		public int cooldownTicks() {
			return SanctumGearConfig.get().slamCooldown();
		}

		@Override
		public void perform(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
			SanctumGearConfig config = SanctumGearConfig.get();
			float damage = weaponHit(player, stack) * config.slamMultiplier();
			for (LivingEntity target : targets(world, player, config.slamRadius())) {
				Vec3d away = target.getPos().subtract(player.getPos());
				if (!SanctumWeaponRules.inRing(away.x, away.z, config.slamRadius())
						|| !target.damage(world.getDamageSources().playerAttack(player), damage)) {
					continue;
				}
				target.takeKnockback(config.slamKnockback(), -away.x, -away.z);
				target.velocityModified = true;
				if (config.slamSlownessTicks() > 0) {
					target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, config.slamSlownessTicks(),
							config.slamSlownessAmplifier()), player);
				}
			}
			BlockState ground = world.getBlockState(BlockPos.ofFloored(player.getPos()).down());
			if (!ground.isAir()) {
				for (int i = 0; i < 24; i++) {
					double angle = i * Math.PI * 2 / 24;
					double r = config.slamRadius() * 0.8;
					world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, ground), player.getX() + Math.cos(angle) * r,
							player.getY() + 0.1, player.getZ() + Math.sin(angle) * r, 3, 0.2, 0.05, 0.2, 0.1);
				}
			}
			world.spawnParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.2, player.getZ(), 1, 0, 0, 0, 0);
			world.playSound(null, player.getX(), player.getY(), player.getZ(), SanctumGearSounds.GROUND_SLAM,
					SoundCategory.PLAYERS, 0.5f, 1.4f);
		}
	};

	/** The weapon's own hit: the attack damage attribute when held in the main hand, else the weapon's damage. */
	static float weaponHit(ServerPlayerEntity player, ItemStack stack) {
		if (player.getMainHandStack() == stack) {
			return (float) player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
		}
		return stack.getItem() instanceof GearWeaponItem weapon ? weapon.spec().damage() : 1;
	}

	/**
	 * Living things within {@code radius} horizontally and a block up or down that the player can see: no hits through
	 * walls (DESIGN §8.1). Marker armor stands are skipped, as vanilla's sweep skips them.
	 */
	static List<LivingEntity> targets(ServerWorld world, ServerPlayerEntity player, double radius) {
		return world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(radius, 1, radius),
				e -> e != player && e.isAlive() && !e.isSpectator() && !player.isTeammate(e)
						&& !(e instanceof ArmorStandEntity stand && stand.isMarker()) && player.canSee(e));
	}

	private SanctumAbilities() {}
}
