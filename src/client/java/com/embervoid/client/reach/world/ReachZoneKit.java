package com.embervoid.client.reach.world;

import com.embervoid.Embervoid;
import com.embervoid.realm.placed.PlacedEnemies;
import com.embervoid.realm.placed.PlacedEnemyMarkerBlock;
import com.embervoid.sanctum.datagen.TemplateBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.Random;

/**
 * What a zone puts into its templates (PLAN 9.6–9.8): its name in ids, its mobs by role (final mob ids, DESIGN §4.5
 * bestiary) and its chest tables. Outposts get fodder and ranged mobs only, never tanks. The Fields have no tank or
 * ranged mob: their "tank" slot is the Ash Stalker ambusher, their "ranged" slot the Ashen Carrion perching up high.
 */
public record ReachZoneKit(String name, Identifier fodder, Identifier ranged, Identifier tank, Identifier elite) {
	public static final ReachZoneKit FIELDS = new ReachZoneKit("fields", mob("cinder_husk"), mob("ashen_carrion"),
			mob("ash_stalker"), mob("ashen_carrion"));
	public static final ReachZoneKit WOODS = new ReachZoneKit("woods", mob("withered_thrall"), mob("lantern_hag"),
			mob("bark_hollow"), mob("bark_hollow"));
	public static final ReachZoneKit NECROPOLIS = new ReachZoneKit("necropolis", mob("restless_bones"), mob("crypt_moth"),
			mob("hollow_sentinel"), mob("grave_warden"));
	public static final List<ReachZoneKit> ALL = List.of(FIELDS, WOODS, NECROPOLIS);

	public Identifier commonChest() {
		return Embervoid.id("chests/ashen_reach/" + name + "_common");
	}

	public Identifier treasureChest() {
		return Embervoid.id("chests/ashen_reach/" + name + "_treasure");
	}

	/** A placed-enemy marker at x, y, z (the block the mob stands in). */
	public static void mob(TemplateBuilder t, int x, int y, int z, Identifier mob, Direction facing, String... tags) {
		t.set(x, y, z, PlacedEnemies.MARKER.getDefaultState().with(PlacedEnemyMarkerBlock.FACING, facing),
				PlacedEnemies.markerNbt(mob, null, List.of(tags)));
	}

	/** A seeded random per template, so the output never changes between datagen runs. */
	public static Random random(String template) {
		return new Random(template.hashCode());
	}

	private static Identifier mob(String path) {
		return Embervoid.id(path);
	}
}
