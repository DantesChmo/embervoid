package com.embervoid.gametest;

import com.embervoid.Embervoid;
import com.embervoid.mob.MobLevels;
import com.embervoid.reach.world.ReachZones;
import com.embervoid.reach.world.ZoneTitles;
import com.embervoid.realm.RealmArenas;
import com.embervoid.realm.RealmDefinition;
import com.embervoid.realm.RealmMobLevels;
import com.embervoid.realm.Realms;
import com.embervoid.realm.placed.PlacedEnemies;
import com.embervoid.realm.placed.PlacedEnemyMarkerBlock;
import com.embervoid.realm.placed.PlacedEnemyProcessor;
import com.embervoid.realm.worldgen.ArenaSite;
import com.embervoid.realm.worldgen.RealmFeaturePiece;
import com.embervoid.sanctum.SanctumBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
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
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.Structure;

import java.util.List;

/** Track reach-world (PLAN 9.x): placed enemies, then realm worldgen pieces. */
public final class ReachWorldTests implements FabricGameTest {
	private static final String CAGE_TAG = "embervoid.placed.cage";

	/** A 3×1×1 template: a zombie marker facing east, an unknown-mob marker, then stone. */
	private static StructureTemplate markerTemplate(ServerWorld world) {
		NbtCompound root = new NbtCompound();
		root.put("size", ints(3, 1, 1));
		NbtList palette = new NbtList();
		palette.add(NbtHelper.fromBlockState(PlacedEnemies.MARKER.getDefaultState().with(PlacedEnemyMarkerBlock.FACING, Direction.EAST)));
		palette.add(NbtHelper.fromBlockState(net.minecraft.block.Blocks.STONE.getDefaultState()));
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
		context.assertTrue(world.getBlockState(origin.east(2)).isOf(net.minecraft.block.Blocks.STONE), "template not placed");
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
}
