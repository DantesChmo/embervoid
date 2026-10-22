package com.embervoid.reach.gear;

import com.embervoid.Embervoid;

/** Ashen Reach gear (PLAN milestone 11, track reach-gear): materials, boss items, armor, weapons, consumables, loot. */
public final class ReachGear {
	public static void register() {
		Embervoid.LOGGER.debug("Gear config section: {}", GearConfig.SECTION.name());
		GearItems.register();
		Bleeding.register();
		GearArmor.register();
		GearDamage.register();
		GearEffects.register();
		GearWeapons.register();
		GearReach.register();
		GearConsumables.register();
		FlaskEmberItem.register();
		GearCommands.register();
	}

	private ReachGear() {}
}
