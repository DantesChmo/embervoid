package com.embervoid.player;

import com.embervoid.config.EmbervoidConfig;
import com.embervoid.stats.Stat;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The player data's save (without the items kept on death, which need the item registry) and its sync packet. */
class PlayerDataTest {
	private static final UUID REMNANT = new UUID(3, 4);

	private static PlayerData sample() {
		PlayerData data = PlayerData.createDefault();
		data.stamina = 42.5f;
		data.flaskCharges = 2;
		data.flaskChargeUpgrades = 3;
		data.flaskHealUpgrades = 1;
		data.receivedFlask = true;
		data.remnantId = REMNANT;
		for (Stat stat : Stat.values()) {
			data.stats[stat.ordinal()] = 11 + stat.ordinal();
		}
		return data;
	}

	private static NbtElement save(PlayerData data) {
		return PlayerData.FIELDS_CODEC.codec().encodeStart(NbtOps.INSTANCE, data).getOrThrow(false, AssertionError::new);
	}

	private static PlayerData load(NbtElement nbt) {
		return PlayerData.FIELDS_CODEC.codec().parse(NbtOps.INSTANCE, nbt).getOrThrow(false, AssertionError::new);
	}

	private static void assertSameData(PlayerData expected, PlayerData actual) {
		assertEquals(expected.stamina, actual.stamina);
		assertEquals(expected.flaskCharges, actual.flaskCharges);
		assertEquals(expected.flaskChargeUpgrades, actual.flaskChargeUpgrades);
		assertEquals(expected.flaskHealUpgrades, actual.flaskHealUpgrades);
		assertEquals(expected.receivedFlask, actual.receivedFlask);
		assertEquals(expected.remnantId, actual.remnantId);
		assertArrayEquals(expected.stats, actual.stats);
	}

	@Test
	void everySavedFieldSurvivesASave() {
		PlayerData data = sample();
		assertSameData(data, load(save(data)));
	}

	@Test
	void noRemnantStaysNone() {
		PlayerData data = sample();
		data.remnantId = null;
		assertNull(load(save(data)).remnantId);
	}

	@Test
	void aSaveWithOnlyStaminaGetsAFreshPlayersFlask() {
		NbtCompound nbt = new NbtCompound();
		nbt.putFloat("stamina", 7);
		PlayerData loaded = load(nbt);
		assertEquals(7, loaded.stamina);
		assertEquals(EmbervoidConfig.get().flask().baseCharges(), loaded.flaskCharges);
		assertEquals(0, loaded.flaskChargeUpgrades);
		assertEquals(0, loaded.flaskHealUpgrades);
		assertFalse(loaded.receivedFlask);
		assertNull(loaded.remnantId);
		for (int value : loaded.stats) {
			assertEquals(Stat.BASE, value);
		}
	}

	@Test
	void unknownStatIdsAreIgnored() {
		NbtCompound nbt = (NbtCompound) save(sample());
		NbtCompound stats = nbt.getCompound("stats");
		stats.putInt("charisma", 50);
		PlayerData loaded = load(nbt);
		assertArrayEquals(sample().stats, loaded.stats);
	}

	@Test
	void aMissingStatStaysAtItsStart() {
		NbtCompound nbt = (NbtCompound) save(sample());
		nbt.getCompound("stats").remove(Stat.VITALITY.id());
		assertEquals(Stat.BASE, load(nbt).stats[Stat.VITALITY.ordinal()]);
	}

	@Test
	void statsBelowTheMinimumAreRaisedToIt() {
		NbtCompound nbt = (NbtCompound) save(sample());
		nbt.getCompound("stats").putInt(Stat.STRENGTH.id(), -5);
		nbt.getCompound("stats").putInt(Stat.DEXTERITY.id(), 0);
		PlayerData loaded = load(nbt);
		assertEquals(Stat.MIN, loaded.stats[Stat.STRENGTH.ordinal()]);
		assertEquals(Stat.MIN, loaded.stats[Stat.DEXTERITY.ordinal()]);
		assertEquals(sample().stats[Stat.VITALITY.ordinal()], loaded.stats[Stat.VITALITY.ordinal()]);
	}

	@Test
	void theSyncPacketCarriesEverySyncedField() {
		PlayerData data = sample();
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		data.write(buf);
		PlayerData read = PlayerData.read(buf);
		assertSameData(data, read);
		assertFalse(buf.isReadable(), "bytes left over");
	}

	@Test
	void theSyncPacketCarriesNoRemnant() {
		PlayerData data = sample();
		data.remnantId = null;
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		data.write(buf);
		assertNull(PlayerData.read(buf).remnantId);
	}

	@Test
	void aFreshPlayerHasFullStaminaAndBaseFlask() {
		PlayerData data = PlayerData.createDefault();
		assertEquals(EmbervoidConfig.get().stamina().max(), data.stamina);
		assertEquals(EmbervoidConfig.get().flask().baseCharges(), data.flaskCharges);
		assertTrue(data.keptOnDeath.isEmpty());
	}
}
