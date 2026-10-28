package com.embervoid.reach.world;

import com.embervoid.realm.EdgeLayout;
import com.embervoid.registry.ModBlocks;
import com.embervoid.realm.RealmDefinition;
import com.embervoid.realm.Realms;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/** Operator debug commands of the Ashen Reach world under {@code /embervoid realm}. Output is not translated. */
public final class ReachWorldCommands {
	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("embervoid").requires(source -> source.hasPermissionLevel(2)).then(literal("realm")
						// /embervoid realm edge: the border wall's radius and the broken bridges (console-safe).
						.then(literal("edge").executes(context -> {
							ServerWorld world = context.getSource().getWorld();
							RealmDefinition realm = Realms.byWorld(world.getRegistryKey());
							if (realm == null || !realm.world().equals(world.getRegistryKey())) {
								context.getSource().sendFeedback(() -> Text.literal("Not in a realm's open zones"), false);
								return 0;
							}
							StringBuilder text = new StringBuilder(String.format("wall at r %.0f", EdgeFeature.wallRadius(realm)));
							for (EdgeLayout.Bridge bridge : EdgeFeature.bridges(world, realm)) {
								double along = bridge.from() + 6;
								text.append(String.format("%nbridge at %d %d, %.0f blocks", (int) (Math.cos(bridge.angle()) * along),
										(int) (Math.sin(bridge.angle()) * along), bridge.to() - bridge.from()));
							}
							context.getSource().sendFeedback(() -> Text.literal(text.toString()), false);
							return 1;
						}))
						// /embervoid realm border <from> <to>: the final zone's border wall on both border lines
						// between those distances from the landing, as generated in loaded chunks, against its layout.
						.then(literal("border").then(argument("from", IntegerArgumentType.integer(0))
								.then(argument("to", IntegerArgumentType.integer(0)).executes(context -> border(context.getSource(),
										IntegerArgumentType.getInteger(context, "from"), IntegerArgumentType.getInteger(context, "to")))))))));
	}

	private static int border(ServerCommandSource source, int from, int to) {
		ServerWorld world = source.getWorld();
		RealmDefinition realm = Realms.byWorld(world.getRegistryKey());
		if (realm == null || !realm.world().equals(world.getRegistryKey()) || !realm.sectors().hasFinal()) {
			source.sendFeedback(() -> Text.literal("Not in a realm with a final zone"), false);
			return 0;
		}
		double[] borders = BorderWallFeature.borders(realm);
		StringBuilder text = new StringBuilder();
		for (int border = 0; border < borders.length; border++) {
			int expected = 0;
			int standing = 0;
			int gaps = 0;
			int notLoaded = 0;
			StringBuilder missing = new StringBuilder();
			for (int along = Math.max(from, (int) Math.ceil(BorderWallFeature.FROM)); along <= to; along++) {
				int x = MathHelper.floor(Math.cos(borders[border]) * (along + 0.5));
				int z = MathHelper.floor(Math.sin(borders[border]) * (along + 0.5));
				if (!world.isChunkLoaded(ChunkSectionPos.getSectionCoord(x), ChunkSectionPos.getSectionCoord(z))) {
					notLoaded++;
				} else if (wallExpected(world.getSeed(), border, EdgeLayout.borderColumn(borders[border], x, z)[0])) {
					expected++;
					if (BorderWallFeature.standsAt(world, x, z)) {
						standing++;
					} else if (missing.length() < 400) {
						missing.append(describeColumn(world, x, z, along));
					}
				} else {
					gaps++;
				}
			}
			if (border > 0) {
				text.append('\n');
			}
			text.append(String.format("border %.0f°: wall %d of %d, %d gap columns, %d not loaded", Math.toDegrees(borders[border]),
					standing, expected, gaps, notLoaded));
			text.append(missing);
		}
		source.sendFeedback(() -> Text.literal(text.toString()), false);
		return 1;
	}

	private static boolean wallExpected(long seed, int border, double along) {
		return EdgeLayout.borderWallHeight(seed, border, along) > 0 || EdgeLayout.isBorderTower(along);
	}

	private static String describeColumn(ServerWorld world, int x, int z, int along) {
		int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
		int brick = Integer.MIN_VALUE;
		for (int y = top; y > world.getBottomY(); y--) {
			BlockState state = world.getBlockState(new BlockPos(x, y, z));
			if (state.isOf(ModBlocks.ASH_BRICKS) || state.isOf(ModBlocks.CRACKED_ASH_BRICKS) || state.isOf(ModBlocks.ASH_BRICK_WALL)) {
				brick = y;
				break;
			}
		}
		return String.format("%nmissing at %d %d (along %d): top %d %s, highest brick %s", x, z, along, top,
				Registries.BLOCK.getId(world.getBlockState(new BlockPos(x, top - 1, z)).getBlock()),
				brick == Integer.MIN_VALUE ? "none" : String.valueOf(brick));
	}

	private ReachWorldCommands() {}
}
