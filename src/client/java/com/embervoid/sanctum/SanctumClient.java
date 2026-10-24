package com.embervoid.sanctum;

import com.embervoid.progress.ProgressClient;
import com.embervoid.sanctum.leveling.LevelUpNetworking;
import com.embervoid.sanctum.leveling.StatSheet;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

/** Client side of the Ember Sanctum: one call from {@code EmbervoidClient.onInitializeClient}. */
public final class SanctumClient {
	public static void register() {
		ProgressClient.register();
		BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), SanctumBlocks.EMBER_SHRINE, SanctumBlocks.VOID_OBELISK);
		BlockRenderLayerMap.INSTANCE.putBlock(SanctumBlocks.FOG_WALL, RenderLayer.getTranslucent());
		BlockEntityRendererFactories.register(SanctumBlocks.VOID_OBELISK_ENTITY, VoidObeliskRenderer::new);
		EntityRendererRegistry.register(SanctumEntities.ASHEN_MAIDEN, AshenMaidenRenderer::new);

		ClientPlayNetworking.registerGlobalReceiver(LevelUpNetworking.SHEET, (client, handler, buf, responseSender) -> {
			boolean open = buf.readBoolean();
			int maidenId = buf.readVarInt();
			StatSheet sheet = StatSheet.read(buf);
			client.execute(() -> {
				if (client.currentScreen instanceof LevelUpScreen screen && screen.maidenId() == maidenId) {
					screen.update(sheet);
				} else if (open) {
					client.setScreen(new LevelUpScreen(maidenId, sheet));
				}
			});
		});
	}

	private SanctumClient() {}
}
