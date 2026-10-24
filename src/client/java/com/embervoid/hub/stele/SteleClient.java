package com.embervoid.hub.stele;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;

/** Client side of the Cleft Stele: one call from {@code EmbervoidClient.onInitializeClient}. */
public final class SteleClient {
	public static void register() {
		// The crack's and the rift's mist is see-through and fades out at its edges.
		BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getTranslucent(), SteleBlocks.STELE_CRACK, SteleBlocks.STELE_RIFT);
	}

	private SteleClient() {}
}
