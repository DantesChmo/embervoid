package com.embervoid.progress;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Per-player progression: attunement to the Ember Sanctum, unlocked and completed realms, beaten bosses.
 * Lives on the player as its own data attachment (see {@link ProgressManager}).
 *
 * <p>Realms and bosses are plain ids owned by the realms track (e.g. {@code embervoid:realm_1}). Mutate through
 * {@link ProgressManager} on the server so the change is synced; the getters here are safe on both sides.
 */
public final class PlayerProgress {
	public static final Codec<PlayerProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("attuned", false).forGetter(p -> p.attuned),
			Identifier.CODEC.listOf().optionalFieldOf("unlocked_realms", List.of()).forGetter(p -> List.copyOf(p.unlockedRealms)),
			Identifier.CODEC.listOf().optionalFieldOf("completed_realms", List.of()).forGetter(p -> List.copyOf(p.completedRealms)),
			Identifier.CODEC.listOf().optionalFieldOf("beaten_bosses", List.of()).forGetter(p -> List.copyOf(p.beatenBosses))
	).apply(instance, PlayerProgress::new));

	boolean attuned;
	// Insertion-ordered so saves and debug output are stable.
	final Set<Identifier> unlockedRealms;
	final Set<Identifier> completedRealms;
	final Set<Identifier> beatenBosses;

	public PlayerProgress(boolean attuned, List<Identifier> unlockedRealms, List<Identifier> completedRealms, List<Identifier> beatenBosses) {
		this.attuned = attuned;
		this.unlockedRealms = new LinkedHashSet<>(unlockedRealms);
		this.completedRealms = new LinkedHashSet<>(completedRealms);
		this.beatenBosses = new LinkedHashSet<>(beatenBosses);
	}

	/** A player who has never reached a Sanctum core. */
	public static PlayerProgress createDefault() {
		return new PlayerProgress(false, List.of(), List.of(), List.of());
	}

	public boolean isAttuned() {
		return attuned;
	}

	public boolean isRealmUnlocked(Identifier realm) {
		return unlockedRealms.contains(realm);
	}

	public boolean isRealmCompleted(Identifier realm) {
		return completedRealms.contains(realm);
	}

	public boolean hasBeatenBoss(Identifier boss) {
		return beatenBosses.contains(boss);
	}

	public Set<Identifier> unlockedRealms() {
		return Collections.unmodifiableSet(unlockedRealms);
	}

	public Set<Identifier> completedRealms() {
		return Collections.unmodifiableSet(completedRealms);
	}

	public Set<Identifier> beatenBosses() {
		return Collections.unmodifiableSet(beatenBosses);
	}

	public void write(PacketByteBuf buf) {
		buf.writeBoolean(attuned);
		buf.writeCollection(unlockedRealms, PacketByteBuf::writeIdentifier);
		buf.writeCollection(completedRealms, PacketByteBuf::writeIdentifier);
		buf.writeCollection(beatenBosses, PacketByteBuf::writeIdentifier);
	}

	public static PlayerProgress read(PacketByteBuf buf) {
		return new PlayerProgress(buf.readBoolean(),
				buf.readCollection(ArrayList::new, PacketByteBuf::readIdentifier),
				buf.readCollection(ArrayList::new, PacketByteBuf::readIdentifier),
				buf.readCollection(ArrayList::new, PacketByteBuf::readIdentifier));
	}

	@Override
	public String toString() {
		return "attuned=" + attuned + ", unlockedRealms=" + unlockedRealms
				+ ", completedRealms=" + completedRealms + ", beatenBosses=" + beatenBosses;
	}
}
