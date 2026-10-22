package com.embervoid.moon.api;

import com.embervoid.config.SectionReader;

/** Which moon night is on (DESIGN §3.3). */
public enum MoonNight {
	/** An ordinary night, or day. */
	NONE,
	/** Full moon (moon phase 0) of an event cycle: stronger mobs, the curse, Ashen Zombie / Ghost waves. */
	FULL_MOON,
	/** New moon (moon phase 4) of an event cycle: red sky, vampirism, the Vampires. */
	BLOOD_NEW_MOON;

	/** Lower-case id for commands, packets and logs. */
	public String id() {
		return SectionReader.enumId(this);
	}

	public boolean isEvent() {
		return this != NONE;
	}

	public static MoonNight byOrdinal(int ordinal) {
		MoonNight[] values = values();
		return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NONE;
	}
}
