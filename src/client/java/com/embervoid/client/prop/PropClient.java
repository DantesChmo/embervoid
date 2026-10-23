package com.embervoid.client.prop;

import com.embervoid.block.prop.Props;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.block.Block;
import net.minecraft.client.render.RenderLayer;

/** Client side of the props (PLAN 20.3): cutout, for the cart wheels, rags, bars and the standard's cloth. */
public final class PropClient {
	public static void register() {
		BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), Props.ALL.toArray(Block[]::new));
	}

	private PropClient() {}
}
