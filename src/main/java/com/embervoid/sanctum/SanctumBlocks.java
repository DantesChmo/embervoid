package com.embervoid.sanctum;

import com.embervoid.Embervoid;
import com.embervoid.registry.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/** Sanctum blocks. All unbreakable and spawn-proof: they only come from the structure (or creative). */
public final class SanctumBlocks {
	public static final Block EMBER_SHRINE = ModBlocks.register("ember_shrine", new EmberShrineBlock(core()
			.luminance(state -> 15).nonOpaque()));
	public static final Block VOID_OBELISK = ModBlocks.register("void_obelisk", new VoidObeliskBlock(core()
			.luminance(state -> 4).nonOpaque()));

	/** Floor of a Sanctum core. Standing on it attunes a player ({@link Attunement}); nothing hostile spawns on it. */
	public static final Block SANCTUM_FLAGSTONE = ModBlocks.register("sanctum_flagstone", new Block(core()));

	/** The fog in the doorway of a Ruined Sanctum's boss arena; no item, it only comes from the structure. */
	public static final Block FOG_WALL = ModBlocks.registerWithoutItem("fog_wall", new FogWallBlock(
			AbstractBlock.Settings.create()
					.noCollision()
					.nonOpaque()
					// Counts as solid so flowing water does not wash it away.
					.solid()
					.strength(-1.0f, 3_600_000.0f)
					.dropsNothing()
					.pistonBehavior(PistonBehavior.BLOCK)
					.allowsSpawning((state, world, pos, type) -> false)));

	public static final BlockEntityType<VoidObeliskBlockEntity> VOID_OBELISK_ENTITY = Registry.register(
			Registries.BLOCK_ENTITY_TYPE, Embervoid.id("void_obelisk"),
			FabricBlockEntityTypeBuilder.create(VoidObeliskBlockEntity::new, VOID_OBELISK).build());

	/** Unbreakable like bedrock, immune to explosions, mobs never spawn on it. */
	static AbstractBlock.Settings core() {
		return AbstractBlock.Settings.copy(Blocks.POLISHED_DEEPSLATE)
				.strength(-1.0f, 3_600_000.0f)
				.dropsNothing()
				.allowsSpawning((state, world, pos, type) -> false);
	}

	public static void register() {
		Embervoid.LOGGER.debug("Registering sanctum blocks");
	}

	private SanctumBlocks() {}
}
