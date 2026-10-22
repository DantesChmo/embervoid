package com.embervoid.reliquary;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The Reliquary's slot kinds, in screen and save order. */
public final class SlotKinds {
	/** Artifacts: stat points (tag {@code embervoid:artifacts}). */
	public static final SlotKind ARTIFACT = SlotKind.of("artifact", "artifacts", 2);
	/** Rings: unique effects (tag {@code embervoid:rings}). */
	public static final SlotKind RING = SlotKind.of("ring", "rings", 2);

	public static final List<SlotKind> ALL = List.of(ARTIFACT, RING);

	@Nullable
	public static SlotKind byName(String name) {
		for (SlotKind kind : ALL) {
			if (kind.name().equals(name)) {
				return kind;
			}
		}
		return null;
	}

	private SlotKinds() {}
}
