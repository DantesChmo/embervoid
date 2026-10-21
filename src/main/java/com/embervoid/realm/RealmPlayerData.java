package com.embervoid.realm;

import com.embervoid.Embervoid;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.GlobalPos;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Per-player realm travel state, server-side only: where to return when leaving a realm, and the last Ember Shrine
 * rested at in each realm (the realm respawn point). Progress (bosses, realms) is not here; see
 * {@link com.embervoid.progress.ProgressManager}.
 */
public final class RealmPlayerData {
	public static final Codec<RealmPlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			GlobalPos.CODEC.optionalFieldOf("return_point").forGetter(data -> Optional.ofNullable(data.returnPoint)),
			Codec.FLOAT.optionalFieldOf("return_yaw", 0f).forGetter(data -> data.returnYaw),
			Codec.unboundedMap(Identifier.CODEC, GlobalPos.CODEC).optionalFieldOf("shrines", Map.of()).forGetter(data -> data.shrines)
	).apply(instance, (returnPoint, returnYaw, shrines) -> new RealmPlayerData(returnPoint.orElse(null), returnYaw, shrines)));

	// Persistent and copied on death: a realm death must still know the shrine and the way home.
	@SuppressWarnings("UnstableApiUsage")
	public static final AttachmentType<RealmPlayerData> ATTACHMENT = AttachmentRegistry.<RealmPlayerData>builder()
			.persistent(CODEC)
			.initializer(RealmPlayerData::new)
			.copyOnDeath()
			.buildAndRegister(Embervoid.id("realm_player"));

	/** Where the player entered the current realm from (a Sanctum), or null. */
	@Nullable
	public GlobalPos returnPoint;
	public float returnYaw;
	/** Realm id → last rested shrine block in that realm (either of its dimensions). */
	public final Map<Identifier, GlobalPos> shrines;

	public RealmPlayerData() {
		this(null, 0, Map.of());
	}

	private RealmPlayerData(@Nullable GlobalPos returnPoint, float returnYaw, Map<Identifier, GlobalPos> shrines) {
		this.returnPoint = returnPoint;
		this.returnYaw = returnYaw;
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
		return "RealmPlayerData{returnPoint=" + returnPoint + ", shrines=" + shrines + "}";
	}
}
