package com.embervoid.client;

import com.embervoid.Embervoid;
import com.embervoid.config.ConfigSync;
import com.embervoid.client.anim.AttackAnimationsClient;
import com.embervoid.client.boss.ArenaFogClient;
import com.embervoid.client.boss.ArenaShrinesClient;
import com.embervoid.client.fx.CombatFxClient;
import com.embervoid.client.fx.particle.EmberParticlesClient;
import com.embervoid.client.reach.boss.ReachBossClient;
import com.embervoid.client.mob.MobLevelClient;
import com.embervoid.client.mob.MobRenderers;
import com.embervoid.client.nightmob.NightMobClient;
import com.embervoid.client.prop.PropClient;
import com.embervoid.client.moon.MoonClient;
import com.embervoid.client.reach.mob.ReachMobClient;
import com.embervoid.client.realm.RealmClient;
import com.embervoid.client.reach.gear.GearClient;
import com.embervoid.client.reliquary.ReliquaryClient;
import com.embervoid.client.sanctum.boss.SanctumBossClient;
import com.embervoid.client.vampire.VampireClient;
import com.embervoid.config.EmbervoidClientConfig;
import com.embervoid.config.EmbervoidConfig;
import com.embervoid.hub.stele.SteleClient;
import com.embervoid.registry.ModBlocks;
import com.embervoid.registry.ModEntities;
import com.embervoid.registry.ModItems;
import com.embervoid.sanctum.SanctumClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.EmptyEntityRenderer;
import net.minecraft.text.Text;

public class EmbervoidClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EmbervoidClientConfig.load();

		// Entity renderers, HUD (boss bar), keybinds.

		// Blocks with see-through pixels.
		BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(),
				ModBlocks.DEAD_DOOR, ModBlocks.DEAD_TRAPDOOR, ModBlocks.EMBER_LANTERN);

		ClientPlayerData.register();
		ClientStamina.register();
		MobRenderers.register();
		MobLevelClient.register();
		NightMobClient.register();
		ReachMobClient.register();
		RealmClient.register();
		MoonClient.register();
		// Also registers the progress sync receiver.
		SanctumClient.register();
		SteleClient.register();
		VampireClient.register();
		GearClient.register();
		ReliquaryClient.register();
		ArenaFogClient.register();
		ArenaShrinesClient.register();
		ReachBossClient.register();
		SanctumBossClient.register();
		CombatFxClient.register();
		EmberParticlesClient.register();
		AttackAnimationsClient.register();
		PropClient.register();
		// Particles and the name label only; a model comes with the art pass.
		EntityRendererRegistry.register(ModEntities.ASH_REMNANT, EmptyEntityRenderer::new);
		FabricModelPredicateProviderRegistry.register(ModItems.HEALING_FLASK, Embervoid.id("empty"),
				(stack, world, entity, seed) -> ClientPlayerData.get().flaskCharges <= 0 ? 1 : 0);

		// Use the server's config while connected (in singleplayer it is the same as the local one).
		ClientPlayNetworking.registerGlobalReceiver(ConfigSync.SYNC_PACKET, (client, handler, buf, responseSender) -> {
			EmbervoidConfig config = EmbervoidConfig.fromNetworkString(buf.readString());
			client.execute(() -> EmbervoidConfig.useServer(config));
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(EmbervoidConfig::useLocal));

		// /embervoid_client data: show the player data as the client sees it (debug).
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
				ClientCommandManager.literal("embervoid_client")
						.then(ClientCommandManager.literal("data").executes(context -> {
							context.getSource().sendFeedback(
									Text.literal("[client] " + ClientPlayerData.get()
											+ ", maxStamina=" + ClientStamina.max()));
							return 1;
						}))));
	}
}
