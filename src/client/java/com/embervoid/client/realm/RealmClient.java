package com.embervoid.client.realm;

import com.embervoid.boss.BossEntities;
import com.embervoid.client.reach.world.ZoneFog;
import com.embervoid.realm.EmberLodestarItem;
import com.embervoid.realm.RealmItems;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.client.item.CompassAnglePredicateProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.ZombieEntityRenderer;
import net.minecraft.util.Identifier;

/** Client side of realms and bosses; the one call from {@code EmbervoidClient}. */
public final class RealmClient {
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void register() {
		// The Ember Lodestar's needle: a compass that points at the target the server wrote into the stack.
		FabricModelPredicateProviderRegistry.register(RealmItems.EMBER_LODESTAR, new Identifier("angle"),
				new CompassAnglePredicateProvider((world, stack, entity) -> EmberLodestarItem.getTarget(stack)));

		// The test boss borrows the zombie model.
		EntityRendererRegistry.register(BossEntities.TEST_BOSS, context -> (EntityRenderer) new ZombieEntityRenderer(context));
		BossMusicClient.register();

		// Realm fog (PLAN 9.1): the zone fog, and nothing carried over to the next server.
		ZoneFog.register();
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(RealmFog::reset));
		// The ember fog curtain on the final zone's borders (PLAN 21.2), for players the lock keeps out.
		FogCurtain.register();
	}

	private RealmClient() {}
}
