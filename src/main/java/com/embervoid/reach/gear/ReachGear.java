package com.embervoid.reach.gear;

import com.embervoid.Embervoid;

/** Ashen Reach gear: materials, boss items, armor, weapons, consumables, loot. */
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
		WeaponFx.register();
		GearConsumables.register();
		FlaskEmberItem.register();
		GearLoot.register();
		GearCommands.register();
	}

	private ReachGear() {}
}
