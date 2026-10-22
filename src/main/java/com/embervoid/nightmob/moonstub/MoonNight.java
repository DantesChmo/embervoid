package com.embervoid.nightmob.moonstub;

/** Stub of {@code moon.api.MoonNight} until track-moon's API is on main; delete it then. */
public enum MoonNight {
	NONE,
	FULL_MOON,
	BLOOD_NEW_MOON;

	public boolean isEvent() {
		return this != NONE;
	}
}
