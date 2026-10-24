package com.embervoid.mixin.client;

import com.embervoid.client.reach.gear.InHandModels;
import com.embervoid.client.reach.gear.ScourgeLashes;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The weapons' in-hand models (PLAN 17.2, 17.4): in a hand each is drawn as its big 3D model ({@link InHandModels}),
 * the way vanilla swaps the trident's and the spyglass's models (the icon stays for the GUI, the ground and item
 * frames), and the Scourge's lash ({@link ScourgeLashes}) learns where its handle is drawn and whose hand it is in.
 */
@Mixin(ItemRenderer.class)
abstract class GearItemRendererMixin {
	private static final String RENDER_HELD = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;"
			+ "Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;"
			+ "Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V";
	private static final String RENDER_MODEL = "renderItem(Lnet/minecraft/item/ItemStack;"
			+ "Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;"
			+ "Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V";

	@Inject(method = RENDER_HELD, at = @At("HEAD"))
	private void embervoid$heldBy(LivingEntity entity, ItemStack stack, ModelTransformationMode mode, boolean leftHanded,
			MatrixStack matrices, VertexConsumerProvider vertexConsumers, World world, int light, int overlay, int seed, CallbackInfo ci) {
		ScourgeLashes.holding(entity);
	}

	@Inject(method = RENDER_HELD, at = @At("RETURN"))
	private void embervoid$heldDone(LivingEntity entity, ItemStack stack, ModelTransformationMode mode, boolean leftHanded,
			MatrixStack matrices, VertexConsumerProvider vertexConsumers, World world, int light, int overlay, int seed, CallbackInfo ci) {
		ScourgeLashes.holding(null);
	}

	@ModifyVariable(method = RENDER_MODEL, at = @At("HEAD"), argsOnly = true)
	private BakedModel embervoid$scourgeHandle(BakedModel model, @Local(argsOnly = true) ItemStack stack,
			@Local(argsOnly = true) ModelTransformationMode mode) {
		BakedModel inHand = InHandModels.modelFor(stack, mode);
		return inHand != null ? inHand : model;
	}

	// Right after the display transform and the move to the model's corner: the pose the handle is drawn with.
	@Inject(method = RENDER_MODEL, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V",
			ordinal = 0, shift = At.Shift.AFTER))
	private void embervoid$scourgeDrawn(ItemStack stack, ModelTransformationMode mode, boolean leftHanded, MatrixStack matrices,
			VertexConsumerProvider vertexConsumers, int light, int overlay, BakedModel model, CallbackInfo ci) {
		ScourgeLashes.drawn(stack, mode, matrices.peek().getPositionMatrix());
	}
}
