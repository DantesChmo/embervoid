package com.embervoid.reach.world;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Pure flood fill of a hollow in a grid of ground heights (sinkhole pools); unit-tested. */
public final class Hollows {
	/**
	 * Columns connected to (x, z) whose ground is below {@code level}; null when the hollow reaches the edge of the
	 * window (it is not closed) or grows past {@code maxColumns}.
	 */
	@Nullable
	public static List<int[]> fill(int[][] ground, int x0, int z0, int level, int maxColumns) {
		int size = ground.length;
		boolean[][] seen = new boolean[size][size];
		List<int[]> pool = new ArrayList<>();
		Deque<int[]> queue = new ArrayDeque<>();
		queue.add(new int[]{x0, z0});
		seen[x0][z0] = true;
		while (!queue.isEmpty()) {
			int[] c = queue.poll();
			if (c[0] == 0 || c[1] == 0 || c[0] == size - 1 || c[1] == size - 1 || pool.size() >= maxColumns) {
				return null;
			}
			pool.add(c);
			for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
				int nx = c[0] + d[0];
				int nz = c[1] + d[1];
				if (!seen[nx][nz] && ground[nx][nz] < level) {
					seen[nx][nz] = true;
					queue.add(new int[]{nx, nz});
				}
			}
		}
		return pool;
	}

	private Hollows() {}
}
