package com.embervoid.registry;

import com.embervoid.Embervoid;
import com.embervoid.item.DebugSweepItem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;

/** Items for testing systems during development. Registered only in a dev environment (runClient/runServer). */
public final class DebugItems {
	public static void register() {
		if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
			return;
		}
		Embervoid.LOGGER.debug("Registering debug items");
		ModItems.register("debug_sweep", new DebugSweepItem(new Item.Settings().maxCount(1)));
	}

	private DebugItems() {}
}
