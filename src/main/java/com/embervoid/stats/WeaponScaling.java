package com.embervoid.stats;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Placeholder STR/DEX scaling for vanilla weapons until the weapons doc (DESIGN §5.2, §8): swords DEX, axes STR,
 * tridents STR+DEX (melee and thrown), bows and crossbows DEX. Applied when damage is dealt, never as an
 * {@code attack_damage} modifier, so an empty hand or a non-weapon gets nothing and arrows are covered.
 *
 * <p>Arrows and thrown tridents remember the weapon they came from in a command tag set when they spawn
 * ({@code embervoid.scaling:<str>:<dex>}), so the bonus survives chunk unloads and does not depend on what the
 * player holds when it lands.
 */
public final class WeaponScaling {
	/**
	 * Share of the STR and DEX damage bonus a weapon gets (1 = the full bonus).
	 */
	public record Scaling(float strength, float dexterity) {
		public static final Scaling NONE = new Scaling(0, 0);
		public static final Scaling STRENGTH = new Scaling(1, 0);
		public static final Scaling DEXTERITY = new Scaling(0, 1);
		public static final Scaling HYBRID = new Scaling(0.5f, 0.5f);
	}

	private static final String TAG_PREFIX = "embervoid.scaling:";

	/** Extra rules for modded or future weapons, consulted first; a rule returns null to fall through. */
	private static final List<Function<ItemStack, Scaling>> MELEE_RULES = new ArrayList<>();
	private static final List<Function<ItemStack, Scaling>> RANGED_RULES = new ArrayList<>();

	public static void addMeleeRule(Function<ItemStack, Scaling> rule) {
		MELEE_RULES.add(rule);
	}

	/** Adds a rule for weapons that shoot or throw projectiles. */
	public static void addRangedRule(Function<ItemStack, Scaling> rule) {
		RANGED_RULES.add(rule);
	}

	public static Scaling melee(ItemStack stack) {
		Scaling custom = firstMatch(MELEE_RULES, stack);
		if (custom != null) {
			return custom;
		}
		if (stack.getItem() instanceof TridentItem) {
			return Scaling.HYBRID;
		}
		if (stack.getItem() instanceof SwordItem) {
			return Scaling.DEXTERITY;
		}
		if (stack.getItem() instanceof AxeItem) {
			return Scaling.STRENGTH;
		}
		return Scaling.NONE;
	}

	public static Scaling ranged(ItemStack stack) {
		Scaling custom = firstMatch(RANGED_RULES, stack);
		if (custom != null) {
			return custom;
		}
		if (stack.getItem() instanceof TridentItem) {
			return Scaling.HYBRID;
		}
		if (stack.getItem() instanceof RangedWeaponItem) {
			return Scaling.DEXTERITY;
		}
		return Scaling.NONE;
	}

	/** The scaling that applies to a hit by {@code attacker}: their main-hand weapon in melee, or the projectile's. */
	static Scaling forDamage(PlayerEntity attacker, DamageSource source) {
		Entity direct = source.getSource();
		if (direct == attacker && (source.isOf(DamageTypes.PLAYER_ATTACK))) {
			return melee(attacker.getMainHandStack());
		}
		if (direct instanceof PersistentProjectileEntity projectile) {
			return fromTag(projectile);
		}
		return Scaling.NONE;
	}

	@Nullable
	private static Scaling firstMatch(List<Function<ItemStack, Scaling>> rules, ItemStack stack) {
		for (Function<ItemStack, Scaling> rule : rules) {
			Scaling scaling = rule.apply(stack);
			if (scaling != null) {
				return scaling;
			}
		}
		return null;
	}

	private static Scaling fromTag(Entity projectile) {
		for (String tag : projectile.getCommandTags()) {
			Scaling scaling = parseTag(tag);
			if (scaling != null) {
				return scaling;
			}
		}
		return Scaling.NONE;
	}

	/** The command tag a projectile carries for {@code scaling} ({@code embervoid.scaling:<str>:<dex>}). */
	static String toTag(Scaling scaling) {
		return String.format(Locale.ROOT, "%s%s:%s", TAG_PREFIX, scaling.strength(), scaling.dexterity());
	}

	/** The scaling a command tag records; null for a tag of something else, {@link Scaling#NONE} for a broken one. */
	@Nullable
	static Scaling parseTag(String tag) {
		if (!tag.startsWith(TAG_PREFIX)) {
			return null;
		}
		String[] parts = tag.substring(TAG_PREFIX.length()).split(":");
		try {
			return new Scaling(Float.parseFloat(parts[0]), Float.parseFloat(parts[1]));
		} catch (RuntimeException e) {
			return Scaling.NONE;
		}
	}

	/** The weapon a player's new projectile came from: the bow being released, else the held ranged weapon. */
	private static ItemStack sourceWeapon(PlayerEntity player) {
		ItemStack active = player.getActiveItem();
		if (!active.isEmpty() && ranged(active) != Scaling.NONE) {
			return active;
		}
		if (ranged(player.getMainHandStack()) != Scaling.NONE) {
			return player.getMainHandStack();
		}
		return player.getOffHandStack();
	}

	static void register() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			// Only freshly fired projectiles; loaded ones already carry their tag (or never had one).
			if (!(entity instanceof PersistentProjectileEntity projectile) || projectile.age > 0
					|| !(projectile.getOwner() instanceof PlayerEntity player)) {
				return;
			}
			if (projectile.getCommandTags().stream().anyMatch(tag -> tag.startsWith(TAG_PREFIX))) {
				return;
			}
			Scaling scaling = ranged(sourceWeapon(player));
			if (scaling != Scaling.NONE) {
				projectile.addCommandTag(toTag(scaling));
			}
		});
	}

	private WeaponScaling() {}
}
