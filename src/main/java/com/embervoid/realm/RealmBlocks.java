package com.embervoid.realm;

import com.embervoid.registry.ModBlocks;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

/** Realm blocks. Like everything a realm is anchored on, they cannot be broken (DESIGN §10.1). */
public final class RealmBlocks {
	public static final Block RETURN_OBELISK = ModBlocks.register("return_obelisk",
			new ReturnObeliskBlock(unbreakable().luminance(state -> 7)));
	/** The Citadel Gate at the landing plaza (the Sealed Gate until 0.5.0, DESIGN §4.1). */
	public static final Block CITADEL_GATE = ModBlocks.register("citadel_gate",
			new CitadelGateBlock(unbreakable().luminance(state -> 3)));

	private static AbstractBlock.Settings unbreakable() {
		return AbstractBlock.Settings.copy(Blocks.REINFORCED_DEEPSLATE).strength(-1.0f, 3_600_000.0f).dropsNothing();
	}

	public static void register() {
	}

	private RealmBlocks() {}
}
