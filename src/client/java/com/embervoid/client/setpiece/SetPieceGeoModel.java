package com.embervoid.client.setpiece;

import com.embervoid.Embervoid;
import com.embervoid.block.setpiece.SetPieceBlockEntity;
import com.embervoid.block.setpiece.SetPieceKind;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One GeckoLib model for every set piece: the files of the piece's kind under {@code geo/block/setpiece},
 * {@code animations/block/setpiece}, {@code textures/block/setpiece}. Bones named {@code bones} follow the block
 * entity's bones flag, bones whose name starts with {@code flame} show only in the lit state.
 */
final class SetPieceGeoModel extends GeoModel<SetPieceBlockEntity> {
	private static final Map<String, Identifier[]> PATHS = new ConcurrentHashMap<>();

	private static Identifier[] paths(SetPieceKind kind) {
		return PATHS.computeIfAbsent(kind.name(), name -> new Identifier[]{
				Embervoid.id("geo/block/setpiece/" + name + ".geo.json"),
				Embervoid.id("animations/block/setpiece/" + name + ".animation.json"),
				Embervoid.id("textures/block/setpiece/" + name + ".png")});
	}

	@Override
	public Identifier getModelResource(SetPieceBlockEntity piece) {
		return paths(piece.kind())[0];
	}

	@Override
	public Identifier getAnimationResource(SetPieceBlockEntity piece) {
		return paths(piece.kind())[1];
	}

	@Override
	public Identifier getTextureResource(SetPieceBlockEntity piece) {
		return paths(piece.kind())[2];
	}

	@Override
	public void setCustomAnimations(SetPieceBlockEntity piece, long instanceId, AnimationState<SetPieceBlockEntity> state) {
		super.setCustomAnimations(piece, instanceId, state);
		SetPieceKind kind = piece.kind();
		if (kind.bonesToggle()) {
			CoreGeoBone bones = getAnimationProcessor().getBone("bones");
			if (bones != null) {
				bones.setHidden(!piece.showsBones());
			}
		}
		if (kind.litState() != null) {
			boolean lit = kind.litState().equals(piece.state());
			for (CoreGeoBone bone : getAnimationProcessor().getRegisteredBones()) {
				if (bone.getName().startsWith("flame")) {
					bone.setHidden(!lit);
				}
			}
		}
	}
}
