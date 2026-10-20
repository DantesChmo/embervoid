package com.embervoid.registry;

import com.embervoid.Embervoid;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public final class ModItemGroups {
	public static final ItemGroup EMBERVOID = Registry.register(Registries.ITEM_GROUP, Embervoid.id("main"),
			FabricItemGroup.builder()
					.displayName(Text.translatable("itemGroup.embervoid.main"))
					// TODO: replace with a mod item once one exists
					.icon(() -> new ItemStack(Items.SOUL_CAMPFIRE))
					.entries((context, entries) -> ModItems.ALL.forEach(entries::add))
					.build());

	public static void register() {
		Embervoid.LOGGER.debug("Registering item groups");
	}

	private ModItemGroups() {}
}
