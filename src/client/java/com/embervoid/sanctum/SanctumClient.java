package com.embervoid.sanctum;

import com.embervoid.progress.ProgressClient;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

/** Client side of the Ember Sanctum: one call from {@code EmbervoidClient.onInitializeClient}. */
public final class SanctumClient {
	public static void register() {
		ProgressClient.register();
		BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), SanctumBlocks.EMBER_SHRINE, SanctumBlocks.VOID_OBELISK);
		BlockEntityRendererFactories.register(SanctumBlocks.VOID_OBELISK_ENTITY, VoidObeliskRenderer::new);
		EntityRendererRegistry.register(SanctumEntities.ASHEN_MAIDEN, AshenMaidenRenderer::new);
	}

	private SanctumClient() {}
}
