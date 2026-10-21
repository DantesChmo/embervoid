package com.embervoid.stats;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** The seven character stats (DESIGN §5.2). Every character starts with 10 in each. */
public enum Stat {
	VITALITY("vit"),
	ENDURANCE("end"),
	STRENGTH("str"),
	DEXTERITY("dex"),
	KINDLING("kin"),
	RESILIENCE("res"),
	FORTUNE("for");

	public static final int COUNT = values().length;
	/** Starting value of every stat; at this value a stat gives exactly vanilla numbers. */
	public static final int BASE = 10;
	/** Lowest value a stat can be set to (debug); leveling only raises stats. */
	public static final int MIN = 1;

	private final String shortName;

	Stat(String shortName) {
		this.shortName = shortName;
	}

	/** Lower-case id used in commands, NBT and translation keys: {@code vitality}. */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** Lower-case three-letter id: {@code vit}. */
	public String shortId() {
		return shortName;
	}

	/** {@code embervoid.stat.<id>}: "Vitality". */
	public Text displayName() {
		return Text.translatable("embervoid.stat." + id());
	}

	/** {@code embervoid.stat.<id>.short}: "VIT". */
	public Text shortName() {
		return Text.translatable("embervoid.stat." + id() + ".short");
	}

	/** Parses {@link #id} or {@link #shortId}, ignoring case. */
	@Nullable
	public static Stat byId(String id) {
		for (Stat stat : values()) {
			if (stat.id().equalsIgnoreCase(id) || stat.shortName.equalsIgnoreCase(id)) {
				return stat;
			}
		}
		return null;
	}
}
