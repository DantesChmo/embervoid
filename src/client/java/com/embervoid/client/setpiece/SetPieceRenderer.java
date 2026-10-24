package com.embervoid.client.setpiece;

import com.embervoid.block.setpiece.SetPieceBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders a set piece: its kind's model with the glowmask layer where the kind has a glowmask (a mill or a gear has
 * nothing that glows and no glowmask; GeckoLib crashes on an empty one, and on a missing one), culled at the kind's distance, outside its block when it is big. */
final class SetPieceRenderer extends GeoBlockRenderer<SetPieceBlockEntity> {
	SetPieceRenderer() {
		super(new SetPieceGeoModel());
		addRenderLayer(new GlowLayer(this));
	}

	/** The glowmask layer, skipped for a texture without {@code <texture>_glowmask.png}. */
	private static final class GlowLayer extends AutoGlowingGeoLayer<SetPieceBlockEntity> {
		private static final Map<Identifier, Boolean> GLOWS = new ConcurrentHashMap<>();

		GlowLayer(SetPieceRenderer renderer) {
			super(renderer);
		}

		@Override
		public void render(MatrixStack poseStack, SetPieceBlockEntity piece, BakedGeoModel model, RenderLayer renderType,
				VertexConsumerProvider bufferSource, VertexConsumer buffer, float partialTick, int light, int overlay) {
			if (GLOWS.computeIfAbsent(getTextureResource(piece), GlowLayer::hasGlowmask)) {
				super.render(poseStack, piece, model, renderType, bufferSource, buffer, partialTick, light, overlay);
			}
		}

		private static boolean hasGlowmask(Identifier texture) {
			Identifier glowmask = texture.withPath(path -> path.replace(".png", "_glowmask.png"));
			return MinecraftClient.getInstance().getResourceManager().getResource(glowmask).isPresent();
		}
	}

	@Override
	public boolean rendersOutsideBoundingBox(SetPieceBlockEntity piece) {
		return piece.kind().big();
	}

	@Override
	public int getRenderDistance() {
		return 128;
	}

	@Override
	public boolean isInRenderDistance(SetPieceBlockEntity piece, Vec3d camera) {
		return Vec3d.ofCenter(piece.getPos()).isInRange(camera, piece.kind().renderDistance());
	}
}
