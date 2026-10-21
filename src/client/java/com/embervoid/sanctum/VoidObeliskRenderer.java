package com.embervoid.sanctum;

import com.embervoid.Embervoid;
import com.embervoid.progress.ProgressManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Draws an obelisk's glow for the local player: runes on the shaft and a floating shard, dim when dark,
 * ember-orange when lit, pale gold when kindled. The block model is the same for everyone.
 */
public class VoidObeliskRenderer implements BlockEntityRenderer<VoidObeliskBlockEntity> {
	private static final Identifier RUNES = Embervoid.id("textures/block/void_obelisk_runes.png");
	private static final Identifier CORE = Embervoid.id("textures/block/void_obelisk_core.png");
	private static final int DARK = 0x4A3F5E;
	private static final int LIT = 0xFF8A2A;
	private static final int KINDLED = 0xFFE7A0;

	public VoidObeliskRenderer(BlockEntityRendererFactory.Context context) {
	}

	@Override
	public void render(VoidObeliskBlockEntity obelisk, float tickDelta, MatrixStack matrices,
			VertexConsumerProvider vertexConsumers, int light, int overlay) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || obelisk.getWorld() == null) {
			return;
		}
		VoidObeliskBlock.Glow glow = VoidObeliskBlock.Glow.of(ProgressManager.get(client.player), obelisk.getRealm());
		boolean glowing = glow != VoidObeliskBlock.Glow.DARK;
		int color = switch (glow) {
			case DARK -> DARK;
			case LIT -> LIT;
			case KINDLED -> KINDLED;
		};
		int glowLight = glowing ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light;

		// Runes on the four sides of the shaft (x/z 4..12, y 3..15), just outside the faces.
		VertexConsumer runes = vertexConsumers.getBuffer(glowing
				? RenderLayer.getEntityTranslucentEmissive(RUNES) : RenderLayer.getEntityTranslucent(RUNES));
		matrices.push();
		matrices.translate(0.5, 0, 0.5);
		for (int side = 0; side < 4; side++) {
			matrices.push();
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90 * side));
			quad(runes, matrices, -0.25f, 3 / 16f, 0.2510f, 0.25f, 15 / 16f, color, glowLight);
			matrices.pop();
		}
		matrices.pop();

		// Floating shard above the shaft, turning slowly; only drawn when the realm is open to this player.
		if (glowing) {
			float time = obelisk.getWorld().getTime() + tickDelta;
			VertexConsumer core = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(CORE));
			matrices.push();
			matrices.translate(0.5, 1.25 + Math.sin(time / 20.0) * 0.05, 0.5);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 1.5f));
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45));
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(35));
			cube(core, matrices, 0.12f, color, glowLight);
			matrices.pop();

			Random random = obelisk.getWorld().getRandom();
			if (!client.isPaused() && random.nextInt(40) == 0) {
				obelisk.getWorld().addParticle(glow == VoidObeliskBlock.Glow.KINDLED ? ParticleTypes.END_ROD : ParticleTypes.FLAME,
						obelisk.getPos().getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4,
						obelisk.getPos().getY() + 1.25,
						obelisk.getPos().getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4,
						0, 0.01, 0);
			}
		}
	}

	/** A quad facing +z at depth z, spanning x0..x1, y0..y1, with the whole texture. */
	private static void quad(VertexConsumer consumer, MatrixStack matrices, float x0, float y0, float z, float x1, float y1,
			int color, int light) {
		MatrixStack.Entry entry = matrices.peek();
		Matrix4f position = entry.getPositionMatrix();
		Matrix3f normal = entry.getNormalMatrix();
		int r = color >> 16 & 0xFF;
		int g = color >> 8 & 0xFF;
		int b = color & 0xFF;
		vertex(consumer, position, normal, x0, y0, z, 0, 1, r, g, b, light);
		vertex(consumer, position, normal, x1, y0, z, 1, 1, r, g, b, light);
		vertex(consumer, position, normal, x1, y1, z, 1, 0, r, g, b, light);
		vertex(consumer, position, normal, x0, y1, z, 0, 0, r, g, b, light);
	}

	/** A cube of half-size h centered on the origin, built from six rotated quads. */
	private static void cube(VertexConsumer consumer, MatrixStack matrices, float h, int color, int light) {
		for (int i = 0; i < 6; i++) {
			matrices.push();
			if (i < 4) {
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90 * i));
			} else {
				matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(i == 4 ? 90 : -90));
			}
			quad(consumer, matrices, -h, -h, h, h, h, color, light);
			matrices.pop();
		}
	}

	private static void vertex(VertexConsumer consumer, Matrix4f position, Matrix3f normal, float x, float y, float z,
			float u, float v, int r, int g, int b, int light) {
		consumer.vertex(position, x, y, z).color(r, g, b, 255).texture(u, v)
				.overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal, 0, 0, 1).next();
	}
}
