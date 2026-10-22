package com.embervoid.gametest;

import com.embervoid.Embervoid;
import com.embervoid.reach.gear.GearItems;
import com.embervoid.reach.gear.GearLootTables;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.item.Item;
import net.minecraft.loot.LootTable;
import net.minecraft.recipe.Recipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;

/** Ashen Reach gear (PLAN milestone 11): recipes, loot tables and item behaviour. */
public final class GearTests implements FabricGameTest {
	/** The fixed chest table ids load (other tracks' structures point at them) and the 11.1 recipes exist. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void materialRecipesAndChestTablesLoad(TestContext context) {
		MinecraftServer server = context.getWorld().getServer();
		for (Identifier id : GearLootTables.all()) {
			context.assertTrue(server.getLootManager().getLootTable(id) != LootTable.EMPTY, "missing chest table " + id);
		}
		assertRecipe(context, "smoldering_ingot", GearItems.SMOLDERING_INGOT);
		assertRecipe(context, "ash_of_rematch", GearItems.ASH_OF_REMATCH);
		assertRecipe(context, "grave_iron_ingot_from_smelting_raw_grave_iron", GearItems.GRAVE_IRON_INGOT);
		assertRecipe(context, "grave_iron_ingot_from_blasting_raw_grave_iron", GearItems.GRAVE_IRON_INGOT);
		context.complete();
	}

	static void assertRecipe(TestContext context, String name, Item output) {
		MinecraftServer server = context.getWorld().getServer();
		Recipe<?> recipe = server.getRecipeManager().get(Embervoid.id(name)).orElse(null);
		context.assertTrue(recipe != null, "missing recipe " + name);
		context.assertTrue(recipe.getOutput(server.getRegistryManager()).isOf(output), "recipe " + name + " makes the wrong item");
	}
}
