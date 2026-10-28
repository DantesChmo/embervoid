package com.embervoid.hub.stele;

import com.embervoid.Embervoid;
import com.embervoid.registry.ModBlocks;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.sound.BlockSoundGroup;

public final class SteleBlocks {
	/** Crafted like stone bricks; kindled, its cracks glow at full brightness and light the area a little. */
	public static final Block CLEFT_STONE = ModBlocks.register("cleft_stone", new CleftStoneBlock(
			AbstractBlock.Settings.copy(Blocks.STONE_BRICKS)
					.mapColor(MapColor.DEEPSLATE_GRAY)
					.luminance(state -> state.get(CleftStoneBlock.KINDLED) ? 7 : 0)
					.emissiveLighting((state, world, pos) -> state.get(CleftStoneBlock.KINDLED))));

	/** No item: it comes only from kindling. Unbreakable, no collision, pistons cannot move it. */
	public static final Block STELE_CRACK = ModBlocks.registerWithoutItem("stele_crack", new SteleCrackBlock(
			AbstractBlock.Settings.create()
					.mapColor(MapColor.ORANGE)
					.noCollision()
					.strength(-1.0f, 3_600_000.0f)
					.dropsNothing()
					.luminance(state -> 11)
					.sounds(BlockSoundGroup.GLASS)
					.pistonBehavior(PistonBehavior.BLOCK)
					.nonOpaque()
					.allowsSpawning((state, world, pos, type) -> false)));

	/** The ritual circle's passage: like the crack, a column of mist over the centre stone. */
	public static final Block STELE_RIFT = ModBlocks.registerWithoutItem("stele_rift", new SteleRiftBlock(
			AbstractBlock.Settings.create()
					.mapColor(MapColor.ORANGE)
					.noCollision()
					.strength(-1.0f, 3_600_000.0f)
					.dropsNothing()
					.luminance(state -> 11)
					.sounds(BlockSoundGroup.GLASS)
					.pistonBehavior(PistonBehavior.BLOCK)
					.nonOpaque()
					.allowsSpawning((state, world, pos, type) -> false)));

	static void register() {
		Embervoid.LOGGER.debug("Registering stele blocks");
	}

	private SteleBlocks() {}
}
