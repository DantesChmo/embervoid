package com.embervoid.realm;

import com.embervoid.Embervoid;
import com.embervoid.progress.ProgressManager;
import com.embervoid.realm.RealmDefinition.LevelBand;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** All realms, in unlock order. */
public final class Realms {
	public static final RealmDefinition ASHEN_REACH = new RealmDefinition(
			ProgressManager.REALM_1,
			RegistryKey.of(RegistryKeys.WORLD, Embervoid.id("ashen_reach")),
			RegistryKey.of(RegistryKeys.WORLD, Embervoid.id("ashen_reach_depths")),
			List.of(Embervoid.id("ashen_reach/zone_a"), Embervoid.id("ashen_reach/zone_b"), Embervoid.id("ashen_reach/zone_c")),
			Embervoid.id("ashen_reach/final"),
			// DESIGN §5.4: zones 15–25 with a band each, final zone 25–30. Zone bosses sit at the top of their band.
			List.of(new LevelBand(15, 18), new LevelBand(18, 21), new LevelBand(21, 25)),
			new LevelBand(15, 15),
			new LevelBand(25, 30),
			1200, 400);

	public static final List<RealmDefinition> ALL = List.of(ASHEN_REACH);

	/** The realm the world belongs to (open zones or final zone), or null outside realms. */
	@Nullable
	public static RealmDefinition byWorld(RegistryKey<World> key) {
		for (RealmDefinition realm : ALL) {
			if (realm.contains(key)) {
				return realm;
			}
		}
		return null;
	}

	@Nullable
	public static RealmDefinition byId(Identifier id) {
		for (RealmDefinition realm : ALL) {
			if (realm.id().equals(id)) {
				return realm;
			}
		}
		return null;
	}

	public static boolean isRealm(World world) {
		return byWorld(world.getRegistryKey()) != null;
	}

	private Realms() {}
}
