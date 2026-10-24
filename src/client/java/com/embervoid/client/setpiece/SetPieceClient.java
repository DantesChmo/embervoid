package com.embervoid.client.setpiece;

import com.embervoid.block.setpiece.SetPieceBlockEntity;
import com.embervoid.block.setpiece.SetPieces;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent;

/**
 * Client side of the set pieces (PLAN 20.1): the renderer, and the sound keyframes of the animation files played as
 * vanilla sounds at the piece ({@code creak}, {@code toll}, {@code chain}, {@code slam}, {@code grind}, {@code clink}).
 */
public final class SetPieceClient {
	public static void register() {
		BlockEntityRendererFactories.register(SetPieces.BLOCK_ENTITY, context -> new SetPieceRenderer());
		SetPieceBlockEntity.soundHandler = SetPieceClient::playKeyframe;
	}

	private static void playKeyframe(SoundKeyframeEvent<SetPieceBlockEntity> event) {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientWorld world = client.world;
		if (world == null || client.player == null) {
			return;
		}
		BlockPos pos = event.getAnimatable().getPos();
		String word = event.getKeyframeData().getSound();
		float pitch = 0.9f + world.random.nextFloat() * 0.2f;
		switch (word) {
			case "creak" -> play(world, pos, SoundEvents.BLOCK_WOODEN_DOOR_OPEN, 0.5f, 0.45f * pitch);
			case "toll" -> play(world, pos, SoundEvents.BLOCK_BELL_USE, 2.5f, 0.55f * pitch);
			case "chain" -> play(world, pos, SoundEvents.BLOCK_CHAIN_PLACE, 0.8f, 0.8f * pitch);
			case "slam" -> play(world, pos, SoundEvents.BLOCK_ANVIL_LAND, 1.2f, 0.7f * pitch);
			case "grind" -> play(world, pos, SoundEvents.BLOCK_GRINDSTONE_USE, 0.7f, 0.6f * pitch);
			case "clink" -> play(world, pos, SoundEvents.BLOCK_CHAIN_HIT, 0.5f, 1.2f * pitch);
			case "wood" -> play(world, pos, SoundEvents.BLOCK_WOOD_HIT, 0.6f, 0.7f * pitch);
			default -> {
			}
		}
	}

	private static void play(ClientWorld world, BlockPos pos, SoundEvent sound, float volume, float pitch) {
		world.playSound(MinecraftClient.getInstance().player, pos, sound, SoundCategory.BLOCKS, volume, pitch);
	}

	private SetPieceClient() {}
}
