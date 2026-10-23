package com.embervoid.block.prop;

import net.minecraft.block.BlockState;

/**
 * A prop block (PLAN 20.3). Each state names its model under models/block/prop: datagen writes the blockstate from
 * it, turned by the facing, and the hitbox is that model's {@link PropShapes} entry, so the two cannot drift apart.
 */
public interface Prop {
	String model(BlockState state);

	/** The model the item shows (a two-block prop: both halves). */
	String itemModel();
}
