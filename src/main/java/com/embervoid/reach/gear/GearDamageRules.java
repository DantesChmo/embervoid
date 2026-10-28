package com.embervoid.reach.gear;

/**
 * How realm gear changes incoming damage, free of game classes (JUnit-tested); {@link GearDamage} reads the hit and
 * the gear into a {@link Hit}.
 */
public final class GearDamageRules {
	/**
	 * The gear numbers the rules use.
	 *
	 * @param fireReductionPerPiece   Smoldering Plate: share of fire damage each worn piece takes away
	 * @param huntsmanProjectile      Huntsman's Leathers set: projectile damage bonus
	 * @param wardenBlockingReduction Necropolis Warden set: share of damage taken away while blocking with a shield
	 * @param openerMultiplier        Huntsman's Crossbow: a bolt into a mob that is not targeting the shooter
	 * @param greavesSafeFallDamage   Huntsman's Greaves: fall damage taken away
	 */
	public record Factors(float fireReductionPerPiece, float huntsmanProjectile, float wardenBlockingReduction,
			float openerMultiplier, float greavesSafeFallDamage) {
		public static Factors of(GearConfig config) {
			return new Factors(config.smoldering().fireReductionPerPiece(), config.sets().huntsmanProjectile(),
					config.sets().wardenBlockingReduction(), config.crossbow().openerMultiplier(),
					config.uniques().greavesSafeFallDamage());
		}
	}

	/**
	 * One incoming hit as the gear sees it.
	 *
	 * @param fire               fire damage of any kind
	 * @param smolderingPieces   Smoldering Plate pieces the target wears
	 * @param huntsmanProjectile a projectile from a shooter in the full Huntsman's Leathers
	 * @param wardenBlocking     the target blocks with a shield in the full Necropolis Warden set
	 * @param attackerMultiplier what the attacking player brings (Kindling, the Rapier's ramp, the whip's tip); 1 for none
	 * @param opener             a Huntsman's bolt into a mob that is not targeting the shooter
	 * @param greavesFall        fall damage to a wearer of Huntsman's Greaves
	 */
	public record Hit(boolean fire, int smolderingPieces, boolean huntsmanProjectile, boolean wardenBlocking,
			float attackerMultiplier, boolean opener, boolean greavesFall) {}

	/** The damage {@code amount} becomes: every multiplier first, then the Greaves take their share off a fall. */
	public static float incoming(float amount, Hit hit, Factors factors) {
		if (hit.fire() && hit.smolderingPieces() > 0) {
			amount *= Math.max(0, 1 - hit.smolderingPieces() * factors.fireReductionPerPiece());
		}
		if (hit.huntsmanProjectile()) {
			amount *= 1 + factors.huntsmanProjectile();
		}
		if (hit.wardenBlocking()) {
			amount *= 1 - factors.wardenBlockingReduction();
		}
		amount *= hit.attackerMultiplier();
		if (hit.opener()) {
			amount *= factors.openerMultiplier();
		}
		if (hit.greavesFall()) {
			amount = Math.max(0, amount - factors.greavesSafeFallDamage());
		}
		return amount;
	}

	/**
	 * Whether the damage happens at all: the full Smoldering Plate does not burn ({@code burningTick}, not lava or fire
	 * blocks), and a fall that Huntsman's Greaves took down to nothing does not even hurt.
	 */
	public static boolean allows(boolean burningTick, boolean fullSmolderingSet, boolean greavesFall, float amount) {
		if (burningTick && fullSmolderingSet) {
			return false;
		}
		return !(greavesFall && amount <= 0);
	}

	private GearDamageRules() {}
}
