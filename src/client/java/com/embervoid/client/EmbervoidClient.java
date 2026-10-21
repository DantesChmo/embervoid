package com.embervoid.client;

import com.embervoid.registry.ModBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;

public class EmbervoidClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Entity renderers, HUD (boss bar), keybinds.

		// Blocks with see-through pixels.
		BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(),
				ModBlocks.DEAD_DOOR, ModBlocks.DEAD_TRAPDOOR, ModBlocks.EMBER_LANTERN);
	}
}
