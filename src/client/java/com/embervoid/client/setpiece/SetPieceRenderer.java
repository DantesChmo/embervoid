package com.embervoid.client.setpiece;

import com.embervoid.block.setpiece.SetPieceBlockEntity;
import net.minecraft.util.math.Vec3d;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Renders a set piece: its kind's model with the glowmask layer, culled at the kind's distance, outside its block when it is big. */
final class SetPieceRenderer extends GeoBlockRenderer<SetPieceBlockEntity> {
	SetPieceRenderer() {
		super(new SetPieceGeoModel());
		addRenderLayer(new AutoGlowingGeoLayer<>(this));
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
