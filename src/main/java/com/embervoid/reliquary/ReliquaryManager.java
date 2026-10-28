package com.embervoid.reliquary;

import com.embervoid.Embervoid;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * The Reliquary API: a player's accessory slots (DESIGN §4.5), stored in their own attachment and synced to the
 * owning client.
 *
 * <p>Read with {@link #get} / {@link #has} on either side (the client holds a synced copy of its own player's
 * Reliquary). Change only on the server, through {@link #setStack} or the screen: each change syncs and fires
 * {@link #CHANGED}, where accessory effects are reapplied.
 */
public final class ReliquaryManager {
	/** S2C: the full {@link Reliquary} of the receiving player. */
	public static final Identifier SYNC_PACKET = Embervoid.id("reliquary");
	/** C2S, empty: open the Reliquary screen (inventory button, keybind). */
	public static final Identifier OPEN_PACKET = Embervoid.id("open_reliquary");

	// Persistent and copied on death: with keepInventory the accessories stay; otherwise the death has already
	// moved them into the Ash Remnant (or dropped them) and the copy is empty. Dimension changes keep the entity.
	@SuppressWarnings("UnstableApiUsage")
	public static final AttachmentType<Reliquary> RELIQUARY = AttachmentRegistry.<Reliquary>builder()
			.persistent(Reliquary.CODEC)
			.initializer(Reliquary::createDefault)
			.copyOnDeath()
			.buildAndRegister(Embervoid.id("reliquary"));

	/**
	 * Fired on the server after a player's accessories changed, and when they are loaded onto a player entity (join,
	 * respawn): reapply what they give. Already synced.
	 */
	public static final Event<Changed> CHANGED = EventFactory.createArrayBacked(Changed.class,
			listeners -> player -> {
				for (Changed listener : listeners) {
					listener.onChanged(player);
				}
			});

	@FunctionalInterface
	public interface Changed {
		void onChanged(ServerPlayerEntity player);
	}

	/** One accessory slot: its kind and index within the kind. */
	public record SlotRef(SlotKind kind, int slot) {}

	@SuppressWarnings("UnstableApiUsage")
	public static Reliquary get(PlayerEntity player) {
		return player.getAttachedOrCreate(RELIQUARY);
	}

	/** Replaces the player's Reliquary; the client uses this when a sync packet arrives. */
	@SuppressWarnings("UnstableApiUsage")
	public static void set(PlayerEntity player, Reliquary reliquary) {
		player.setAttached(RELIQUARY, reliquary);
	}

	/** Whether the player wears {@code item} in a current Reliquary slot. */
	public static boolean has(PlayerEntity player, Item item) {
		Reliquary reliquary = get(player);
		for (SlotRef ref : slots()) {
			if (reliquary.get(ref.kind(), ref.slot()).isOf(item)) {
				return true;
			}
		}
		return false;
	}

	/** Stacks in the player's current slots of {@code kind} (empty ones left out). */
	public static List<ItemStack> equipped(PlayerEntity player, SlotKind kind) {
		Reliquary reliquary = get(player);
		List<ItemStack> result = new ArrayList<>();
		for (int i = 0; i < kind.count(); i++) {
			ItemStack stack = reliquary.get(kind, i);
			if (!stack.isEmpty()) {
				result.add(stack);
			}
		}
		return result;
	}

	/** The current slots, kind after kind (config counts). */
	public static List<SlotRef> slots() {
		return ReliquarySlots.slots(SlotKinds.ALL, SlotKind::count);
	}

	/**
	 * Whether {@code stack} may go into the slot: the kind's tag, a slot that exists, one item, and no other slot
	 * holding the same item (each accessory counts once, so two of a kind are not worth wearing).
	 */
	public static boolean canEquip(PlayerEntity player, SlotKind kind, int slot, ItemStack stack) {
		return kind.accepts(stack) && stack.getCount() == 1
				&& ReliquarySlots.canTake(store(get(player)), slots(), kind, slot, kind.count(), held -> held.isOf(stack.getItem()));
	}

	private static ReliquarySlots.Store<ItemStack> store(Reliquary reliquary) {
		return new ReliquarySlots.Store<>() {
			@Override
			public Iterable<String> kindNames() {
				return reliquary.kindNames();
			}

			@Override
			public int size(String kind) {
				return reliquary.size(kind);
			}

			@Override
			public ItemStack get(String kind, int slot) {
				return reliquary.get(kind, slot);
			}
		};
	}

	/**
	 * Puts {@code stack} into a slot without checks (see {@link #canEquip}) and returns what was there. Syncs and
	 * fires {@link #CHANGED}.
	 */
	public static ItemStack setStack(ServerPlayerEntity player, SlotKind kind, int slot, ItemStack stack) {
		Reliquary reliquary = get(player);
		ItemStack previous = reliquary.get(kind, slot);
		reliquary.set(kind, slot, stack);
		changed(player);
		return previous;
	}

	/**
	 * Takes every stack out of the Reliquary, current slots and leftovers alike, in {@link #slots()} order (empty
	 * slots as {@link ItemStack#EMPTY}) followed by the leftovers. Syncs and fires {@link #CHANGED}.
	 */
	public static List<ItemStack> takeAll(ServerPlayerEntity player) {
		Reliquary reliquary = get(player);
		List<ItemStack> taken = new ArrayList<>();
		for (SlotRef ref : slots()) {
			taken.add(reliquary.get(ref.kind(), ref.slot()));
		}
		taken.addAll(leftovers(reliquary));
		set(player, Reliquary.createDefault());
		changed(player);
		return taken;
	}

	/** Stacks outside the current slots: in kinds that no longer exist or slots past the current count. */
	private static List<ItemStack> leftovers(Reliquary reliquary) {
		return ReliquarySlots.leftovers(store(reliquary), SlotKinds.ALL, SlotKind::count, ItemStack::isEmpty);
	}

	/**
	 * Hands stacks outside the current slots back to the player (inventory, else at their feet): after the config
	 * lowered a slot count or a kind went away.
	 */
	static void handBackLeftovers(ServerPlayerEntity player) {
		Reliquary reliquary = get(player);
		List<ItemStack> leftovers = leftovers(reliquary);
		if (leftovers.isEmpty()) {
			return;
		}
		Reliquary kept = Reliquary.createDefault();
		for (SlotRef ref : slots()) {
			kept.set(ref.kind(), ref.slot(), reliquary.get(ref.kind(), ref.slot()));
		}
		set(player, kept);
		leftovers.forEach(stack -> player.getInventory().offerOrDrop(stack));
		Embervoid.LOGGER.info("Reliquary: handed {} accessories outside the current slots back to {}",
				leftovers.size(), player.getGameProfile().getName());
	}

	/** Sends the player's Reliquary to their client. */
	public static void sync(ServerPlayerEntity player) {
		PacketByteBuf buf = PacketByteBufs.create();
		get(player).write(buf);
		ServerPlayNetworking.send(player, SYNC_PACKET, buf);
	}

	static void changed(ServerPlayerEntity player) {
		sync(player);
		CHANGED.invoker().onChanged(player);
	}

	/** Opens the Reliquary screen for the player. */
	public static void open(ServerPlayerEntity player) {
		List<SlotRef> slots = slots();
		player.openHandledScreen(new ExtendedScreenHandlerFactory() {
			@Override
			public void writeScreenOpeningData(ServerPlayerEntity target, PacketByteBuf buf) {
				ReliquaryScreenHandler.writeCounts(buf);
			}

			@Override
			public Text getDisplayName() {
				return Text.translatable("embervoid.reliquary.title");
			}

			@Override
			public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity target) {
				return new ReliquaryScreenHandler(syncId, inventory, ReliquaryScreenHandler.currentCounts(),
						new ReliquaryInventory(player, slots));
			}
		});
		player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), ReliquarySounds.OPEN, SoundCategory.PLAYERS,
				1.0f, 1.0f);
	}

	public static void register() {
		// The client creates a new player entity on join, respawn and dimension change, so resend each time.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			handBackLeftovers(player);
			changed(player);
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> changed(newPlayer));
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> sync(player));
		ServerPlayNetworking.registerGlobalReceiver(OPEN_PACKET, (server, player, handler, buf, responseSender) ->
				server.execute(() -> {
					if (player.isAlive() && !player.isSpectator()) {
						open(player);
					}
				}));
	}

	private ReliquaryManager() {}
}
