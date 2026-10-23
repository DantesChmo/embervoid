package com.embervoid.sanctum.gear;

import com.embervoid.Embervoid;
import com.embervoid.anim.AttackAnimations;
import com.embervoid.anim.WeaponKind;

/** The Sanctum weapons (PLAN L1): one call from {@code Sanctum.register}. */
public final class SanctumGear {
	public static void register() {
		Embervoid.LOGGER.debug("Sanctum gear: {} weapons, config section {}", SanctumWeapons.ALL.size(),
				SanctumGearConfig.SECTION.name());
		AttackAnimations.register(SanctumWeapons.SANCTUM_LONGSWORD, WeaponKind.SWORD);
		AttackAnimations.register(SanctumWeapons.ASHBEARERS_MACE, WeaponKind.MACE);
	}

	private SanctumGear() {}
}
