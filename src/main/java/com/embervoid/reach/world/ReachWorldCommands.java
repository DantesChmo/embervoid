package com.embervoid.reach.world;

import com.embervoid.realm.EdgeLayout;
import com.embervoid.realm.RealmDefinition;
import com.embervoid.realm.Realms;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

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
						})))));
	}

	private ReachWorldCommands() {}
}
