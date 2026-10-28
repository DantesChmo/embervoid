package com.embervoid.gametest;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The last sounds the server played (fed by {@code ServerWorldSoundMixin}). Tests run side by side, so a check looks
 * only near its own entity.
 */
public final class SoundLog {
	private static final int KEEP = 2048;
	private static final Deque<Heard> HEARD = new ArrayDeque<>();

	private record Heard(ServerWorld world, Identifier id, Vec3d pos) {}

	public static synchronized void record(ServerWorld world, Identifier id, Vec3d pos) {
		HEARD.addLast(new Heard(world, id, pos));
		if (HEARD.size() > KEEP) {
			HEARD.removeFirst();
		}
	}

	/** Drops what was heard around {@code near} (an earlier test in the same spot). */
	public static synchronized void forget(ServerWorld world, Vec3d near, double radius) {
		HEARD.removeIf(h -> h.world() == world && h.pos().isInRange(near, radius));
	}

	public static synchronized boolean heard(ServerWorld world, SoundEvent sound, Vec3d near, double radius) {
		return HEARD.stream().anyMatch(h -> h.world() == world && h.id().equals(sound.getId()) && h.pos().isInRange(near, radius));
	}

	private SoundLog() {}
}
