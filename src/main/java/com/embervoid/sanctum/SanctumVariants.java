package com.embervoid.sanctum;

import com.embervoid.Embervoid;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Ruined Sanctum's variants (PLAN 16.2), told apart by their start piece: the courtyard ruin (the only one before
 * 0.5.0), the collapsed chapel and the sunken cloister. Each leads through its own stairwell and guarded hall to the
 * same boss wing.
 */
public final class SanctumVariants {
	public static final Identifier RUIN = Embervoid.id("sanctum/ruin");
	public static final Identifier CHAPEL = Embervoid.id("sanctum/chapel");
	public static final Identifier CLOISTER = Embervoid.id("sanctum/cloister");
	/** In the start pool's order. */
	public static final List<Identifier> ALL = List.of(RUIN, CHAPEL, CLOISTER);

	/** Which variant a planned Ruined Sanctum is (its start piece's template), or null for none of them. */
	@Nullable
	public static Identifier of(StructureStart start) {
		if (!start.hasChildren()) {
			return null;
		}
		return ALL.stream().filter(id -> SanctumArena.isPiece(start.getChildren().get(0), id)).findFirst().orElse(null);
	}

	private SanctumVariants() {}
}
