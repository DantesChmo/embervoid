package com.embervoid.sanctum.leveling;

import net.minecraft.network.PacketByteBuf;

import java.util.List;

/**
 * Everything the level-up screen shows, built on the server and sent to the client: so the client needs no stat
 * formulas or config of its own.
 *
 * @param level    character level
 * @param cost     XP points the next level costs
 * @param xpPoints XP points the player has
 * @param stats    one row per stat, in display order
 */
public record StatSheet(int level, int cost, int xpPoints, List<Stat> stats) {
	/**
	 * @param id      stat id sent back in level-up requests (e.g. "vitality"); its name is {@code embervoid.stat.<id>}
	 * @param value   current value
	 * @param max     cap; no level-up past it
	 * @param derived what this stat drives now and after one more point (bonus points included)
	 * @param bonus   points on top of {@code value} that do not count in the level (worn artifacts)
	 */
	public record Stat(String id, int value, int max, List<Derived> derived, int bonus) {
		public Stat(String id, int value, int max, List<Derived> derived) {
			this(id, value, max, derived, 0);
		}
	}

	/**
	 * One derived value, e.g. max health.
	 *
	 * @param key     translation key of its name
	 * @param current formatted value now
	 * @param next    formatted value with one more point in the stat
	 */
	public record Derived(String key, String current, String next) {}

	public boolean canAfford() {
		return xpPoints >= cost;
	}

	public void write(PacketByteBuf buf) {
		buf.writeVarInt(level);
		buf.writeVarInt(cost);
		buf.writeVarInt(xpPoints);
		buf.writeCollection(stats, (b, stat) -> {
			b.writeString(stat.id);
			b.writeVarInt(stat.value);
			b.writeVarInt(stat.max);
			b.writeCollection(stat.derived, (d, derived) -> {
				d.writeString(derived.key);
				d.writeString(derived.current);
				d.writeString(derived.next);
			});
			b.writeVarInt(stat.bonus);
		});
	}

	public static StatSheet read(PacketByteBuf buf) {
		return new StatSheet(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
				buf.readList(b -> new Stat(b.readString(), b.readVarInt(), b.readVarInt(),
						b.readList(d -> new Derived(d.readString(), d.readString(), d.readString())), b.readVarInt())));
	}
}
