package com.embervoid.boss;

/**
 * Something the arena does to the fighters for a while (fire pool, ash storm, falling cinders, rising water), owned
 * and ticked by a {@link BossFight} ({@link BossFight#addHazard}) and ended with it, so a reset leaves nothing behind.
 * Hazards are particles and damage; the only real blocks they may change go through {@link BossFight#blocks()}, which
 * puts them back when the fight ends.
 */
public interface ArenaHazard {
	/** One server tick of the hazard. Returns false when it is over; {@link #end} follows. */
	boolean tick(BossFight fight);

	/** The hazard is over or its fight ended (kill or reset): undo whatever it left. */
	default void end(BossFight fight) {
	}
}
