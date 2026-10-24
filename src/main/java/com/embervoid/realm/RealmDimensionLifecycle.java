package com.embervoid.realm;

import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Why our realm dimensions do not trigger vanilla's "experimental settings" warning.
 *
 * <p>In 1.20.1, {@code DimensionOptionsRegistryHolder.toConfig} marks the dimension registry experimental when it
 * holds anything but exactly overworld, nether and end, and marks every non-vanilla dimension entry experimental.
 * The world's lifecycle then makes the client ask for a backup on every load. {@code DimensionLifecycleMixin}
 * keeps vanilla's rule but leaves our realm dimensions out of it, so other mods' dimensions or a modified vanilla
 * dimension still warn.
 */
public final class RealmDimensionLifecycle {
	/** Overworld, nether, end. */
	public static final int VANILLA_DIMENSION_COUNT = 3;

	/** Ids of the realm dimensions. */
	public static Set<Identifier> realmDimensions() {
		return Realms.ALL.stream()
				.map(realm -> realm.world().getValue())
				.collect(Collectors.toUnmodifiableSet());
	}

	/** Vanilla's "exactly the vanilla dimensions" check, counting only dimensions that are not ours. */
	public static boolean onlyVanillaBesides(Collection<Identifier> dimensions, Set<Identifier> ours) {
		return dimensions.stream().filter(id -> !ours.contains(id)).distinct().count() == VANILLA_DIMENSION_COUNT;
	}

	private RealmDimensionLifecycle() {}
}
