package com.embervoid.item;

import com.embervoid.config.EmbervoidConfig;

/** The Healing Flask's numbers from the config and a player's upgrades; {@link HealingFlaskItem} reads them per player. */
final class FlaskNumbers {
	/** Charge cap: base charges plus upgrades, never above {@code flask.maxCharges}. */
	static int maxCharges(EmbervoidConfig.Flask config, int chargeUpgrades) {
		return Math.min(config.baseCharges() + chargeUpgrades, config.maxCharges());
	}

	/** Charges a player can drink: what is stored, within 0 and the cap. */
	static int charges(int stored, int maxCharges) {
		return Math.max(0, Math.min(stored, maxCharges));
	}

	/**
	 * Health one full sip restores, in half-hearts: {@code flask.healAmount} plus {@code healPerUpgrade} per heal
	 * upgrade, times the product of the heal multipliers.
	 */
	static float healAmount(EmbervoidConfig.Flask config, int healUpgrades, float healPerUpgrade, double multiplier) {
		return (float) ((config.healAmount() + healUpgrades * healPerUpgrade) * multiplier);
	}

	private FlaskNumbers() {}
}
