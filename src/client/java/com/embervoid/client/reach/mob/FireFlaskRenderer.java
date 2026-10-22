package com.embervoid.client.reach.mob;

import com.embervoid.Embervoid;
import com.embervoid.reach.mob.FireFlaskEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;

/**
 * The Lantern Hag's fire flask in flight: a sprite of its own ({@link #MODEL}, an item model loaded as an extra model,
 * no item behind it) drawn like a thrown item, lit by its own flame. Once it is a fire patch it is invisible and only
 * its particles show.
 */
public class FireFlaskRenderer extends EntityRenderer<FireFlaskEntity> {
	public static final Identifier MODEL = Embervoid.id("item/fire_flask");
	private final ItemRenderer items;

	public FireFlaskRenderer(EntityRendererFactory.Context context) {
		super(context);
		this.items = context.getItemRenderer();
	}

	@Override
	public void render(FireFlaskEntity flask, float yaw, float tickDelta, MatrixStack matrices,
			VertexConsumerProvider vertices, int light) {
		if (flask.isInvisible()
				|| flask.age < 2 && dispatcher.camera.getFocusedEntity().squaredDistanceTo(flask) < 12.25) {
			return;
		}
		matrices.push();
		matrices.multiply(dispatcher.getRotation());
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));
		BakedModel model = MinecraftClient.getInstance().getBakedModelManager().getModel(MODEL);
		items.renderItem(flask.getStack(), ModelTransformationMode.GROUND, false, matrices, vertices, light,
				OverlayTexture.DEFAULT_UV, model);
		matrices.pop();
		super.render(flask, yaw, tickDelta, matrices, vertices, light);
	}

	@Override
	protected int getBlockLight(FireFlaskEntity flask, BlockPos pos) {
		return 15;
	}

	@Override
	public Identifier getTexture(FireFlaskEntity flask) {
		return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
	}
}
