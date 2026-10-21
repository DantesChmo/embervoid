package com.embervoid.client.realm;

import com.embervoid.realm.EmberLodestarItem;
import com.embervoid.realm.RealmItems;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.client.item.CompassAnglePredicateProvider;
import net.minecraft.util.Identifier;

/** Client side of realms and bosses; the one call from {@code EmbervoidClient}. */
public final class RealmClient {
	public static void register() {
		// The Ember Lodestar's needle: a compass that points at the target the server wrote into the stack.
		FabricModelPredicateProviderRegistry.register(RealmItems.EMBER_LODESTAR, new Identifier("angle"),
				new CompassAnglePredicateProvider((world, stack, entity) -> EmberLodestarItem.getTarget(stack)));
	}

	private RealmClient() {}
}
