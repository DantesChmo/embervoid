package com.embervoid.block.prop;

import com.embervoid.Embervoid;
import com.embervoid.registry.ModBlocks;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.sound.BlockSoundGroup;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Props of the burnt kingdom: decor with a shape and a hitbox of its own instead of a textured cube.
 * Models and textures from tools/prop_art.py; blockstates, loot and tags from PropDataGen.
 */
public final class Props {
	// Ashen Fields: farms, granaries, the pilgrim camp.
	public static final StackedPropBlock GRAIN_SACKS = register("grain_sacks", StackedPropBlock::new,
			settings(MapColor.BROWN, 0.8f, BlockSoundGroup.WOOL));
	public static final PropBlock BURNT_BARREL = register("burnt_barrel", PropBlock::new,
			settings(MapColor.BLACK, 1.5f, BlockSoundGroup.WOOD));
	public static final LongPropBlock BROKEN_CART = register("broken_cart", LongPropBlock::new,
			settings(MapColor.OAK_TAN, 1.5f, BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY));
	public static final TallPropBlock SCARECROW = register("scarecrow", TallPropBlock::new,
			settings(MapColor.BROWN, 1.0f, BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY));
	// Royal Woods and the outposts: the king's men and his hiders of the flame.
	public static final PropBlock WEAPON_RACK = register("weapon_rack", PropBlock::new,
			settings(MapColor.BROWN, 1.5f, BlockSoundGroup.WOOD));
	public static final HangingPropBlock IRON_CAGE = register("iron_cage", HangingPropBlock::new,
			settings(MapColor.IRON_GRAY, 2.0f, BlockSoundGroup.CHAIN));
	public static final TallPropBlock TATTERED_STANDARD = register("tattered_standard", TallPropBlock::new,
			settings(MapColor.DARK_RED, 1.0f, BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY));
	// Sunken Necropolis: the dead burned without rites.
	public static final LongPropBlock COFFIN = register("coffin", LongPropBlock::new,
			settings(MapColor.TERRACOTTA_BROWN, 1.5f, BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY));
	public static final PropBlock GRAVE_CROSS = register("grave_cross", PropBlock::new,
			settings(MapColor.STONE_GRAY, 1.5f, BlockSoundGroup.STONE));
	public static final PropBlock LEANING_GRAVESTONE = register("leaning_gravestone", PropBlock::new,
			settings(MapColor.STONE_GRAY, 1.5f, BlockSoundGroup.STONE));
	public static final PropBlock BONE_PILE = register("bone_pile", PropBlock::new,
			settings(MapColor.PALE_YELLOW, 1.0f, BlockSoundGroup.BONE));
	public static final StackedPropBlock ASH_URNS = register("ash_urns", StackedPropBlock::new,
			settings(MapColor.TERRACOTTA_ORANGE, 0.5f, BlockSoundGroup.DECORATED_POT));
	public static final PropCandleBlock TALLOW_CANDLES = register("tallow_candles", PropCandleBlock::new,
			AbstractBlock.Settings.copy(Blocks.CANDLE).mapColor(MapColor.PALE_YELLOW));
	// Everywhere, the overworld dungeon too: what fell down.
	public static final PropBlock RUBBLE_PILE = register("rubble_pile", PropBlock::new,
			settings(MapColor.DEEPSLATE_GRAY, 1.5f, BlockSoundGroup.STONE));

	public static final List<Block> ALL = List.of(GRAIN_SACKS, BURNT_BARREL, BROKEN_CART, SCARECROW, WEAPON_RACK, IRON_CAGE,
			TATTERED_STANDARD, COFFIN, GRAVE_CROSS, LEANING_GRAVESTONE, BONE_PILE, ASH_URNS, TALLOW_CANDLES, RUBBLE_PILE);

	public static void register() {
		Embervoid.LOGGER.debug("Registering props");
	}

	private static AbstractBlock.Settings settings(MapColor color, float strength, BlockSoundGroup sounds) {
		return AbstractBlock.Settings.create().mapColor(color).strength(strength).sounds(sounds).nonOpaque();
	}

	private static <T extends Block> T register(String name, BiFunction<AbstractBlock.Settings, String, T> factory,
			AbstractBlock.Settings settings) {
		return ModBlocks.register(name, factory.apply(settings, name));
	}

	private Props() {}
}
