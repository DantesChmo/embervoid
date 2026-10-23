package com.embervoid.anim;

import com.embervoid.Embervoid;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The player attack animation sets, one per weapon kind (PLAN 17.1, DESIGN §6.1). Each clip is a playerAnimator file
 * {@code assets/embervoid/player_animation/<name>.json} whose {@code name} is the clip's name, keyed {@code ticks}
 * long. A swing is keyed to the weapon's attack cooldown: the blow lands in the first few ticks (the hit is already
 * dealt when the swing packet arrives) and the rest is the recovery until the next full-strength hit. The client
 * stretches a clip to the length the server sends, so a changed attack speed keeps the timing; the files are
 * written by {@code tools/player_anim_art.py}.
 */
public enum WeaponKind {
	/** Sanctum Longsword, speed 1.6: two swings in turn (forehand, backhand); Wide Cleave. */
	SWORD(List.of(new Clip("sword_swing_1", 13), new Clip("sword_swing_2", 13)), new Clip("sword_wide_cleave", 14)),
	/** Royal Ash Rapier, speed 2.2 (a 10-tick cooldown, as at 2.0): a thrust, no sweep; Lunge. */
	RAPIER(List.of(new Clip("rapier_thrust", 10)), new Clip("rapier_lunge", 12)),
	/** Ashen Scythe, speed 1.1: a wide sweep; Harvest (a full spin). */
	SCYTHE(List.of(new Clip("scythe_sweep", 19)), new Clip("scythe_harvest", 16)),
	/** Ashbearer's Mace, speed 0.9: an overhead blow; Ground Slam. */
	MACE(List.of(new Clip("mace_overhead", 23)), new Clip("mace_ground_slam", 18)),
	/** Warden's Scourge, speed 1.0: a crack; Lash (a pull). */
	WHIP(List.of(new Clip("whip_crack", 20)), new Clip("whip_lash", 14)),
	/** Huntsman's Crossbow: no swing of its own (left-click stays vanilla), {@link #CROSSBOW_LOAD} and {@link #CROSSBOW_FIRE}. */
	CROSSBOW(List.of(), null);

	/** Loading the Huntsman's Crossbow: its charge time (config {@code gear.crossbow.chargeTicks}). */
	public static final Clip CROSSBOW_LOAD = new Clip("crossbow_load", 32);
	/** The shot and the recoil. */
	public static final Clip CROSSBOW_FIRE = new Clip("crossbow_fire", 8);

	private final List<Clip> swings;
	@Nullable
	private final Clip special;

	WeaponKind(List<Clip> swings, @Nullable Clip special) {
		this.swings = swings;
		this.special = special;
	}

	/** The swings, played in turn. */
	public List<Clip> swings() {
		return swings;
	}

	/** The right-click special attack, or null for none. */
	@Nullable
	public Clip special() {
		return special;
	}

	/** Every clip of the kind. */
	public List<Clip> clips() {
		List<Clip> clips = new ArrayList<>(swings);
		if (special != null) {
			clips.add(special);
		}
		if (this == CROSSBOW) {
			clips.add(CROSSBOW_LOAD);
			clips.add(CROSSBOW_FIRE);
		}
		return clips;
	}

	/** Ticks until an attack at {@code attackSpeed} (attacks per second) is back to full strength. */
	public static int cooldownTicks(double attackSpeed) {
		return (int) Math.ceil(20 / attackSpeed - 1e-4);
	}

	/** One animation: its file and animation name, and the ticks it is keyed for. */
	public record Clip(String name, int ticks) {
		public Identifier id() {
			return Embervoid.id(name);
		}
	}
}
