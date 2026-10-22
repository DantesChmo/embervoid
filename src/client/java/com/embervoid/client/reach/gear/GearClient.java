package com.embervoid.client.reach.gear;

import com.embervoid.reach.gear.FlaskEmberItem;
import com.embervoid.reach.gear.GearConsumables;
import com.embervoid.reach.gear.GearWeapons;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Client side of the reach-gear track: the Huntsman's Crossbow model states (as vanilla's crossbow has them), the Ash
 * Bomb renderer, the Flask Ember sync and the "resin / oil burning" tooltip lines.
 */
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

		EntityRendererRegistry.register(GearConsumables.ASH_BOMB_ENTITY, FlyingItemEntityRenderer::new);

		ClientPlayNetworking.registerGlobalReceiver(FlaskEmberItem.SYNC_PACKET, (client, handler, buf, responseSender) -> {
			boolean used = buf.readBoolean();
			client.execute(() -> FlaskEmberItem.clientUsed = used);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> FlaskEmberItem.clientUsed = false);

		ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.world == null) {
				return;
			}
			long time = client.world.getTime();
			if (GearConsumables.resinBurning(stack, time)) {
				lines.add(Text.translatable("embervoid.gear.resin.burning", seconds(stack, GearConsumables.RESIN_KEY, time))
						.formatted(Formatting.GOLD));
			}
			if (GearConsumables.lanternBurning(stack, time)) {
				lines.add(Text.translatable("embervoid.gear.oil.burning", (seconds(stack, GearConsumables.OIL_KEY, time) + 59) / 60)
						.formatted(Formatting.GOLD));
			}
		});
	}

	private static long seconds(ItemStack stack, String key, long time) {
		return Math.max(0, (stack.getNbt().getLong(key) - time) / 20);
	}

	private GearClient() {}
}
