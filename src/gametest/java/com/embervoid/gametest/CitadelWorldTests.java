package com.embervoid.gametest;

import com.embervoid.Embervoid;
import com.embervoid.reach.citadel.CitadelLayout;
import com.embervoid.reach.citadel.CitadelPiece;
import com.embervoid.reach.gear.GearLootTables;
import com.embervoid.reach.world.ReachBlocks;
import com.embervoid.reach.world.ReachZones;
import com.embervoid.realm.RealmArenas;
import com.embervoid.realm.RealmDefinition;
import com.embervoid.realm.RealmMobLevels;
import com.embervoid.realm.Realms;
import com.embervoid.realm.placed.PlacedEnemies;
import com.embervoid.realm.worldgen.RealmFeaturePlacement;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The depths "Burning City" and the Smoldering Citadel (PLAN 13.1–13.2, track citadel). */
public final class CitadelWorldTests implements FabricGameTest {
	private static ServerWorld depths(TestContext context) {
		ServerWorld depths = context.getWorld().getServer().getWorld(Realms.ASHEN_REACH.depths());
		context.assertTrue(depths != null, "the depths dimension is missing from the test world");
		return depths;
	}

	/** Own dimension type: the same dusk as the open zones, ultrawarm, dark red sky. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void depthsHaveTheirOwnDuskAndRedSky(TestContext context) {
		ServerWorld depths = depths(context);
		context.assertTrue(depths.getDimensionKey().getValue().equals(Embervoid.id("ashen_reach_depths")),
				"depths dimension type is " + depths.getDimensionKey().getValue());
		context.assertTrue(depths.getDimension().fixedTime().orElse(-1) == 13500, "depths time is not fixed at dusk");
		context.assertTrue(depths.getDimension().ultrawarm(), "the Burning City is not ultrawarm");
		Biome biome = depths.getRegistryManager().get(RegistryKeys.BIOME).get(ReachZones.DEPTHS);
		int sky = biome.getSkyColor();
		context.assertTrue((sky >> 16 & 0xff) > 2 * (sky & 0xff) && (sky >> 16 & 0xff) > 2 * (sky >> 8 & 0xff),
				"sky is not dark red: " + Integer.toHexString(sky));
		RealmDefinition.LevelBand band = RealmMobLevels.bandAt(depths, new BlockPos(0, 80, -100));
		context.assertTrue(band != null && band.min() == 25 && band.max() == 30, "depths levels " + band);
		context.complete();
	}

	/** The ground matches the layout: the landing and the courtyard flat at their heights, the hill above the city. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 400)
	public void depthsTerrainFollowsTheLayout(TestContext context) {
		ServerWorld depths = depths(context);
		ChunkGenerator generator = depths.getChunkManager().getChunkGenerator();
		for (int dx = -24; dx <= 24; dx += 8) {
			for (int dz = -24; dz <= 24; dz += 8) {
				assertGround(context, depths, generator, dx, dz, CitadelLayout.LANDING_Y);
				assertGround(context, depths, generator, CitadelLayout.COURTYARD_X + dx, CitadelLayout.COURTYARD_Z + dz,
						CitadelLayout.PLATEAU_Y);
			}
		}
		for (CitadelLayout.Piece piece : List.of(CitadelLayout.OUTSKIRTS, CitadelLayout.MARKET, CitadelLayout.KEEP)) {
			assertGround(context, depths, generator, (piece.minX() + piece.maxX()) / 2, (piece.minZ() + piece.maxZ()) / 2,
					piece.floorY());
		}
		int below = height(depths, generator, 130, -200);
		context.assertTrue(below < CitadelLayout.PLATEAU_Y - 12, "the hill's foot east of the plateau is at " + below);
		context.complete();
	}

	/** Ember cracks and smouldering logs lie in the Burning City, and nowhere in the open zones. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 2400)
	public void emberCracksAndSmolderingLogsOnlyInTheDepths(TestContext context) {
		ServerWorld depths = depths(context);
		int[] inDepths = count(depths, 120, -40, 3);
		context.assertTrue(inDepths[0] > 0 && inDepths[1] > 0,
				"depths: " + inDepths[0] + " ember cracks, " + inDepths[1] + " smouldering logs");
		ServerWorld realm = context.getWorld().getServer().getWorld(Realms.ASHEN_REACH.world());
		for (double angle : new double[]{-30, 90, 210}) {
			double a = Math.toRadians(angle);
			int[] inZone = count(realm, (int) (Math.cos(a) * 300), (int) (Math.sin(a) * 300), 2);
			context.assertTrue(inZone[0] == 0 && inZone[1] == 0,
					"zone at " + angle + "°: " + inZone[0] + " ember cracks, " + inZone[1] + " smouldering logs");
		}
		context.complete();
	}

	/** The Smoldering Lord's courtyard is the depths arena, fixed north of the landing on the plateau. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void courtyardIsTheDepthsArena(TestContext context) {
		ServerWorld depths = depths(context);
		List<RealmArenas.Location> arenas = RealmArenas.all(depths);
		context.assertTrue(arenas.size() == 1, "expected one depths arena, found " + arenas.size());
		RealmArenas.Location arena = arenas.get(0);
		context.assertTrue(arena.pos().getX() == CitadelLayout.COURTYARD_X && arena.pos().getZ() == CitadelLayout.COURTYARD_Z,
				"courtyard at " + arena.pos());
		context.assertTrue(arena.siteRadius() == CitadelLayout.COURTYARD_RADIUS, "courtyard site radius " + arena.siteRadius());
		context.assertTrue(arena.spec().bossId().equals(Realms.ASHEN_REACH.finalBoss()) && arena.spec().level() == 30,
				"courtyard boss " + arena.spec());
		context.complete();
	}

	/**
	 * The Citadel (PLAN 13.2): one start at its fixed chunk, every piece with its template, built the way worldgen does
	 * at its real position. Placed mobs where the bestiary puts them (read from the templates' markers, then spawned with
	 * no marker left), Citadel chests only, the treasure in the keep, and the route open from the landing to the keep.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 6000)
	public void citadelBuildsAlongTheRoute(TestContext context) {
		ServerWorld depths = depths(context);
		ChunkGenerator generator = depths.getChunkManager().getChunkGenerator();
		Identifier id = Embervoid.id("ashen_reach_depths_citadel");
		Structure structure = depths.getRegistryManager().get(RegistryKeys.STRUCTURE).get(id);
		context.assertTrue(structure != null, "no structure " + id);
		ChunkPos startChunk = null;
		for (RegistryEntry<StructureSet> set : depths.getChunkManager().getStructurePlacementCalculator().getStructureSets()) {
			if (set.value().placement() instanceof RealmFeaturePlacement placement
					&& set.value().structures().stream().anyMatch(e -> e.structure().value() == structure)) {
				startChunk = placement.getStartChunk(depths.getSeed(), 0, 0);
			}
		}
		context.assertTrue(new ChunkPos(new BlockPos(CitadelLayout.START_X, 0, CitadelLayout.START_Z)).equals(startChunk),
				"the Citadel starts in chunk " + startChunk);
		StructureStart start = structure.createStructureStart(depths.getRegistryManager(), generator, generator.getBiomeSource(),
				depths.getChunkManager().getNoiseConfig(), depths.getStructureTemplateManager(), depths.getSeed(), startChunk, 0,
				depths, biome -> true);
		context.assertTrue(start.getChildren().size() == CitadelLayout.PIECES.size(), "pieces: " + start.getChildren().size());

		// Markers in the templates: which mob each piece places.
		Map<String, List<Identifier>> mobs = new HashMap<>();
		for (StructurePiece child : start.getChildren()) {
			CitadelPiece piece = (CitadelPiece) child;
			StructureTemplate template = piece.getTemplate();
			CitadelLayout.Piece layout = CitadelLayout.PIECES.stream()
					.filter(p -> p.template().equals(piece.templateId())).findFirst().orElseThrow();
			context.assertTrue(template.getSize().getX() == layout.sizeX() && template.getSize().getY() == layout.sizeY()
					&& template.getSize().getZ() == layout.sizeZ(), layout.name() + ": template size " + template.getSize());
			List<Identifier> ids = new ArrayList<>();
			for (StructureTemplate.StructureBlockInfo info : template.getInfosForBlock(BlockPos.ORIGIN, new StructurePlacementData(),
					PlacedEnemies.MARKER)) {
				ids.add(new Identifier(info.nbt().getString("entity")));
			}
			mobs.put(layout.name(), ids);
		}
		Identifier marksman = Embervoid.id("sparking_marksman");
		Identifier golem = Embervoid.id("furnace_golem");
		Identifier knight = Embervoid.id("smoldering_knight");
		Map<String, List<Identifier>> expected = Map.of(
				"gatehouse", List.of(marksman, marksman, marksman), "wall_west", List.of(marksman, marksman),
				"wall_east", List.of(marksman, marksman), "tower_west", List.of(knight), "tower_east", List.of(knight),
				"inner_ring_west", List.of(golem, golem, golem), "inner_ring_east", List.of(golem, golem),
				"keep", List.of(knight, knight));
		for (CitadelLayout.Piece piece : CitadelLayout.PIECES) {
			List<Identifier> found = new ArrayList<>(mobs.get(piece.name()));
			List<Identifier> want = new ArrayList<>(expected.getOrDefault(piece.name(), List.of()));
			found.sort(null);
			want.sort(null);
			context.assertTrue(found.equals(want), piece.name() + " places " + found + ", expected " + want);
		}

		BlockBox box = start.getBoundingBox();
		ChunkPos.stream(new ChunkPos(box.getMinX() >> 4, box.getMinZ() >> 4), new ChunkPos(box.getMaxX() >> 4, box.getMaxZ() >> 4))
				.forEach(c -> start.place(depths, depths.getStructureAccessor(), generator, depths.getRandom(),
						new BlockBox(c.getStartX(), depths.getBottomY(), c.getStartZ(), c.getEndX(), depths.getTopY(), c.getEndZ()), c));

		int common = 0;
		List<BlockPos> treasure = new ArrayList<>();
		for (StructurePiece child : start.getChildren()) {
			BlockBox pieceBox = child.getBoundingBox();
			for (BlockPos pos : BlockPos.iterate(pieceBox.getMinX(), pieceBox.getMinY(), pieceBox.getMinZ(),
					pieceBox.getMaxX(), pieceBox.getMaxY(), pieceBox.getMaxZ())) {
				BlockState state = depths.getBlockState(pos);
				context.assertTrue(!state.isOf(PlacedEnemies.MARKER), "placed-enemy marker left at " + pos);
				if (state.isOf(Blocks.CHEST) && depths.getBlockEntity(pos) instanceof LootableContainerBlockEntity chest) {
					String table = chest.createNbt().getString("LootTable");
					if (table.equals(GearLootTables.CITADEL_TREASURE.toString())) {
						treasure.add(pos.toImmutable());
					} else {
						context.assertTrue(table.equals(GearLootTables.CITADEL_COMMON.toString()), "chest table " + table + " at " + pos);
						common++;
					}
				}
			}
		}
		CitadelLayout.Piece keep = CitadelLayout.KEEP;
		context.assertTrue(treasure.size() == 1 && treasure.get(0).getX() >= keep.minX() && treasure.get(0).getX() <= keep.maxX()
				&& treasure.get(0).getZ() >= keep.minZ() && treasure.get(0).getZ() <= keep.maxZ(), "treasure chests at " + treasure);
		context.assertTrue(common >= 7, "only " + common + " common chests");

		// The spawned mobs, then gone again (test worlds are shared).
		int placed = 0;
		List<Entity> spawned = new ArrayList<>();
		for (Entity entity : depths.iterateEntities()) {
			if (PlacedEnemies.isPlaced(entity) && box.contains(entity.getBlockPos())) {
				spawned.add(entity);
			}
		}
		for (Entity entity : spawned) {
			placed++;
			entity.discard();
		}
		context.assertTrue(placed == 16, "spawned " + placed + " placed mobs, expected 16");

		// The route is open: the gate, the tunnel, the plaza, the courtyard's edge, the keep door and its hall.
		int floor = CitadelLayout.PLATEAU_Y;
		for (BlockPos stand : List.of(new BlockPos(0, 67 + 1, -45), new BlockPos(0, 71 + 1, -100), new BlockPos(0, 75, -130),
				new BlockPos(0, 82, -140), new BlockPos(0, floor + 1, -150), new BlockPos(0, floor + 1, -231),
				new BlockPos(4, floor + 1, -255))) {
			context.assertTrue(!depths.getBlockState(stand.down()).isAir(), "no floor under " + stand);
			context.assertTrue(depths.getBlockState(stand).getCollisionShape(depths, stand).isEmpty()
					&& depths.getBlockState(stand.up()).getCollisionShape(depths, stand.up()).isEmpty(), "route blocked at " + stand);
		}
		context.complete();
	}

	private static void assertGround(TestContext context, ServerWorld world, ChunkGenerator generator, int x, int z, int expected) {
		int y = height(world, generator, x, z);
		context.assertTrue(y == expected, "ground at " + x + ", " + z + " is " + y + ", expected " + expected);
	}

	private static int height(ServerWorld world, ChunkGenerator generator, int x, int z) {
		return generator.getHeightInGround(x, z, Heightmap.Type.WORLD_SURFACE_WG, world, world.getChunkManager().getNoiseConfig());
	}

	/** Ember cracks and smouldering logs in a square of (2r + 1)² chunks around a column. */
	private static int[] count(ServerWorld world, int x, int z, int r) {
		int[] counts = new int[2];
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int cx = (x >> 4) - r; cx <= (x >> 4) + r; cx++) {
			for (int cz = (z >> 4) - r; cz <= (z >> 4) + r; cz++) {
				WorldChunk chunk = world.getChunk(cx, cz);
				for (int dx = 0; dx < 16; dx++) {
					for (int dz = 0; dz < 16; dz++) {
						int top = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, dx, dz);
						for (int y = top - 4; y <= top + 1; y++) {
							Block block = chunk.getBlockState(pos.set(dx, y, dz)).getBlock();
							if (block == ReachBlocks.EMBER_CRACK) {
								counts[0]++;
							} else if (block == ReachBlocks.SMOLDERING_LOG) {
								counts[1]++;
							}
						}
					}
				}
			}
		}
		return counts;
	}
}
