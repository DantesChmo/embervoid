package com.embervoid.mixin.client;

import com.embervoid.client.mob.MobLevelClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws mob levels next to entity names (see {@link MobLevelClient}). */
@Mixin(EntityRenderer.class)
abstract class EntityRendererMobLevelMixin {
	@Shadow
	protected abstract boolean hasLabel(Entity entity);

	@Shadow
	protected abstract void renderLabelIfPresent(Entity entity, Text text, MatrixStack matrices,
			VertexConsumerProvider vertexConsumers, int light);

	/** Name tags vanilla already draws (custom names) get the level appended. */
	@ModifyArg(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/render/entity/EntityRenderer;renderLabelIfPresent(Lnet/minecraft/entity/Entity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"),
			index = 1)
	private Text embervoid$appendLevel(Entity entity, Text text, MatrixStack matrices,
			VertexConsumerProvider vertexConsumers, int light) {
		return MobLevelClient.withLevel(entity, text);
	}

	/** Otherwise the looked-at leveled mob gets a "Name Lv. N" label. */
	@Inject(method = "render", at = @At("RETURN"))
	private void embervoid$levelLabel(Entity entity, float yaw, float tickDelta, MatrixStack matrices,
			VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
		if (!hasLabel(entity) && MobLevelClient.isLookedAt(entity)) {
			renderLabelIfPresent(entity, MobLevelClient.withLevel(entity, entity.getDisplayName()), matrices, vertexConsumers, light);
		}
	}
}
