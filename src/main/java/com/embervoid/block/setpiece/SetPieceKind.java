package com.embervoid.block.setpiece;

import com.embervoid.block.prop.PropShape;
import net.minecraft.block.Block;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * What one kind of set piece is (PLAN 20.1): its animations, states, actions, how it reacts to arena events, its
 * hitbox and how far it renders. The model, animation and texture files sit under
 * {@code geo/block/setpiece/<name>}, {@code animations/block/setpiece/<name>}, {@code textures/block/setpiece/<name>}
 * (written by {@code tools/setpiece_art.py}).
 *
 * <p>Animation names: {@code idle} loops; {@code state.<s>} loops while the piece is in state {@code s};
 * {@code action.<a>} plays once when triggered. A TRIGGERED piece in a state without its own loop plays {@code idle}.
 */
public final class SetPieceKind {
	/** How a piece runs: always animating by itself, or holding a state until the server says otherwise. */
	public enum Mode {
		ALWAYS, TRIGGERED;

		public String id() {
			return name().toLowerCase(Locale.ROOT);
		}

		@Nullable
		public static Mode byId(String id) {
			for (Mode mode : values()) {
				if (mode.id().equals(id)) {
					return mode;
				}
			}
			return null;
		}
	}

	/**
	 * An arena event the piece answers: {@code event} is the fired event's first word(s) ({@code summon},
	 * {@code phase}, {@code phase 2}, {@code hazard ash_storm}, {@code bossHit heavy}, {@code kill}, {@code reset},
	 * {@code raise_dead}); {@code effect} is what {@link SetPieces#trigger} understands.
	 */
	public record Reaction(String event, String effect) {
		public boolean matches(String fired) {
			return fired.equals(event) || fired.startsWith(event + " ");
		}

		/** "{@code event=effect}", the form a template's NBT override uses. */
		public static Reaction parse(String text) {
			int eq = text.indexOf('=');
			if (eq < 0) {
				throw new IllegalArgumentException("reaction without '=': " + text);
			}
			return new Reaction(text.substring(0, eq).trim(), text.substring(eq + 1).trim());
		}

		@Override
		public String toString() {
			return event + "=" + effect;
		}
	}

	/** Client-side particles while the piece is in {@code state} (or any state when null): a puff every {@code every} ticks at an offset from the block's centre (north-facing, in blocks). */
	public record Puff(@Nullable String state, String particle, double x, double y, double z, double spread, int every) {}

	private final String name;
	private final Mode defaultMode;
	private final boolean idle;
	private final List<String> states;
	private final Set<String> stateLoops;
	private final List<String> actions;
	private final List<Reaction> reactions;
	@Nullable
	private final String litState;
	private final int luminance;
	private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
	private final int renderDistance;
	private final boolean big;
	private final List<Puff> puffs;
	private final boolean bonesToggle;
	private final boolean followsCampfire;

	private SetPieceKind(Builder b) {
		this.name = b.name;
		this.defaultMode = b.defaultMode;
		this.idle = b.idle;
		this.states = List.copyOf(b.states);
		this.stateLoops = Set.copyOf(b.stateLoops);
		this.actions = List.copyOf(b.actions);
		this.reactions = List.copyOf(b.reactions);
		this.litState = b.litState;
		this.luminance = b.luminance;
		this.renderDistance = b.renderDistance;
		this.big = b.big;
		this.puffs = List.copyOf(b.puffs);
		this.bonesToggle = b.bonesToggle;
		this.followsCampfire = b.followsCampfire;
		for (Direction facing : Direction.Type.HORIZONTAL) {
			VoxelShape shape = VoxelShapes.empty();
			for (double[] box : b.boxes) {
				double[] t = PropShape.turn(box, PropShape.quarterTurns(facing));
				shape = VoxelShapes.union(shape, Block.createCuboidShape(t[0], t[1], t[2], t[3], t[4], t[5]));
			}
			shapes.put(facing, shape.isEmpty() ? VoxelShapes.fullCube() : shape);
		}
	}

	public static Builder builder(String name) {
		return new Builder(name);
	}

	public String name() {
		return name;
	}

	public Mode defaultMode() {
		return defaultMode;
	}

	/** States a TRIGGERED piece can hold, the first one the default; empty for pure ALWAYS pieces. */
	public List<String> states() {
		return states;
	}

	public String defaultState() {
		return states.isEmpty() ? "" : states.get(0);
	}

	public boolean hasState(String state) {
		return states.contains(state);
	}

	/** One-shot animations ({@code action.<name>} in the file). */
	public List<String> actions() {
		return actions;
	}

	public List<Reaction> reactions() {
		return reactions;
	}

	/** The state in which the piece gives light, or null. */
	@Nullable
	public String litState() {
		return litState;
	}

	public int luminance() {
		return luminance;
	}

	public VoxelShape shape(Direction facing) {
		return shapes.getOrDefault(facing, VoxelShapes.fullCube());
	}

	/** Chunks of render distance, in blocks, for the client renderer. */
	public int renderDistance() {
		return renderDistance;
	}

	/** Whether the model reaches beyond its block (rendered outside the bounding box). */
	public boolean big() {
		return big;
	}

	public List<Puff> puffs() {
		return puffs;
	}

	/** Whether the piece has a {@code bones} bone the NBT may hide (the hanging cage's bundle). */
	public boolean bonesToggle() {
		return bonesToggle;
	}

	/** Whether the piece's lit state follows a campfire in the block below it (the great brazier). */
	public boolean followsCampfire() {
		return followsCampfire;
	}

	/** Whether the kind has an {@code idle} loop. */
	public boolean hasIdle() {
		return idle;
	}

	/** Whether state {@code state} has its own loop ({@code state.<state>}) in the animation file. */
	public boolean hasStateLoop(String state) {
		return stateLoops.contains(state);
	}

	/** The loop a piece plays in {@code mode} and {@code state}, or null for the rest pose. */
	@Nullable
	public String loopFor(Mode mode, String state) {
		if (mode == Mode.TRIGGERED && hasStateLoop(state)) {
			return "state." + state;
		}
		if (mode == Mode.ALWAYS && hasStateLoop(state) && !idle) {
			return "state." + state;
		}
		return idle ? "idle" : null;
	}

	/** Every animation name the file must hold (checked by {@code SetPieceModelFilesTest}). */
	public List<String> animationNames() {
		List<String> names = new ArrayList<>();
		if (idle) {
			names.add("idle");
		}
		for (String state : states) {
			if (stateLoops.contains(state)) {
				names.add("state." + state);
			}
		}
		for (String action : actions) {
			names.add("action." + action);
		}
		return names;
	}

	@Override
	public String toString() {
		return "SetPieceKind " + name;
	}

	public static final class Builder {
		private final String name;
		private Mode defaultMode = Mode.ALWAYS;
		private boolean idle = true;
		private final List<String> states = new ArrayList<>();
		private final Set<String> stateLoops = new LinkedHashSet<>();
		private final List<String> actions = new ArrayList<>();
		private final List<Reaction> reactions = new ArrayList<>();
		@Nullable
		private String litState;
		private int luminance = 12;
		private final List<double[]> boxes = new ArrayList<>();
		private int renderDistance = 64;
		private boolean big;
		private final List<Puff> puffs = new ArrayList<>();
		private boolean bonesToggle;
		private boolean followsCampfire;

		private Builder(String name) {
			this.name = name;
		}

		public Builder triggered() {
			this.defaultMode = Mode.TRIGGERED;
			return this;
		}

		/** No {@code idle} loop: the piece rests unless a state loop or an action moves it. */
		public Builder noIdle() {
			this.idle = false;
			return this;
		}

		/** A state the piece can hold; {@code loop} says whether {@code state.<name>} exists in the file. The first state added is the default. */
		public Builder state(String state, boolean loop) {
			states.add(state);
			if (loop) {
				stateLoops.add(state);
			}
			return this;
		}

		public Builder actions(String... names) {
			actions.addAll(List.of(names));
			return this;
		}

		public Builder reacts(String event, String effect) {
			reactions.add(new Reaction(event, effect));
			return this;
		}

		public Builder lit(String state, int luminance) {
			this.litState = state;
			this.luminance = luminance;
			return this;
		}

		/** A hitbox box in pixels, north-facing (x1, y1, z1, x2, y2, z2). */
		public Builder box(double x1, double y1, double z1, double x2, double y2, double z2) {
			boxes.add(new double[]{x1, y1, z1, x2, y2, z2});
			return this;
		}

		/** Renders beyond its block, seen from this far. */
		public Builder big(int renderDistance) {
			this.big = true;
			this.renderDistance = renderDistance;
			return this;
		}

		public Builder puff(@Nullable String state, String particle, double x, double y, double z, double spread, int every) {
			puffs.add(new Puff(state, particle, x, y, z, spread, every));
			return this;
		}

		public Builder bonesToggle() {
			this.bonesToggle = true;
			return this;
		}

		public Builder followsCampfire() {
			this.followsCampfire = true;
			return this;
		}

		public SetPieceKind build() {
			return new SetPieceKind(this);
		}
	}
}
