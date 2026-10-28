package com.embervoid.boss;

import net.minecraft.util.Identifier;

/** How a boss id names its boss in the language files. */
public final class BossNames {
	/** Translation key of a boss's name: {@code boss.<namespace>.<path with dots>}. */
	public static String translationKey(Identifier bossId) {
		return "boss." + bossId.getNamespace() + "." + bossId.getPath().replace('/', '.');
	}

	private BossNames() {}
}
