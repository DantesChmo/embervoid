package com.embervoid.realm;

import com.embervoid.Embervoid;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.GlobalPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-player realm travel state, server-side only: the last Ember Shrine rested at in each realm dimension (the respawn
 * point there). The way out of a realm is the hub ({@link com.embervoid.hub.HubTravel}); the {@code return_point} of
 * 0.4.x saves is ignored. Progress (bosses, realms) is not here; see {@link com.embervoid.progress.ProgressManager}.
 */
public final class RealmPlayerData {
	// Unknown keys are skipped, so the old return_point / return_yaw are dropped on load.
	public static final Codec<RealmPlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.unboundedMap(Identifier.CODEC, GlobalPos.CODEC).optionalFieldOf("shrines", Map.of()).forGetter(data -> data.shrines)
	).apply(instance, RealmPlayerData::new));

	// Persistent and copied on death: a realm death must still know the shrine.
	@SuppressWarnings("UnstableApiUsage")
	public static final AttachmentType<RealmPlayerData> ATTACHMENT = AttachmentRegistry.<RealmPlayerData>builder()
			.persistent(CODEC)
			.initializer(RealmPlayerData::new)
			.copyOnDeath()
			.buildAndRegister(Embervoid.id("realm_player"));

	/** Realm dimension id → last shrine block rested at in that dimension (open zones and final zone apart). */
	public final Map<Identifier, GlobalPos> shrines;

	public RealmPlayerData() {
		this(Map.of());
	}

	private RealmPlayerData(Map<Identifier, GlobalPos> shrines) {
		this.shrines = new HashMap<>(shrines);
	}

	/** Loads the class so the attachment type is registered before any player data is read. */
	public static void register() {
	}

	@SuppressWarnings("UnstableApiUsage")
	public static RealmPlayerData get(PlayerEntity player) {
		return player.getAttachedOrCreate(ATTACHMENT);
	}

	@Override
	public String toString() {
		return "RealmPlayerData{shrines=" + shrines + "}";
	}
}
