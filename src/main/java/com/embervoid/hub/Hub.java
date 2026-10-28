package com.embervoid.hub;

import com.embervoid.Embervoid;
import com.embervoid.hub.stele.CleftStele;
import com.embervoid.hub.stele.CleftStoneBlock;
import com.embervoid.hub.stele.HubStele;
import com.embervoid.hub.stele.SteleBlocks;
import com.embervoid.hub.stele.SteleFrame;
import com.embervoid.realm.api.RealmTeleports;
import com.embervoid.sanctum.SanctumBlocks;
import com.embervoid.sanctum.VoidObeliskBlockEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.FireChargeItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The hub, Ember Sanctum: dimension {@code embervoid:hub}, an island in a void under a fixed dusk, shared by every
 * player. It holds the Ashen Maiden, the ring of Void Obelisks (dark, lit or kindled per player) and an Ember Shrine
 * (rest and flask refill; the hub is not a realm, so resting there sets no respawn point). Layout in
 * {@link HubLayout}.
 *
 * <ul>
 *   <li>The hub structure is placed once per world, when the server first starts with the dimension
 *   ({@link HubState} remembers it); nothing rebuilds it later.</li>
 *   <li>Outside creative nothing can be broken or built in the whole dimension, like in arenas.</li>
 *   <li>Leaving a realm lands here; the way in from the overworld is the world's lit Cleft Stele, the way out the
 *   hub's own ({@code hub.stele.SteleTravel}). Travel in and out: {@link HubTravel}.</li>
 * </ul>
 */
public final class Hub {
	public static final RegistryKey<World> KEY = RegistryKey.of(RegistryKeys.WORLD, Embervoid.id("hub"));

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(Hub::ensurePlaced);
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> !isProtected(world, player));
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (isProtected(world, player) && placesSomething(player.getStackInHand(hand))
					&& (player.isSneaking() || !isInteractive(world.getBlockState(hit.getBlockPos())))) {
				return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		});
		// A bucket aimed past any block in reach goes through item use, not block use.
		UseItemCallback.EVENT.register((player, world, hand) -> {
			ItemStack stack = player.getStackInHand(hand);
			return isProtected(world, player) && stack.getItem() instanceof BucketItem
					? TypedActionResult.fail(stack) : TypedActionResult.pass(stack);
		});
		HubTravel.register();
		HubCommands.register();
	}

	@Nullable
	public static ServerWorld world(MinecraftServer server) {
		return server.getWorld(KEY);
	}

	public static boolean isHub(World world) {
		return world.getRegistryKey() == KEY;
	}

	/**
	 * Places the hub structure if this world has never had it. Returns whether it was placed now; false if it was
	 * already there, or if the dimension or the template is missing (a broken datapack, logged).
	 */
	public static boolean ensurePlaced(MinecraftServer server) {
		ServerWorld hub = world(server);
		if (hub == null) {
			Embervoid.LOGGER.error("Hub dimension {} is missing", KEY.getValue());
			return false;
		}
		HubState state = HubState.get(hub);
		if (state.placed) {
			return false;
		}
		StructureTemplate template = hub.getStructureTemplateManager().getTemplate(HubLayout.TEMPLATE).orElse(null);
		if (template == null) {
			Embervoid.LOGGER.error("Hub template {} is missing", HubLayout.TEMPLATE);
			return false;
		}
		BlockPos origin = HubLayout.TEMPLATE_ORIGIN;
		template.place(hub, origin, origin, new StructurePlacementData(), hub.getRandom(), Block.NOTIFY_LISTENERS);
		state.placed = true;
		state.placedAt = hub.getTime();
		state.markDirty();
		Embervoid.LOGGER.info("Placed the hub at {}", origin.toShortString());
		return true;
	}

	public static Box area() {
		BlockPos origin = HubLayout.TEMPLATE_ORIGIN;
		return new Box(origin, origin.add(HubLayout.TEMPLATE_SIZE_XZ, HubLayout.TEMPLATE_SIZE_Y, HubLayout.TEMPLATE_SIZE_XZ));
	}

	/** Teleports a player to the hub's landing (placing the hub first if needed). False if there is no hub. */
	public static boolean sendToLanding(ServerPlayerEntity player) {
		MinecraftServer server = player.getServer();
		ServerWorld hub = world(server);
		if (hub == null) {
			return false;
		}
		ensurePlaced(server);
		RealmTeleports.teleport(player, hub, Vec3d.ofBottomCenter(HubLayout.LANDING), HubLayout.LANDING_YAW);
		return true;
	}

	private static boolean isProtected(World world, PlayerEntity player) {
		// The abilities follow the game mode; unlike isCreative() they also hold for GameTest's mock players.
		return isHub(world) && !player.getAbilities().creativeMode;
	}

	private static boolean placesSomething(ItemStack stack) {
		Item item = stack.getItem();
		return item instanceof BlockItem || item instanceof BucketItem || item instanceof FlintAndSteelItem
				|| item instanceof FireChargeItem;
	}

	/** Blocks used with a right click: holding a block while using one must not count as building. */
	private static boolean isInteractive(BlockState state) {
		return state.isOf(SanctumBlocks.EMBER_SHRINE) || state.isOf(SanctumBlocks.VOID_OBELISK)
				|| state.isOf(SteleBlocks.CLEFT_STONE) || state.isOf(SteleBlocks.STELE_CRACK);
	}

	/**
	 * Checks the placed hub for the debug command, the server check and the GameTests: shrine, obelisk ring with its realms, the
	 * hub's lit Cleft Stele on its pad, void below. Returns null if it is whole, else what is wrong.
	 */
	@Nullable
	public static String verify(ServerWorld hub) {
		List<String> wrong = new ArrayList<>();
		if (!HubState.get(hub).placed) {
			wrong.add("never placed");
		}
		if (!hub.getBlockState(HubLayout.SHRINE).isOf(SanctumBlocks.EMBER_SHRINE)) {
			wrong.add("no shrine at " + HubLayout.SHRINE.toShortString());
		}
		for (HubLayout.Obelisk obelisk : HubLayout.OBELISKS) {
			if (!(hub.getBlockEntity(obelisk.pos()) instanceof VoidObeliskBlockEntity entity)) {
				wrong.add("no obelisk at " + obelisk.pos().toShortString());
			} else if (!entity.getRealm().equals(obelisk.realm())) {
				wrong.add("obelisk at " + obelisk.pos().toShortString() + " leads to " + entity.getRealm());
			}
		}
		if (!hub.getBlockState(HubLayout.SHRINE.down()).isOf(SanctumBlocks.SANCTUM_FLAGSTONE)) {
			wrong.add("no flagstone under the shrine");
		}
		if (!CleftStele.isLit(hub, HubStele.FRAME) || HubStele.FRAME.stones().stream().anyMatch(pos -> !isKindledStone(hub.getBlockState(pos)))) {
			wrong.add("the hub's stele at " + HubLayout.STELE_CRACK_BOTTOM.toShortString() + " is not whole and lit");
		}
		for (int column = 0; column < SteleFrame.WIDTH; column++) {
			BlockPos pad = HubStele.FRAME.at(column, -1);
			if (hub.getBlockState(pad).isAir()) {
				wrong.add("no pad under the stele at " + pad.toShortString());
			}
		}
		if (!hub.getBlockState(HubLayout.LANDING.down()).isSolidBlock(hub, HubLayout.LANDING.down())
				|| !hub.getBlockState(HubLayout.LANDING).isAir() || !hub.getBlockState(HubLayout.LANDING.up()).isAir()) {
			wrong.add("no room to land at " + HubLayout.LANDING.toShortString());
		}
		if (!hub.getBlockState(new BlockPos(0, HubLayout.TEMPLATE_ORIGIN.getY() - 2, 0)).isAir()) {
			wrong.add("no void below the island");
		}
		return wrong.isEmpty() ? null : String.join("; ", wrong);
	}

	private static boolean isKindledStone(BlockState state) {
		return state.isOf(SteleBlocks.CLEFT_STONE) && state.get(CleftStoneBlock.KINDLED);
	}

	private Hub() {}
}
