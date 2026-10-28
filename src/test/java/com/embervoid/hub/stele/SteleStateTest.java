package com.embervoid.hub.stele;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The stele save. A lit stele's world key needs the registries, so the lit entry is checked as {@code Saved}. */
class SteleStateTest {
	private static final BlockPos ORIGIN = new BlockPos(120, 70, -45);
	private static final Identifier NETHER = new Identifier("minecraft", "the_nether");
	private static final UUID PLAYER = new UUID(7, 7);

	private static Optional<SteleState.Saved> roundTrip(SteleState.Saved saved) {
		return SteleState.Saved.fromNbt(saved.toNbt());
	}

	private static NbtCompound entry(String dimension, String axis) {
		NbtCompound lit = new NbtCompound();
		lit.putString("Dimension", dimension);
		lit.put("Origin", NbtHelper.fromBlockPos(ORIGIN));
		lit.putString("Axis", axis);
		return lit;
	}

	@Test
	void aLitCircleSurvivesASave() {
		SteleState.Saved circle = new SteleState.Saved(NETHER, SteleFrame.circle(ORIGIN));
		assertEquals(Optional.of(circle), roundTrip(circle));
	}

	@Test
	void aLitSlabSurvivesASaveOnEitherAxis() {
		for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
			SteleState.Saved slab = new SteleState.Saved(NETHER, new SteleFrame(ORIGIN, axis));
			assertEquals(Optional.of(slab), roundTrip(slab));
		}
	}

	@Test
	void aSaveFromBeforeTheCircleLoadsAsASlab() {
		assertEquals(Optional.of(new SteleState.Saved(new Identifier("minecraft", "overworld"),
						new SteleFrame(ORIGIN, Direction.Axis.Z, SteleFrame.Shape.SLAB))),
				SteleState.Saved.fromNbt(entry("minecraft:overworld", "z")));
	}

	@Test
	void aBrokenEntryLoadsAsNoLitStele() {
		assertEquals(Optional.empty(), SteleState.Saved.fromNbt(entry("minecraft:overworld", "y")));
		assertEquals(Optional.empty(), SteleState.Saved.fromNbt(entry("minecraft:overworld", "")));
		assertEquals(Optional.empty(), SteleState.Saved.fromNbt(entry("Not A Dimension!", "x")));
		NbtCompound nbt = new NbtCompound();
		nbt.put("Lit", entry("minecraft:overworld", "y"));
		assertNull(SteleState.fromNbt(nbt).lit());
	}

	@Test
	void noLitSteleStaysNone() {
		assertNull(SteleState.fromNbt(SteleState.fromNbt(new NbtCompound()).writeNbt(new NbtCompound())).lit());
	}

	@Test
	void usersSurviveASave() {
		SteleState state = SteleState.fromNbt(new NbtCompound());
		state.used(PLAYER);
		SteleState loaded = SteleState.fromNbt(state.writeNbt(new NbtCompound()));
		assertEquals(Set.of(PLAYER), loaded.replace(null));
	}
}
