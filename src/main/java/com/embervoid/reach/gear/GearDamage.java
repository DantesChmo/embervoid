package com.embervoid.reach.gear;

import com.embervoid.Embervoid;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
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
		if (source.isIn(DamageTypeTags.IS_FIRE)) {
			int pieces = GearArmor.pieces(target, GearArmor.SMOLDERING);
			if (pieces > 0) {
				amount *= Math.max(0, 1 - pieces * config.smoldering().fireReductionPerPiece());
			}
		}
		if (source.isIn(DamageTypeTags.IS_PROJECTILE) && source.getAttacker() instanceof LivingEntity shooter
				&& GearArmor.fullSet(shooter, GearArmor.HUNTSMAN)) {
			amount *= 1 + config.sets().huntsmanProjectile();
		}
		if (target.isBlocking() && GearArmor.fullSet(target, GearArmor.WARDEN)) {
			amount *= 1 - config.sets().wardenBlockingReduction();
		}
		if (source.isIn(DamageTypeTags.IS_FALL) && GearArmor.wears(target, GearArmor.HUNTSMAN_GREAVES_ITEM)) {
			amount = Math.max(0, amount - config.uniques().greavesSafeFallDamage());
		}
		return amount;
	}

	/**
	 * Whether the damage happens at all: the full Smoldering Plate does not burn (lava and fire blocks still hurt);
	 * a fall that Huntsman's Greaves took down to nothing does not even hurt. {@code amount} already went through
	 * {@link #modifyIncoming} (the event fires further into {@code damage}).
	 */
	static boolean allow(LivingEntity target, DamageSource source, float amount) {
		if (source.isOf(DamageTypes.ON_FIRE) && GearArmor.fullSet(target, GearArmor.SMOLDERING)) {
			return false;
		}
		return !(source.isIn(DamageTypeTags.IS_FALL) && amount <= 0 && GearArmor.wears(target, GearArmor.HUNTSMAN_GREAVES_ITEM));
	}

	static void register() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(GearDamage::allow);
	}

	private GearDamage() {}
}
