package com.embervoid.player;

import com.embervoid.config.EmbervoidConfig;
import com.embervoid.stats.Stat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Per-player Embervoid state. Lives on the player as a Fabric data attachment (see {@link PlayerDataManager}):
 * saved in player NBT, kept on death and dimension change, and synced to the owning client.
 *
 * <p>Mutate it on the server only, then call {@link PlayerDataManager#sync}. Other tracks keep their own attachment
 * in their own package instead of adding fields here.
 */
public final class PlayerData {
	public static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.FLOAT.fieldOf("stamina").forGetter(data -> data.stamina),
			Codec.INT.optionalFieldOf("flask_charges").forGetter(data -> Optional.of(data.flaskCharges)),
			Codec.INT.optionalFieldOf("flask_charge_upgrades", 0).forGetter(data -> data.flaskChargeUpgrades),
			Codec.INT.optionalFieldOf("flask_heal_upgrades", 0).forGetter(data -> data.flaskHealUpgrades),
			Codec.BOOL.optionalFieldOf("received_flask", false).forGetter(data -> data.receivedFlask),
			Uuids.INT_STREAM_CODEC.optionalFieldOf("remnant_id").forGetter(data -> Optional.ofNullable(data.remnantId)),
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("stats", Map.of()).forGetter(PlayerData::statsById)
	).apply(instance, (stamina, charges, chargeUpgrades, healUpgrades, receivedFlask, remnantId, stats) -> {
		PlayerData data = createDefault();
		data.stamina = stamina;
		charges.ifPresent(value -> data.flaskCharges = value);
		data.flaskChargeUpgrades = chargeUpgrades;
		data.flaskHealUpgrades = healUpgrades;
		data.receivedFlask = receivedFlask;
		data.remnantId = remnantId.orElse(null);
		stats.forEach((id, value) -> {
			Stat stat = Stat.byId(id);
			if (stat != null) {
				data.stats[stat.ordinal()] = Math.max(Stat.MIN, value);
			}
		});
		return data;
	}));

	/** Current stamina; {@link StaminaManager} keeps it within the player's max. */
	public float stamina;
	/** Healing Flask charges left; the flask item only shows and spends these (DESIGN §10.6). */
	public int flaskCharges;
	/** Extra flask charges from upgrades. */
	public int flaskChargeUpgrades;
	/** Extra flask heal from upgrades. */
	public int flaskHealUpgrades;
	/** Whether the player got their starting Healing Flask. */
	public boolean receivedFlask;
	/** The player's current Ash Remnant, or null if there is none. */
	@Nullable
	public UUID remnantId;

	/**
	 * Stat values by {@link Stat#ordinal()}, all {@link Stat#BASE} at the start. Read and change them through
	 * {@code PlayerStats}, which also applies their effects.
	 */
	public final int[] stats = new int[Stat.COUNT];

	/** Ticks until stamina starts regenerating. Server-only, not saved or synced. */
	public int staminaRegenDelay;

	/** Time of day when the player lay down at night, or -1. Server-only, not saved or synced. */
	public long sleepStartTime = -1;

	/** Last spot the player stood on safe ground, where their remnant goes. Server-only, not saved or synced. */
	@Nullable
	public BlockPos lastSafePos;
	@Nullable
	public RegistryKey<World> lastSafeWorld;

	private PlayerData() {
		Arrays.fill(stats, Stat.BASE);
	}

	private Map<String, Integer> statsById() {
		Map<String, Integer> map = new LinkedHashMap<>();
		for (Stat stat : Stat.values()) {
			map.put(stat.id(), stats[stat.ordinal()]);
		}
		return map;
	}

	/** State of a player who has never joined with the mod: full stamina and flask, no upgrades, no remnant. */
	public static PlayerData createDefault() {
		PlayerData data = new PlayerData();
		data.stamina = EmbervoidConfig.get().stamina().max();
		data.flaskCharges = EmbervoidConfig.get().flask().baseCharges();
		return data;
	}

	public void write(PacketByteBuf buf) {
		buf.writeFloat(stamina);
		buf.writeVarInt(flaskCharges);
		buf.writeVarInt(flaskChargeUpgrades);
		buf.writeVarInt(flaskHealUpgrades);
		buf.writeBoolean(receivedFlask);
		buf.writeNullable(remnantId, PacketByteBuf::writeUuid);
		for (int value : stats) {
			buf.writeVarInt(value);
		}
	}

	public static PlayerData read(PacketByteBuf buf) {
		PlayerData data = new PlayerData();
		data.stamina = buf.readFloat();
		data.flaskCharges = buf.readVarInt();
		data.flaskChargeUpgrades = buf.readVarInt();
		data.flaskHealUpgrades = buf.readVarInt();
		data.receivedFlask = buf.readBoolean();
		data.remnantId = buf.readNullable(PacketByteBuf::readUuid);
		for (int i = 0; i < Stat.COUNT; i++) {
			data.stats[i] = buf.readVarInt();
		}
		return data;
	}

	@Override
	public String toString() {
		return "stamina=" + stamina + ", flaskCharges=" + flaskCharges + ", flaskChargeUpgrades=" + flaskChargeUpgrades
				+ ", flaskHealUpgrades=" + flaskHealUpgrades + ", receivedFlask=" + receivedFlask + ", remnantId=" + remnantId + ", stats=" + statsById();
	}
}
