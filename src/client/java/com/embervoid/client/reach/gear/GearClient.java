package com.embervoid.client.reach.gear;

import com.embervoid.reach.gear.GearWeapons;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

/** Client side of the reach-gear track: the Huntsman's Crossbow model states, as vanilla's crossbow has them. */
public final class GearClient {
	public static void register() {
		Item crossbow = GearWeapons.HUNTSMANS_CROSSBOW;
		FabricModelPredicateProviderRegistry.register(crossbow, new Identifier("pull"), (stack, world, entity, seed) ->
				entity == null || CrossbowItem.isCharged(stack) ? 0
						: (stack.getMaxUseTime() - entity.getItemUseTimeLeft()) / (float) CrossbowItem.getPullTime(stack));
		FabricModelPredicateProviderRegistry.register(crossbow, new Identifier("pulling"), (stack, world, entity, seed) ->
				entity != null && entity.isUsingItem() && entity.getActiveItem() == stack && !CrossbowItem.isCharged(stack) ? 1 : 0);
		FabricModelPredicateProviderRegistry.register(crossbow, new Identifier("charged"), (stack, world, entity, seed) ->
				entity != null && CrossbowItem.isCharged(stack) ? 1 : 0);
		FabricModelPredicateProviderRegistry.register(crossbow, new Identifier("firework"), (stack, world, entity, seed) ->
				entity != null && CrossbowItem.isCharged(stack) && CrossbowItem.hasProjectile(stack, Items.FIREWORK_ROCKET) ? 1 : 0);
	}

	private GearClient() {}
}
