package com.embervoid.reach.world;

import com.embervoid.realm.RealmDefinition;
import com.embervoid.realm.RealmLayout;
import com.embervoid.realm.RealmMobLevels;
import com.embervoid.realm.Realms;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zone names (PLAN 9.3): crossing into another zone of a realm shows its name as a title, like a Souls area name.
 * Keys {@code embervoid.zone.<realm world path>.<landing|zone_a|zone_b|…|depths>}; nothing past the abyss edge.
 */
public final class ZoneTitles {
	private static final int CHECK_EVERY = 10;
	private static final Map<UUID, String> CURRENT = new ConcurrentHashMap<>();

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(ZoneTitles::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> CURRENT.remove(handler.player.getUuid()));
	}

	/** Lang key of the zone the player stands in, or null outside realms and past the abyss edge. */
	@Nullable
	public static String zoneKey(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		RealmDefinition realm = Realms.byWorld(world.getRegistryKey());
		if (realm == null) {
			return null;
		}
		String prefix = "embervoid.zone." + realm.world().getValue().getPath() + ".";
		if (realm.depths().equals(world.getRegistryKey())) {
			return prefix + "depths";
		}
		int zone = RealmMobLevels.zoneAt(world, player.getBlockPos());
		return switch (zone) {
			case RealmLayout.LANDING -> prefix + "landing";
			case RealmLayout.ABYSS -> null;
			default -> prefix + "zone_" + (char) ('a' + zone);
		};
	}

	/** The zone whose title the player was last shown (null if none yet), for tests. */
	@Nullable
	public static String shownZone(ServerPlayerEntity player) {
		return CURRENT.get(player.getUuid());
	}

	private static void tick(MinecraftServer server) {
		if (server.getTicks() % CHECK_EVERY != 0) {
			return;
		}
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			String key = zoneKey(player);
			if (key == null) {
				if (!Realms.isRealm(player.getServerWorld())) {
					CURRENT.remove(player.getUuid());
				}
				continue;
			}
			if (!Objects.equals(CURRENT.put(player.getUuid(), key), key)) {
				show(player, key);
			}
		}
	}

	private static void show(ServerPlayerEntity player, String key) {
		player.networkHandler.sendPacket(new TitleFadeS2CPacket(15, 50, 25));
		player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.translatable(key).formatted(Formatting.GRAY)));
		player.networkHandler.sendPacket(new TitleS2CPacket(Text.empty()));
	}

	private ZoneTitles() {}
}
