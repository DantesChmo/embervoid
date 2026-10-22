package com.embervoid.vampire;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Splits players into groups for the Vampires (DESIGN §10.12): players within {@code radius} of each other share a
 * group, chained (A near B and B near C puts all three together). Pure logic, positions as {x, y, z}.
 */
public final class VampireGroups {
	/** Indices of the positions in each group, in input order; every index is in exactly one group. */
	public static List<List<Integer>> cluster(List<double[]> positions, double radius) {
		int n = positions.size();
		int[] parent = new int[n];
		for (int i = 0; i < n; i++) {
			parent[i] = i;
		}
		double r2 = radius * radius;
		for (int i = 0; i < n; i++) {
			for (int j = i + 1; j < n; j++) {
				if (distanceSquared(positions.get(i), positions.get(j)) <= r2) {
					parent[find(parent, i)] = find(parent, j);
				}
			}
		}
		Map<Integer, List<Integer>> groups = new LinkedHashMap<>();
		for (int i = 0; i < n; i++) {
			groups.computeIfAbsent(find(parent, i), root -> new ArrayList<>()).add(i);
		}
		return new ArrayList<>(groups.values());
	}

	private static int find(int[] parent, int i) {
		while (parent[i] != i) {
			parent[i] = parent[parent[i]];
			i = parent[i];
		}
		return i;
	}

	private static double distanceSquared(double[] a, double[] b) {
		double dx = a[0] - b[0];
		double dy = a[1] - b[1];
		double dz = a[2] - b[2];
		return dx * dx + dy * dy + dz * dz;
	}

	private VampireGroups() {}
}
