package com.embervoid.gametest;

import com.embervoid.Embervoid;
import com.embervoid.mob.MobLevels;
import com.embervoid.reach.gear.GearItems;
import com.embervoid.reach.world.ReachBlocks;
import com.embervoid.reach.world.ReachZones;
import com.embervoid.reach.world.ZoneTitles;
import com.embervoid.realm.RealmArenas;
import com.embervoid.realm.RealmDefinition;
import com.embervoid.realm.RealmMobLevels;
import com.embervoid.realm.RoadLayout;
import com.embervoid.realm.worldgen.RealmRoads;
import com.embervoid.realm.worldgen.RealmScatterPlacement;
import com.embervoid.registry.ModBlocks;
import com.embervoid.realm.Realms;
import com.embervoid.realm.placed.PlacedEnemies;
import com.embervoid.realm.placed.PlacedEnemyMarkerBlock;
import com.embervoid.realm.placed.PlacedEnemyProcessor;
import com.embervoid.realm.worldgen.ArenaSite;
import com.embervoid.realm.worldgen.RealmFeaturePiece;
import com.embervoid.sanctum.SanctumBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.structure.Structure;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Track reach-world (PLAN 9.x): placed enemies, then realm worldgen pieces. */
public final class ReachWorldTests implements FabricGameTest {
	private static final String CAGE_TAG = "embervoid.placed.cage";

	/** A 3×1×1 template: a zombie marker facing east, an unknown-mob marker, then stone. */
	private static StructureTemplate markerTemplate(ServerWorld world) {
		NbtCompound root = new NbtCompound();
		root.put("size", ints(3, 1, 1));
		NbtList palette = new NbtList();
		palette.add(NbtHelper.fromBlockState(PlacedEnemies.MARKER.getDefaultState().with(PlacedEnemyMarkerBlock.FACING, Direction.EAST)));
		palette.add(NbtHelper.fromBlockState(Blocks.STONE.getDefaultState()));
		root.put("palette", palette);
		NbtList blocks = new NbtList();
		blocks.add(block(0, 0, PlacedEnemies.markerNbt(new Identifier("minecraft", "zombie"), null, List.of(CAGE_TAG))));
		blocks.add(block(1, 0, PlacedEnemies.markerNbt(new Identifier("embervoid", "no_such_mob_for_tests"), null)));
		blocks.add(block(2, 1, null));
		root.put("blocks", blocks);
		root.put("entities", new NbtList());
		root.putInt("DataVersion", SharedConstants.getGameVersion().getSaveVersion().getId());
		return world.getStructureTemplateManager().createTemplate(root);
	}

	private static NbtCompound block(int x, int state, NbtCompound nbt) {
		NbtCompound entry = new NbtCompound();
		entry.put("pos", ints(x, 0, 0));
		entry.putInt("state", state);
		if (nbt != null) {
			entry.put("nbt", nbt);
		}
		return entry;
	}

	private static NbtList ints(int... values) {
		NbtList list = new NbtList();
		for (int v : values) {
			list.add(NbtInt.of(v));
		}
		return list;
	}

	private static List<? extends MobEntity> placedZombies(ServerWorld world, BlockPos around) {
		return world.getEntitiesByType(EntityType.ZOMBIE, new Box(around).expand(4), PlacedEnemies::isPlaced);
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void placedEnemySpawnsOnceAndPersists(TestContext context) {
		ServerWorld world = context.getWorld();
		BlockPos origin = context.getAbsolutePos(new BlockPos(1, 2, 1));
		StructureTemplate template = markerTemplate(world);

		// A pass for another chunk's box: the marker is cleared but nothing spawns there.
		StructurePlacementData elsewhere = new StructurePlacementData().addProcessor(PlacedEnemyProcessor.INSTANCE)
				.setBoundingBox(new BlockBox(origin.getX() + 1, origin.getY(), origin.getZ(), origin.getX() + 2,
						origin.getY(), origin.getZ()));
		template.place(world, origin, origin, elsewhere, world.getRandom(), 2);
		context.assertTrue(placedZombies(world, origin).isEmpty(), "spawned by a pass whose box misses the marker");

		StructurePlacementData whole = new StructurePlacementData().addProcessor(PlacedEnemyProcessor.INSTANCE);
		template.place(world, origin, origin, whole, world.getRandom(), 2);
		List<? extends MobEntity> zombies = placedZombies(world, origin);
		context.assertTrue(zombies.size() == 1, "expected one placed zombie, got " + zombies.size());
		MobEntity zombie = zombies.get(0);
		context.assertTrue(zombie.isPersistent(), "placed enemy is not persistent");
		context.assertTrue(zombie.getCommandTags().contains(CAGE_TAG), "marker tags not added: " + zombie.getCommandTags());
		context.assertTrue(zombie.getBlockPos().equals(origin), "placed at " + zombie.getBlockPos() + ", not " + origin);
		context.assertTrue(Math.abs(zombie.getYaw() - Direction.EAST.asRotation()) < 0.01f,
				"facing yaw " + zombie.getYaw() + ", marker faces east");
		context.assertTrue(world.getBlockState(origin).isAir(), "marker left in the world: " + world.getBlockState(origin));
		BlockState unknown = world.getBlockState(origin.east());
		context.assertTrue(unknown.isAir(), "unknown-id marker left in the world: " + unknown);
		context.assertTrue(world.getBlockState(origin.east(2)).isOf(Blocks.STONE), "template not placed");
		zombie.discard();
		context.complete();
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void placedEnemyFacingFollowsRotation(TestContext context) {
		ServerWorld world = context.getWorld();
		BlockPos origin = context.getAbsolutePos(new BlockPos(1, 2, 1));
		StructurePlacementData rotated = new StructurePlacementData().addProcessor(PlacedEnemyProcessor.INSTANCE)
				.setRotation(BlockRotation.CLOCKWISE_90);
		markerTemplate(world).place(world, origin, origin, rotated, world.getRandom(), 2);
		List<? extends MobEntity> zombies = placedZombies(world, origin);
		context.assertTrue(zombies.size() == 1, "expected one placed zombie, got " + zombies.size());
		float yaw = zombies.get(0).getYaw();
		context.assertTrue(Math.abs(yaw - Direction.SOUTH.asRotation()) < 0.01f, "east turned 90° should face south, yaw " + yaw);
		zombies.get(0).discard();
		context.complete();
	}

	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void placedEnemyGetsZoneLevel(TestContext context) {
		ServerWorld realm = context.getWorld().getServer().getWorld(Realms.ASHEN_REACH.world());
		context.assertTrue(realm != null, "the Ashen Reach dimension is missing from the test world");
		// Out in a zone, away from the landing structure. The test server never brings far realm chunks to entity
		// tracking, so check the level the realm provider gives the placed mob: what MobLevels applies when it loads.
		BlockPos column = new BlockPos(300, 0, 300);
		realm.getChunk(column);
		BlockPos pos = realm.getTopPosition(Heightmap.Type.MOTION_BLOCKING, column).up();
		NbtCompound marker = PlacedEnemies.markerNbt(new Identifier("minecraft", "zombie"), null);
		Entity entity = PlacedEnemies.spawn(realm, pos, 0, marker);
		context.assertTrue(entity instanceof MobEntity, "no zombie spawned");
		RealmDefinition.LevelBand band = RealmMobLevels.bandAt(realm, pos);
		int level = MobLevels.spawnLevel((MobEntity) entity, realm);
		entity.discard();
		context.assertTrue(band != null && level >= band.min() && level <= band.max(),
				"level " + level + " outside the zone band " + band);
		context.complete();
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void realmHasEternalDuskAndNoVanillaSpawns(TestContext context) {
		ServerWorld realm = context.getWorld().getServer().getWorld(Realms.ASHEN_REACH.world());
		context.assertTrue(realm != null, "the Ashen Reach dimension is missing from the test world");
		context.assertTrue(realm.getDimension().fixedTime().orElse(-1) == 13500,
				"realm time is not fixed at dusk: " + realm.getDimension().fixedTime());
		Registry<Biome> biomes = realm.getRegistryManager().get(RegistryKeys.BIOME);
		for (RegistryKey<Biome> key : List.of(ReachZones.LANDING, ReachZones.FIELDS, ReachZones.WOODS,
				ReachZones.NECROPOLIS, ReachZones.ABYSS, ReachZones.DEPTHS)) {
			Biome biome = biomes.get(key);
			context.assertTrue(biome != null, "missing biome " + key.getValue());
			for (SpawnGroup group : SpawnGroup.values()) {
				for (SpawnSettings.SpawnEntry entry : biome.getSpawnSettings().getSpawnEntries(group).getEntries()) {
					Identifier id = Registries.ENTITY_TYPE.getId(entry.type);
					context.assertTrue(!id.getNamespace().equals("minecraft"),
							key.getValue() + " still spawns vanilla " + id);
				}
			}
		}
		context.complete();
	}

	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void zoneTitleFollowsThePlayer(TestContext context) {
		ServerWorld realm = context.getWorld().getServer().getWorld(Realms.ASHEN_REACH.world());
		ServerPlayerEntity player = TestPlayers.create(context, 1, 1);
		BlockPos field = realm.getTopPosition(Heightmap.Type.MOTION_BLOCKING, new BlockPos(-300, 0, 300));
		player.teleport(realm, field.getX() + 0.5, field.getY() + 1, field.getZ() + 0.5, 0, 0);
		context.waitAndRun(25, () -> {
			String zone = ZoneTitles.zoneKey(player);
			context.assertTrue(zone != null && zone.startsWith("embervoid.zone.ashen_reach.zone_"), "not in a zone: " + zone);
			context.assertTrue(zone.equals(ZoneTitles.shownZone(player)), "shown " + ZoneTitles.shownZone(player) + ", in " + zone);
			BlockPos landing = realm.getTopPosition(Heightmap.Type.MOTION_BLOCKING, new BlockPos(8, 0, 8));
			player.teleport(realm, landing.getX() + 0.5, landing.getY() + 1, landing.getZ() + 0.5, 0, 0);
			context.waitAndRun(25, () -> {
				context.assertTrue("embervoid.zone.ashen_reach.landing".equals(ZoneTitles.shownZone(player)),
						"after the landing teleport shown " + ZoneTitles.shownZone(player));
				TestPlayers.complete(context, player);
			});
		});
	}
	/**
	 * Each arena site (PLAN 9.4) generates solid at its floor height, open to the sky above its contents, with the shrine. The test world does not generate
	 * structures by itself, so each arena start is made and placed the way worldgen does, at its real position.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 2400)
	public void arenaSitesAreFlatAndOpen(TestContext context) {
		ServerWorld realm = context.getWorld().getServer().getWorld(Realms.ASHEN_REACH.world());
		Registry<Structure> structures = realm.getRegistryManager().get(RegistryKeys.STRUCTURE);
		ChunkGenerator generator = realm.getChunkManager().getChunkGenerator();
		List<RealmArenas.Location> arenas = RealmArenas.all(realm);
		context.assertTrue(arenas.size() == 3, "expected 3 zone arenas, found " + arenas.size());
		for (String zone : List.of("a", "b", "c")) {
			String name = "ashen_reach_arena_" + zone;
			Structure structure = structures.get(Embervoid.id(name));
			RealmArenas.Location arena = arenas.stream()
					.filter(a -> a.spec().bossId().getPath().endsWith("zone_" + zone)).findFirst().orElseThrow();
			ChunkPos chunk = new ChunkPos(arena.pos());
			StructureStart start = structure.createStructureStart(realm.getRegistryManager(), generator,
					generator.getBiomeSource(), realm.getChunkManager().getNoiseConfig(), realm.getStructureTemplateManager(),
					realm.getSeed(), chunk, 0, realm, biome -> true);
			context.assertTrue(start.hasChildren(), name + ": no pieces at " + chunk);
			RealmFeaturePiece piece = (RealmFeaturePiece) start.getChildren().get(0);
			ArenaSite site = piece.site();
			context.assertTrue(site != null, name + ": the arena has no site");
			BlockPos center = piece.center();
			BlockBox box = start.getBoundingBox();
			ChunkPos.stream(new ChunkPos(box.getMinX() >> 4, box.getMinZ() >> 4), new ChunkPos(box.getMaxX() >> 4, box.getMaxZ() >> 4))
					.forEach(c -> start.place(realm, realm.getStructureAccessor(), generator, realm.getRandom(),
							new BlockBox(c.getStartX(), realm.getBottomY(), c.getStartZ(), c.getEndX(), realm.getTopY(), c.getEndZ()), c));
			// Arena contents (the template, or the placeholder's pillars and path) stand on the floor; above their
			// height the site is open sky.
			int contents = site.template().flatMap(id -> realm.getStructureTemplateManager().getTemplate(id))
					.map(template -> template.getSize().getY()).orElse(6);
			int checked = 0;
			for (int dx = -site.radius() + 1; dx < site.radius(); dx += 3) {
				for (int dz = -site.radius() + 1; dz < site.radius(); dz += 3) {
					if (Math.sqrt(dx * dx + dz * dz) > site.radius() - 1) {
						continue;
					}
					BlockPos floor = center.add(dx, 0, dz);
					context.assertTrue(!realm.getBlockState(floor).isAir(), name + ": hole in the floor at " + floor);
					for (int dy = contents + 1; dy <= contents + 6; dy++) {
						context.assertTrue(realm.getBlockState(floor.up(dy)).isAir(),
								name + ": the site is not open above " + floor + " (" + realm.getBlockState(floor.up(dy)) + " at +" + dy + ")");
					}
					checked++;
				}
			}
			context.assertTrue(realm.getBlockState(piece.shrinePos()).isOf(SanctumBlocks.EMBER_SHRINE),
					name + ": no shrine at " + piece.shrinePos());
			context.assertTrue(checked > 100, name + ": only " + checked + " floor columns checked");
		}
		context.complete();
	}
	/** Ore drops (PLAN 9.5): counts per DESIGN §4.5, and the pickaxe each ore needs. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void oresDropTheirMaterials(TestContext context) {
		ServerWorld world = context.getWorld();
		BlockPos pos = context.getAbsolutePos(new BlockPos(1, 2, 1));
		ItemStack iron = new ItemStack(Items.IRON_PICKAXE);
		ItemStack diamond = new ItemStack(Items.DIAMOND_PICKAXE);
		for (int i = 0; i < 20; i++) {
			assertDrop(context, world, pos, ReachBlocks.CINDER_ORE, iron, GearItems.CINDER_SHARD, 1, 3);
			assertDrop(context, world, pos, ReachBlocks.AMBER_VEIN, iron, GearItems.ROYAL_AMBER, 1, 2);
			assertDrop(context, world, pos, ReachBlocks.GRAVE_IRON_ORE, diamond, GearItems.RAW_GRAVE_IRON, 1, 1);
		}
		context.assertTrue(iron.isSuitableFor(ReachBlocks.CINDER_ORE.getDefaultState())
				&& iron.isSuitableFor(ReachBlocks.AMBER_VEIN.getDefaultState()), "an iron pickaxe cannot mine cinder / amber");
		context.assertTrue(!new ItemStack(Items.STONE_PICKAXE).isSuitableFor(ReachBlocks.CINDER_ORE.getDefaultState()),
				"a stone pickaxe mines cinder ore");
		context.assertTrue(!iron.isSuitableFor(ReachBlocks.GRAVE_IRON_ORE.getDefaultState())
				&& diamond.isSuitableFor(ReachBlocks.GRAVE_IRON_ORE.getDefaultState()), "grave iron is not a diamond-tier ore");
		for (Block ore : ReachBlocks.ORES) {
			context.assertTrue(ore.getDefaultState().isIn(ReachBlocks.REALM_ORES), ore + " is not in embervoid:realm_ores");
		}
		context.complete();
	}

	private static void assertDrop(TestContext context, ServerWorld world, BlockPos pos, Block ore, ItemStack tool,
			Item item, int min, int max) {
		List<ItemStack> drops = Block.getDroppedStacks(ore.getDefaultState(), world, pos, null, null, tool);
		int count = drops.stream().filter(stack -> stack.isOf(item)).mapToInt(ItemStack::getCount).sum();
		context.assertTrue(drops.size() == 1 && count >= min && count <= max,
				ore + " dropped " + drops + ", expected " + min + "–" + max + " " + item);
	}

	/** Each zone ore generates in its own zone only (PLAN 9.5). */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 2400)
	public void zoneOresStayInTheirZone(TestContext context) {
		ServerWorld realm = context.getWorld().getServer().getWorld(Realms.ASHEN_REACH.world());
		// Sector centres: start angle −90°, three 120° sectors.
		double[] centres = {-30, 90, 210};
		for (int zone = 0; zone < 3; zone++) {
			int[] counts = new int[ReachBlocks.ORES.size()];
			double a = Math.toRadians(centres[zone]);
			for (int k = 0; k < 4; k++) {
				int x = (int) (Math.cos(a) * 350) + (k % 2) * 16;
				int z = (int) (Math.sin(a) * 350) + (k / 2) * 16;
				WorldChunk chunk = realm.getChunk(x >> 4, z >> 4);
				BlockPos.Mutable pos = new BlockPos.Mutable();
				for (int dx = 0; dx < 16; dx++) {
					for (int dz = 0; dz < 16; dz++) {
						for (int y = realm.getBottomY(); y < 120; y++) {
							BlockState state = chunk.getBlockState(pos.set(dx, y, dz));
							int ore = ReachBlocks.ORES.indexOf(state.getBlock());
							if (ore >= 0) {
								counts[ore]++;
							}
						}
					}
				}
			}
			String found = "zone " + zone + ": cinder " + counts[0] + ", amber " + counts[1] + ", grave iron " + counts[2];
			for (int ore = 0; ore < counts.length; ore++) {
				context.assertTrue(ore == zone ? counts[ore] > 0 : counts[ore] == 0, found);
			}
		}
		context.complete();
	}
	/** Roads (PLAN 9.6): paved along the line, a milestone beside it, outposts with their chest. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 2400)
	public void roadsArePavedWithMilestonesAndOutposts(TestContext context) {
		ServerWorld realm = context.getWorld().getServer().getWorld(Realms.ASHEN_REACH.world());
		List<RoadLayout.Road> roads = RealmRoads.of(realm);
		context.assertTrue(roads.size() == 3, "expected 3 roads, got " + roads.size());
		RoadLayout.Road road = roads.get(0);
		int paved = 0;
		int checked = 0;
		for (int k = 4; k < 64; k += 3) {
			double[] p = road.points().get(k);
			BlockPos column = BlockPos.ofFloored(p[0], 0, p[1]);
			realm.getChunk(column);
			checked++;
			if (columnHas(realm, column, state -> ROAD_BLOCKS.contains(state.getBlock()))) {
				paved++;
			}
		}
		context.assertTrue(paved >= checked - 1, "road paved at only " + paved + " of " + checked + " points");

		RoadLayout.Milestone milestone = road.milestones().get(0);
		BlockPos stone = new BlockPos(milestone.x(), 0, milestone.z());
		realm.getChunk(stone);
		context.assertTrue(columnHas(realm, stone, state -> state.isOf(ReachBlocks.MILESTONE)), "no milestone at " + stone);

		RealmArenas.Location arena = RealmRoads.arenas(realm).get(road.arena());
		String table = "embervoid:chests/ashen_reach/" + ReachZones.zoneName(arena.spec().bossId()) + "_common";
		for (RoadLayout.Outpost outpost : road.outposts()) {
			ChunkPos centre = new ChunkPos(new BlockPos(outpost.x(), 0, outpost.z()));
			boolean chest = false;
			for (int cx = -1; cx <= 1; cx++) {
				for (int cz = -1; cz <= 1; cz++) {
					WorldChunk chunk = realm.getChunk(centre.x + cx, centre.z + cz);
					for (BlockEntity entity : chunk.getBlockEntities().values()) {
						if (entity instanceof LootableContainerBlockEntity
								&& table.equals(entity.createNbt().getString("LootTable"))) {
							chest = true;
						}
					}
				}
			}
			context.assertTrue(chest, outpost.kind() + " at " + outpost.x() + ", " + outpost.z() + " has no " + table + " chest");
		}
		context.complete();
	}

	/** The crossroads plaza (PLAN 9.6): a gap with lantern posts where each road leaves, the shrine in the middle. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 1200)
	public void landingPlazaOpensOntoTheRoads(TestContext context) {
		ServerWorld realm = context.getWorld().getServer().getWorld(Realms.ASHEN_REACH.world());
		Structure structure = realm.getRegistryManager().get(RegistryKeys.STRUCTURE).get(Embervoid.id("ashen_reach_landing"));
		ChunkGenerator generator = realm.getChunkManager().getChunkGenerator();
		StructureStart start = structure.createStructureStart(realm.getRegistryManager(), generator,
				generator.getBiomeSource(), realm.getChunkManager().getNoiseConfig(), realm.getStructureTemplateManager(),
				realm.getSeed(), new ChunkPos(0, 0), 0, realm, biome -> true);
		BlockBox box = start.getBoundingBox();
		ChunkPos.stream(new ChunkPos(box.getMinX() >> 4, box.getMinZ() >> 4), new ChunkPos(box.getMaxX() >> 4, box.getMaxZ() >> 4))
				.forEach(c -> start.place(realm, realm.getStructureAccessor(), generator, realm.getRandom(),
						new BlockBox(c.getStartX(), realm.getBottomY(), c.getStartZ(), c.getEndX(), realm.getTopY(), c.getEndZ()), c));
		RealmFeaturePiece piece = (RealmFeaturePiece) start.getChildren().get(0);
		BlockPos center = piece.center();
		context.assertTrue(realm.getBlockState(piece.shrinePos()).isOf(SanctumBlocks.EMBER_SHRINE), "no shrine on the plaza");
		for (RoadLayout.Road road : RealmRoads.of(realm)) {
			double[] exit = road.points().get(0);
			double angle = Math.atan2(exit[1], exit[0]);
			int r = RealmFeaturePiece.LANDING_RADIUS;
			BlockPos rim = center.add((int) Math.round(Math.cos(angle) * r), 1, (int) Math.round(Math.sin(angle) * r));
			context.assertTrue(realm.getBlockState(rim).isAir(), "the plaza wall blocks road " + road.arena() + " at " + rim);
			int lanterns = 0;
			for (BlockPos pos : BlockPos.iterate(rim.add(-7, 0, -7), rim.add(7, 3, 7))) {
				if (realm.getBlockState(pos).isOf(ModBlocks.EMBER_LANTERN)) {
					lanterns++;
				}
			}
			context.assertTrue(lanterns == 2, "road " + road.arena() + " exit has " + lanterns + " lantern posts");
		}
		context.complete();
	}

	private static final Set<Block> ROAD_BLOCKS = Set.of(ModBlocks.ASH_BRICKS, ModBlocks.CRACKED_ASH_BRICKS,
			ModBlocks.COBBLED_ASH_STONE, ModBlocks.ASH_STONE);

	/** Whether any of the top 12 blocks of a column matches. */
	private static boolean columnHas(ServerWorld world, BlockPos column, Predicate<BlockState> test) {
		int top = world.getTopY(Heightmap.Type.WORLD_SURFACE, column.getX(), column.getZ());
		for (int y = top; y > top - 12; y--) {
			if (test.test(world.getBlockState(new BlockPos(column.getX(), y, column.getZ())))) {
				return true;
			}
		}
		return false;
	}
	/**
	 * Minor structures (PLAN 9.7): each one built at a spot of its zone the way worldgen does, with every placed-enemy
	 * marker turned into air and its chest (if any) pointing at a zone table.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 2400)
	public void minorStructuresBuildWithTheirChests(TestContext context) {
		ServerWorld realm = context.getWorld().getServer().getWorld(Realms.ASHEN_REACH.world());
		Registry<Structure> structures = realm.getRegistryManager().get(RegistryKeys.STRUCTURE);
		ChunkGenerator generator = realm.getChunkManager().getChunkGenerator();
		Map<String, Double> zoneAngles = Map.of("fields", -30.0, "woods", 90.0, "necropolis", 210.0);
		int built = 0;
		int index = 0;
		for (Map.Entry<RegistryKey<Structure>, Structure> entry : structures.getEntrySet()) {
			String path = entry.getKey().getValue().getPath();
			if (!entry.getKey().getValue().getNamespace().equals("embervoid") || !path.startsWith("ashen_reach/")) {
				continue;
			}
			String zone = path.split("/")[1];
			double angle = Math.toRadians(zoneAngles.get(zone) + (index++ % 5 - 2) * 8);
			ChunkPos chunk = new ChunkPos(BlockPos.ofFloored(Math.cos(angle) * 520, 0, Math.sin(angle) * 520));
			StructureStart start = entry.getValue().createStructureStart(realm.getRegistryManager(), generator,
					generator.getBiomeSource(), realm.getChunkManager().getNoiseConfig(), realm.getStructureTemplateManager(),
					realm.getSeed(), chunk, 0, realm, biome -> true);
			context.assertTrue(start.hasChildren(), path + ": no pieces");
			BlockBox box = start.getBoundingBox();
			ChunkPos.stream(new ChunkPos(box.getMinX() >> 4, box.getMinZ() >> 4), new ChunkPos(box.getMaxX() >> 4, box.getMaxZ() >> 4))
					.forEach(c -> start.place(realm, realm.getStructureAccessor(), generator, realm.getRandom(),
							new BlockBox(c.getStartX(), realm.getBottomY(), c.getStartZ(), c.getEndX(), realm.getTopY(), c.getEndZ()), c));
			Set<String> tables = new HashSet<>();
			for (BlockPos pos : BlockPos.iterate(box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ())) {
				BlockState state = realm.getBlockState(pos);
				context.assertTrue(!state.isOf(PlacedEnemies.MARKER), path + ": placed-enemy marker left at " + pos);
				if (state.isOf(Blocks.CHEST) && realm.getBlockEntity(pos) instanceof LootableContainerBlockEntity chest) {
					tables.add(chest.createNbt().getString("LootTable"));
				}
			}
			for (String table : tables) {
				context.assertTrue(table.startsWith("embervoid:chests/ashen_reach/" + zone + "_"), path + ": chest table " + table);
			}
			built++;
		}
		context.assertTrue(built >= 20, "only " + built + " minor structures");
		context.complete();
	}

	/** Minor structure starts (PLAN 9.7) keep clear of the roads, the arena sites and the plaza. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 400)
	public void minorStructuresStayOffRoads(TestContext context) {
		ServerWorld realm = context.getWorld().getServer().getWorld(Realms.ASHEN_REACH.world());
		StructurePlacementCalculator calculator = realm.getChunkManager().getStructurePlacementCalculator();
		List<RoadLayout.Road> roads = RealmRoads.of(realm);
		int starts = 0;
		for (RegistryEntry<StructureSet> set : calculator.getStructureSets()) {
			if (!(set.value().placement() instanceof RealmScatterPlacement placement)) {
				continue;
			}
			for (int cx = -75; cx <= 75; cx++) {
				for (int cz = -75; cz <= 75; cz++) {
					if (!placement.shouldGenerate(calculator, cx, cz)) {
						continue;
					}
					starts++;
					int x = cx * 16;
					int z = cz * 16;
					for (RoadLayout.Road road : roads) {
						context.assertTrue(RoadLayout.distanceSquared(road, x, z) >= 20 * 20,
								"a minor structure starts on road " + road.arena() + " at chunk " + cx + ", " + cz);
					}
					for (RealmArenas.Location arena : RealmRoads.arenas(realm)) {
						context.assertTrue(arena.pos().getSquaredDistance(x, 0, z) >= Math.pow(arena.siteRadius() + 20, 2),
								"a minor structure starts on the " + arena.spec().bossId() + " site at chunk " + cx + ", " + cz);
					}
					context.assertTrue((double) x * x + (double) z * z >= 40 * 40, "a minor structure starts on the plaza");
				}
			}
		}
		context.assertTrue(starts >= 150, "only " + starts + " minor structure starts in 1200 blocks around the landing");
		context.complete();
	}
	/** The Necropolis crypt (PLAN 9.8): entrance, junction, halls, and always the lower crypt with the treasure. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 2400)
	public void cryptReachesTheLowerCrypt(TestContext context) {
		ServerWorld realm = context.getWorld().getServer().getWorld(Realms.ASHEN_REACH.world());
		Structure crypt = realm.getRegistryManager().get(RegistryKeys.STRUCTURE).get(Embervoid.id("ashen_reach/necropolis/crypt"));
		ChunkGenerator generator = realm.getChunkManager().getChunkGenerator();
		for (int attempt = 0; attempt < 3; attempt++) {
			double angle = Math.toRadians(200 + attempt * 10);
			ChunkPos chunk = new ChunkPos(BlockPos.ofFloored(Math.cos(angle) * 600, 0, Math.sin(angle) * 600));
			StructureStart start = crypt.createStructureStart(realm.getRegistryManager(), generator,
					generator.getBiomeSource(), realm.getChunkManager().getNoiseConfig(), realm.getStructureTemplateManager(),
					realm.getSeed() + attempt, chunk, 0, realm, biome -> true);
			List<String> pieces = start.getChildren().stream()
					.map(piece -> piece instanceof PoolStructurePiece pool ? pool.getPoolElement().toString() : "?").toList();
			context.assertTrue(pieces.size() >= 4, "crypt with only " + pieces);
			context.assertTrue(pieces.stream().anyMatch(p -> p.contains("crypt/lower_crypt")), "no lower crypt in " + pieces);
			BlockBox box = start.getBoundingBox();
			ChunkPos.stream(new ChunkPos(box.getMinX() >> 4, box.getMinZ() >> 4), new ChunkPos(box.getMaxX() >> 4, box.getMaxZ() >> 4))
					.forEach(c -> start.place(realm, realm.getStructureAccessor(), generator, realm.getRandom(),
							new BlockBox(c.getStartX(), realm.getBottomY(), c.getStartZ(), c.getEndX(), realm.getTopY(), c.getEndZ()), c));
			boolean treasure = false;
			boolean water = false;
			for (BlockPos pos : BlockPos.iterate(box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ())) {
				BlockState state = realm.getBlockState(pos);
				context.assertTrue(!state.isOf(PlacedEnemies.MARKER), "placed-enemy marker left in the crypt at " + pos);
				water |= state.isOf(Blocks.WATER);
				if (state.isOf(Blocks.CHEST) && realm.getBlockEntity(pos) instanceof LootableContainerBlockEntity chest) {
					treasure |= chest.createNbt().getString("LootTable").equals("embervoid:chests/ashen_reach/necropolis_treasure");
				}
			}
			context.assertTrue(treasure, "no Necropolis treasure chest in the crypt " + pieces);
			context.assertTrue(water, "no water in the crypt " + pieces);
		}
		context.complete();
	}
}
