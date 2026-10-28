package com.embervoid.reach.gear;

import com.embervoid.Embervoid;
import com.embervoid.stats.PlayerStats;
import com.embervoid.stats.Stat;
import com.embervoid.stats.StatCurve;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.DamageTypeTags;
import org.jetbrains.annotations.Nullable;

/** Damage changes from realm gear (armor piece and set effects) and the realm's ember damage. */
public final class GearDamage {
	/** Damage type {@code embervoid:ember}: the realm's flame (cuirass nova, ash waves, Ash Bombs). Armor applies. */
	public static final RegistryKey<DamageType> EMBER = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Embervoid.id("ember"));

	/** Ember damage dealt by {@code attacker} (directly, or through {@code source} such as a thrown bomb). */
	public static DamageSource ember(Entity attacker, @Nullable Entity source) {
		return new DamageSource(attacker.getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(EMBER),
				source == null ? attacker : source, attacker);
	}

	/**
	 * Called from {@code GearLivingEntityMixin} at the start of {@code LivingEntity.damage}, before armor: scales the
	 * incoming damage by what the target and the attacker wear.
	 */
	public static float modifyIncoming(LivingEntity target, DamageSource source, float amount) {
		if (target.getWorld().isClient()) {
			return amount;
		}
		GearConfig config = GearConfig.get();
		boolean fire = source.isIn(DamageTypeTags.IS_FIRE);
		GearDamageRules.Hit hit = new GearDamageRules.Hit(
				fire,
				fire ? GearArmor.pieces(target, GearArmor.SMOLDERING) : 0,
				source.isIn(DamageTypeTags.IS_PROJECTILE) && source.getAttacker() instanceof LivingEntity shooter
						&& GearArmor.fullSet(shooter, GearArmor.HUNTSMAN),
				target.isBlocking() && GearArmor.fullSet(target, GearArmor.WARDEN),
				attackerMultiplier(target, source, config),
				HuntsmansCrossbowItem.isOpener(target, source),
				greavesFall(target, source));
		return GearDamageRules.incoming(amount, hit, GearDamageRules.Factors.of(config));
	}

	/** What the attacking player brings to the hit: Kindling, the Rapier's ramp, the whip's tip. */
	private static float attackerMultiplier(LivingEntity target, DamageSource source, GearConfig config) {
		if (!(source.getAttacker() instanceof PlayerEntity player)) {
			return 1;
		}
		float multiplier = kindling(player, source, config.weapons().kindlingPerPoint());
		if (source.isOf(DamageTypes.PLAYER_ATTACK) && source.getSource() == player && RapierRamp.holdsRapier(player)) {
			multiplier *= RapierRamp.multiplier(player, target);
		}
		if (source.isOf(DamageTypes.PLAYER_ATTACK) && source.getSource() == player && WardensScourgeItem.tipHit(player, target)) {
			multiplier *= config.weapons().whipTipMultiplier();
		}
		return multiplier;
	}

	private static boolean greavesFall(LivingEntity target, DamageSource source) {
		return source.isIn(DamageTypeTags.IS_FALL) && GearArmor.wears(target, GearArmor.HUNTSMAN_GREAVES_ITEM);
	}

	/**
	 * Kindling: ember damage dealt by a player scales with KIN in full; a melee hit with a realm weapon that has an
	 * ember part (the Rapier) scales with KIN for that part.
	 */
	private static float kindling(PlayerEntity player, DamageSource source, float perPoint) {
		float share = 0;
		if (source.isOf(EMBER)) {
			share = 1;
		} else if (source.isOf(DamageTypes.PLAYER_ATTACK) && source.getSource() == player
				&& player.getMainHandStack().getItem() instanceof GearWeaponItem weapon) {
			share = weapon.spec().kindlingShare();
		}
		if (share == 0) {
			return 1;
		}
		double points = StatCurve.effectivePoints(PlayerStats.effective(player, Stat.KINDLING));
		return WeaponRules.kindlingMultiplier(points, perPoint, share);
	}

	/**
	 * Whether the damage happens at all: the full Smoldering Plate does not burn (lava and fire blocks still hurt);
	 * a fall that Huntsman's Greaves took down to nothing does not even hurt. {@code amount} already went through
	 * {@link #modifyIncoming} (the event fires further into {@code damage}).
	 */
	static boolean allow(LivingEntity target, DamageSource source, float amount) {
		boolean burningTick = source.isOf(DamageTypes.ON_FIRE);
		return GearDamageRules.allows(burningTick, burningTick && GearArmor.fullSet(target, GearArmor.SMOLDERING),
				greavesFall(target, source), amount);
	}

	static void register() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(GearDamage::allow);
	}

	private GearDamage() {}
}
