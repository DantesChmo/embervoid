package com.embervoid.client.reach.boss;

import com.embervoid.reach.boss.TreeLanternEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Draws the Grieving Tree's hanged lantern as a hanging lantern block with a stretch of chain above it, always lit. */
public class TreeLanternRenderer extends EntityRenderer<TreeLanternEntity> {
	private static final BlockState LANTERN = Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true);
	private static final BlockState CHAIN = Blocks.CHAIN.getDefaultState();
	private final BlockRenderManager blocks;

	public TreeLanternRenderer(EntityRendererFactory.Context context) {
		super(context);
		this.blocks = context.getBlockRenderManager();
	}

	@Override
	public void render(TreeLanternEntity lantern, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertices, int light) {
		matrices.push();
		matrices.translate(-0.5, 0, -0.5);
		blocks.renderBlockAsEntity(LANTERN, matrices, vertices, 0xF000F0, OverlayTexture.DEFAULT_UV);
		matrices.translate(0, 1, 0);
		blocks.renderBlockAsEntity(CHAIN, matrices, vertices, light, OverlayTexture.DEFAULT_UV);
		matrices.pop();
		super.render(lantern, yaw, tickDelta, matrices, vertices, light);
	}

	@Override
	protected int getBlockLight(TreeLanternEntity lantern, BlockPos pos) {
		return 15;
	}

	@Override
	public Identifier getTexture(TreeLanternEntity lantern) {
		return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
	}
}
