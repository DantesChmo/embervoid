package com.embervoid.gametest;

import com.embervoid.mob.MobLevels;
import com.embervoid.realm.RealmDefinition;
import com.embervoid.realm.RealmMobLevels;
import com.embervoid.realm.Realms;
import com.embervoid.realm.placed.PlacedEnemies;
import com.embervoid.realm.placed.PlacedEnemyMarkerBlock;
import com.embervoid.realm.placed.PlacedEnemyProcessor;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

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
}
