package com.embervoid.player;

import com.embervoid.config.EmbervoidConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Uuids;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Per-player Embervoid state. Lives on the player as a Fabric data attachment (see {@link PlayerDataManager}):
 * saved in player NBT, kept on death and dimension change, and synced to the owning client.
 *
 * <p>Mutate it on the server only, then call {@link PlayerDataManager#sync}.
 */
public final class PlayerData {
	public static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.FLOAT.fieldOf("stamina").forGetter(data -> data.stamina),
			Codec.INT.optionalFieldOf("flask_charge_upgrades", 0).forGetter(data -> data.flaskChargeUpgrades),
			Codec.INT.optionalFieldOf("flask_heal_upgrades", 0).forGetter(data -> data.flaskHealUpgrades),
			Uuids.INT_STREAM_CODEC.optionalFieldOf("remnant_id").forGetter(data -> Optional.ofNullable(data.remnantId))
	).apply(instance, (stamina, chargeUpgrades, healUpgrades, remnantId) ->
			new PlayerData(stamina, chargeUpgrades, healUpgrades, remnantId.orElse(null))));

	/** Current stamina; the stamina system (task 2.3) keeps it within {@code stamina.max}. */
	public float stamina;
	/** Extra flask charges from upgrades. */
	public int flaskChargeUpgrades;
	/** Extra flask heal from upgrades. */
	public int flaskHealUpgrades;
	/** The player's current Ash Remnant, or null if there is none. */
	@Nullable
	public UUID remnantId;

	public PlayerData(float stamina, int flaskChargeUpgrades, int flaskHealUpgrades, @Nullable UUID remnantId) {
		this.stamina = stamina;
		this.flaskChargeUpgrades = flaskChargeUpgrades;
		this.flaskHealUpgrades = flaskHealUpgrades;
		this.remnantId = remnantId;
	}

	/** State of a player who has never joined with the mod: full stamina, no upgrades, no remnant. */
	public static PlayerData createDefault() {
		return new PlayerData(EmbervoidConfig.get().stamina().max(), 0, 0, null);
	}

	public void write(PacketByteBuf buf) {
		buf.writeFloat(stamina);
		buf.writeVarInt(flaskChargeUpgrades);
		buf.writeVarInt(flaskHealUpgrades);
		buf.writeNullable(remnantId, PacketByteBuf::writeUuid);
	}

	public static PlayerData read(PacketByteBuf buf) {
		return new PlayerData(buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readNullable(PacketByteBuf::readUuid));
	}

	@Override
	public String toString() {
		return "stamina=" + stamina + ", flaskChargeUpgrades=" + flaskChargeUpgrades
				+ ", flaskHealUpgrades=" + flaskHealUpgrades + ", remnantId=" + remnantId;
	}
}
