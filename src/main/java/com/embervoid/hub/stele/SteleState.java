package com.embervoid.hub.stele;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Saved with the overworld: the world's one lit Cleft Stele and who has gone
 * through it. The hub's own stele is never here.
 */
public final class SteleState extends PersistentState {
	private static final String ID = "embervoid_stele";

	public record Lit(RegistryKey<World> dimension, SteleFrame frame) {
		/** Whether {@code pos} in {@code world} is one of its stones or its crack. */
		public boolean contains(RegistryKey<World> world, BlockPos pos) {
			return world == dimension && (frame.stones().contains(pos) || frame.crack().contains(pos));
		}
	}

	/** A lit stele as saved: the dimension by id, since a world key needs the registries. */
	record Saved(Identifier dimension, SteleFrame frame) {
		/** Empty for a broken entry: no dimension id, or an axis that is not horizontal. */
		static Optional<Saved> fromNbt(NbtCompound nbt) {
			Identifier dimension = Identifier.tryParse(nbt.getString("Dimension"));
			Direction.Axis axis = Direction.Axis.fromName(nbt.getString("Axis"));
			// No shape: an older save, from when every stele was a slab.
			SteleFrame.Shape shape = nbt.getString("Shape").equals("circle") ? SteleFrame.Shape.CIRCLE : SteleFrame.Shape.SLAB;
			if (dimension == null || axis == null || !axis.isHorizontal()) {
				return Optional.empty();
			}
			return Optional.of(new Saved(dimension, new SteleFrame(NbtHelper.toBlockPos(nbt.getCompound("Origin")), axis, shape)));
		}

		NbtCompound toNbt() {
			NbtCompound nbt = new NbtCompound();
			nbt.putString("Dimension", dimension.toString());
			nbt.put("Origin", NbtHelper.fromBlockPos(frame.origin()));
			nbt.putString("Axis", frame.axis().asString());
			nbt.putString("Shape", frame.shape() == SteleFrame.Shape.CIRCLE ? "circle" : "slab");
			return nbt;
		}
	}

	@Nullable
	private Lit lit;
	/** Players who went in or out through the lit stele: they are told when another one puts it out. */
	private final Set<UUID> users = new HashSet<>();

	public static SteleState get(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager().getOrCreate(SteleState::fromNbt, SteleState::new, ID);
	}

	@Nullable
	public Lit lit() {
		return lit;
	}

	/** Makes {@code lit} the world's stele (null: none) and forgets its users; returns the users of the old one. */
	Set<UUID> replace(@Nullable Lit lit) {
		Set<UUID> old = Set.copyOf(users);
		this.lit = lit;
		users.clear();
		markDirty();
		return old;
	}

	void used(UUID player) {
		if (users.add(player)) {
			markDirty();
		}
	}

	static SteleState fromNbt(NbtCompound nbt) {
		SteleState state = new SteleState();
		if (nbt.contains("Lit", NbtElement.COMPOUND_TYPE)) {
			Saved.fromNbt(nbt.getCompound("Lit")).ifPresent(saved ->
					state.lit = new Lit(RegistryKey.of(RegistryKeys.WORLD, saved.dimension()), saved.frame()));
		}
		for (NbtElement user : nbt.getList("Users", NbtElement.INT_ARRAY_TYPE)) {
			state.users.add(NbtHelper.toUuid(user));
		}
		return state;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		if (lit != null) {
			nbt.put("Lit", new Saved(lit.dimension().getValue(), lit.frame()).toNbt());
		}
		NbtList users = new NbtList();
		for (UUID user : this.users) {
			users.add(NbtHelper.fromUuid(user));
		}
		nbt.put("Users", users);
		return nbt;
	}
}
