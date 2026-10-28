package com.embervoid.gametest;

import com.embervoid.Embervoid;
import com.embervoid.boss.ArenaSpec;
import com.embervoid.hub.Hub;
import com.embervoid.hub.HubTravel;
import com.embervoid.realm.Realms;
import com.mojang.serialization.DataResult;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Codecs and rules that need the game's registries, which the JUnit classpath cannot load. */
public final class SaveFormatTests implements FabricGameTest {
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void wayHomeSurvivesASaveInAnyDimension(TestContext context) {
		for (RegistryKey<World> world : new RegistryKey[]{World.OVERWORLD, World.NETHER,
				RegistryKey.of(RegistryKeys.WORLD, new Identifier("othermod", "sky"))}) {
			HubTravel.Exit exit = new HubTravel.Exit(world, new Vec3d(-120.5, 64, 33.25), 135f);
			HubTravel.Exit read = roundTrip(HubTravel.Exit.CODEC.encodeStart(NbtOps.INSTANCE, exit),
					nbt -> HubTravel.Exit.CODEC.parse(NbtOps.INSTANCE, nbt));
			context.assertTrue(exit.equals(read), "the way home from " + world.getValue() + " came back as " + read);
		}
		context.complete();
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void wayHomeWithoutAYawFacesSouth(TestContext context) {
		NbtCompound nbt = (NbtCompound) HubTravel.Exit.CODEC.encodeStart(NbtOps.INSTANCE,
				new HubTravel.Exit(World.OVERWORLD, Vec3d.ZERO, 90f)).getOrThrow(false, error -> {});
		nbt.remove("yaw");
		HubTravel.Exit read = HubTravel.Exit.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow(false, error -> {});
		context.assertTrue(read.yaw() == 0f, "an exit saved without a yaw read as " + read.yaw());
		context.complete();
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void onlyWorldsOutsideTheModAreRememberedAsTheWayHome(TestContext context) {
		context.assertTrue(HubTravel.isOverworld(World.OVERWORLD), "the overworld");
		context.assertTrue(HubTravel.isOverworld(World.NETHER), "the Nether");
		context.assertTrue(HubTravel.isOverworld(World.END), "the End");
		context.assertTrue(HubTravel.isOverworld(RegistryKey.of(RegistryKeys.WORLD, new Identifier("othermod", "sky"))),
				"another mod's dimension");
		context.assertFalse(HubTravel.isOverworld(Hub.KEY), "the hub");
		Realms.ALL.forEach(realm -> context.assertFalse(HubTravel.isOverworld(realm.world()), "realm " + realm.world().getValue()));
		context.complete();
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void arenaSpecSurvivesASave(TestContext context) {
		ArenaSpec spec = new ArenaSpec(Embervoid.id("test_boss"), EntityType.WITHER_SKELETON, 25, 18);
		ArenaSpec read = roundTrip(ArenaSpec.CODEC.encodeStart(NbtOps.INSTANCE, spec), nbt -> ArenaSpec.CODEC.parse(NbtOps.INSTANCE, nbt));
		context.assertTrue(spec.equals(read), "the arena came back as " + read);
		context.complete();
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void arenaSpecNeverGivesAFightBelowLevelOneOrATinyArena(TestContext context) {
		context.assertTrue(ArenaSpec.CODEC.parse(NbtOps.INSTANCE, arenaNbt("minecraft:zombie", 0)).error().isPresent(), "level 0");
		NbtCompound tiny = arenaNbt("minecraft:zombie", 10);
		tiny.putInt("radius", 3);
		// 1.20.1's optionalFieldOf reads a bad value as its default instead of failing; either way no arena is that small.
		DataResult<ArenaSpec> small = ArenaSpec.CODEC.parse(NbtOps.INSTANCE, tiny);
		context.assertTrue(small.error().isPresent() || small.result().orElseThrow().radius() >= 4,
				"a radius-3 arena was read as " + small.result().map(ArenaSpec::radius).orElse(-1));
		context.complete();
	}

	private static NbtCompound arenaNbt(String entity, int level) {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("id", "embervoid:test_boss");
		nbt.putString("entity", entity);
		nbt.putInt("level", level);
		return nbt;
	}

	private static <T> T roundTrip(DataResult<NbtElement> written, java.util.function.Function<NbtElement, DataResult<T>> read) {
		return read.apply(written.getOrThrow(false, error -> {})).getOrThrow(false, error -> {});
	}
}
