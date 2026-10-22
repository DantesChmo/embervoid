package com.embervoid.gametest;

import com.embervoid.entity.AshRemnantEntity;
import com.embervoid.player.XpUtil;
import com.embervoid.registry.ModEntities;
import com.embervoid.reliquary.Reliquary;
import com.embervoid.reliquary.ReliquaryManager;
import com.embervoid.reliquary.ReliquaryScreenHandler;
import com.embervoid.reliquary.SlotKinds;
import com.embervoid.stats.PlayerStats;
import com.embervoid.stats.Stat;
import com.embervoid.stats.StatEffects;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Reliquary (PLAN 11.9–11.13) on real server players. The gametest mod tags vanilla stand-ins as accessories
 * ({@code data/embervoid/tags/items}): rabbit foot and heart of the sea as artifacts, gold nugget as a ring.
 */
public final class ReliquaryTests implements FabricGameTest {
	private static final Item ARTIFACT = Items.RABBIT_FOOT;
	private static final Item OTHER_ARTIFACT = Items.HEART_OF_THE_SEA;
	private static final Item RING = Items.GOLD_NUGGET;

	/** Shift-clicking an artifact in the menu equips it, saves it on the player and sends it to the client. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void menuEquipsSavesAndSyncs(TestContext context) {
		RecordingConnection connection = new RecordingConnection();
		ServerPlayerEntity player = joinPlayer(context, connection);
		player.getInventory().setStack(9, new ItemStack(ARTIFACT, 3));
		ReliquaryScreenHandler menu = openMenu(context, player);
		int synced = connection.reliquaries.size();

		menu.quickMove(player, menu.accessoryCount() + inventoryIndex(9));
		context.assertTrue(ReliquaryManager.get(player).get(SlotKinds.ARTIFACT, 0).isOf(ARTIFACT), "artifact not equipped");
		context.assertTrue(player.getInventory().getStack(9).getCount() == 2, "should take one item, left "
				+ player.getInventory().getStack(9));
		context.assertTrue(ReliquaryManager.has(player, ARTIFACT), "has() does not see the artifact");
		context.assertTrue(connection.reliquaries.size() > synced, "no sync packet after equipping");
		Reliquary last = connection.reliquaries.get(connection.reliquaries.size() - 1);
		context.assertTrue(last.get(SlotKinds.ARTIFACT, 0).isOf(ARTIFACT), "sync packet without the artifact: " + last);

		// Saved with the player.
		NbtCompound nbt = player.writeNbt(new NbtCompound());
		ServerPlayerEntity loaded = new ServerPlayerEntity(player.getServer(), context.getWorld(),
				new GameProfile(UUID.randomUUID(), "reliquary-loaded"));
		loaded.readNbt(nbt);
		context.assertTrue(ReliquaryManager.get(loaded).get(SlotKinds.ARTIFACT, 0).isOf(ARTIFACT), "not saved");
		TestPlayers.complete(context, player);
	}

	/** A slot takes only its own kind, one item, and never an item already worn in another slot. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void slotsTakeOnlyTheirKindOnce(TestContext context) {
		ServerPlayerEntity player = joinPlayer(context, new RecordingConnection());
		ReliquaryScreenHandler menu = openMenu(context, player);
		int artifactSlot = 0;
		int secondArtifactSlot = 1;
		int ringSlot = SlotKinds.ARTIFACT.count();

		context.assertFalse(menu.getSlot(artifactSlot).canInsert(new ItemStack(RING)), "ring fits an artifact slot");
		context.assertFalse(menu.getSlot(ringSlot).canInsert(new ItemStack(ARTIFACT)), "artifact fits a ring slot");
		context.assertFalse(menu.getSlot(artifactSlot).canInsert(new ItemStack(Items.DIAMOND)), "diamond fits");
		context.assertTrue(menu.getSlot(ringSlot).canInsert(new ItemStack(RING)), "ring does not fit a ring slot");

		// Clicking a stack of two onto the slot places one.
		menu.setCursorStack(new ItemStack(ARTIFACT, 2));
		menu.onSlotClick(artifactSlot, 0, SlotActionType.PICKUP, player);
		context.assertTrue(menu.getSlot(artifactSlot).getStack().getCount() == 1, "slot holds "
				+ menu.getSlot(artifactSlot).getStack());
		context.assertTrue(menu.getCursorStack().getCount() == 1, "cursor holds " + menu.getCursorStack());
		// The same artifact again does not fit the second slot; a different one does.
		context.assertFalse(menu.getSlot(secondArtifactSlot).canInsert(menu.getCursorStack()), "same artifact twice");
		context.assertTrue(menu.getSlot(secondArtifactSlot).canInsert(new ItemStack(OTHER_ARTIFACT)), "other artifact refused");
		context.assertFalse(ReliquaryManager.canEquip(player, SlotKinds.ARTIFACT, 1, new ItemStack(ARTIFACT)),
				"canEquip allows the same artifact twice");
		menu.setCursorStack(ItemStack.EMPTY);
		TestPlayers.complete(context, player);
	}

	/** Shift-clicking an equipped accessory puts it back into the inventory and empties the slot. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void menuUnequipsToInventory(TestContext context) {
		ServerPlayerEntity player = joinPlayer(context, new RecordingConnection());
		ReliquaryManager.setStack(player, SlotKinds.RING, 1, new ItemStack(RING));
		ReliquaryScreenHandler menu = openMenu(context, player);
		int ringSlot = SlotKinds.ARTIFACT.count() + 1;
		context.assertTrue(menu.getSlot(ringSlot).getStack().isOf(RING), "menu does not show the equipped ring");

		menu.quickMove(player, ringSlot);
		context.assertTrue(ReliquaryManager.get(player).get(SlotKinds.RING, 1).isEmpty(), "ring still equipped");
		context.assertTrue(player.getInventory().count(RING) == 1, "ring not in the inventory");
		context.assertFalse(ReliquaryManager.has(player, RING), "has() still sees the ring");
		TestPlayers.complete(context, player);
	}

	/** Accessories in slots that no longer exist (config lowered) are handed back on join. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void leftoversAreHandedBackOnJoin(TestContext context) {
		ServerPlayerEntity player = newPlayer(context);
		Reliquary reliquary = ReliquaryManager.get(player);
		reliquary.set(SlotKinds.ARTIFACT, 0, new ItemStack(ARTIFACT));
		reliquary.set(SlotKinds.ARTIFACT, SlotKinds.ARTIFACT.count() + 3, new ItemStack(OTHER_ARTIFACT));
		connect(context, player, new RecordingConnection());

		context.assertTrue(ReliquaryManager.get(player).get(SlotKinds.ARTIFACT, 0).isOf(ARTIFACT), "kept slot lost");
		context.assertTrue(player.getInventory().count(OTHER_ARTIFACT) == 1, "leftover not handed back");
		context.assertTrue(ReliquaryManager.get(player).get(SlotKinds.ARTIFACT, SlotKinds.ARTIFACT.count() + 3).isEmpty(),
				"leftover still in the Reliquary");
		TestPlayers.complete(context, player);
	}

	/**
	 * Death moves worn accessories into the Ash Remnant with the inventory (they survive the remnant being saved and
	 * loaded); reclaiming it puts them back into their slots.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void accessoriesGoIntoRemnantAndBack(TestContext context) {
		ServerPlayerEntity player = joinPlayer(context, new RecordingConnection());
		ReliquaryManager.setStack(player, SlotKinds.ARTIFACT, 1, new ItemStack(ARTIFACT));
		ReliquaryManager.setStack(player, SlotKinds.RING, 0, new ItemStack(RING));
		player.getInventory().setStack(5, new ItemStack(Items.DIAMOND));

		player.kill();
		context.assertTrue(ReliquaryManager.get(player).isEmpty(), "accessories still worn after death");
		List<? extends AshRemnantEntity> remnants = remnantsOf(context, player);
		context.assertTrue(remnants.size() == 1, "expected one remnant, found " + remnants.size());
		context.assertTrue(context.getWorld().getEntitiesByClass(ItemEntity.class, player.getBoundingBox().expand(8),
				item -> true).isEmpty(), "items dropped on the ground");

		// Through NBT, as when the chunk is saved and loaded.
		AshRemnantEntity original = remnants.get(0);
		AshRemnantEntity loaded = new AshRemnantEntity(ModEntities.ASH_REMNANT, context.getWorld());
		loaded.readNbt(original.writeNbt(new NbtCompound()));
		original.discard();

		ServerPlayerEntity respawned = TestPlayers.respawn(player);
		context.assertTrue(ReliquaryManager.get(respawned).isEmpty(), "accessories kept through death");
		loaded.interact(respawned, Hand.MAIN_HAND);
		context.assertTrue(ReliquaryManager.get(respawned).get(SlotKinds.ARTIFACT, 1).isOf(ARTIFACT), "artifact not back in its slot");
		context.assertTrue(ReliquaryManager.get(respawned).get(SlotKinds.RING, 0).isOf(RING), "ring not back in its slot");
		context.assertTrue(respawned.getInventory().getStack(5).isOf(Items.DIAMOND), "inventory not back");
		TestPlayers.complete(context, respawned);
	}

	/** A death with only accessories (the flask stays with the player) still leaves a remnant with them. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void accessoriesAloneLeaveARemnant(TestContext context) {
		ServerPlayerEntity player = joinPlayer(context, new RecordingConnection());
		player.getInventory().clear();
		XpUtil.setPoints(player, 0);
		ReliquaryManager.setStack(player, SlotKinds.RING, 0, new ItemStack(RING));

		player.kill();
		List<? extends AshRemnantEntity> remnants = remnantsOf(context, player);
		context.assertTrue(remnants.size() == 1, "expected one remnant, found " + remnants.size());
		ServerPlayerEntity respawned = TestPlayers.respawn(player);
		remnants.get(0).interact(respawned, Hand.MAIN_HAND);
		context.assertTrue(ReliquaryManager.has(respawned, RING), "ring not back");
		TestPlayers.complete(context, respawned);
	}

	/** An accessory whose slot is taken again by recovery time goes to the inventory instead. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void recoveredAccessoryIntoTakenSlotGoesToInventory(TestContext context) {
		ServerPlayerEntity player = joinPlayer(context, new RecordingConnection());
		ReliquaryManager.setStack(player, SlotKinds.ARTIFACT, 0, new ItemStack(ARTIFACT));
		player.kill();
		AshRemnantEntity remnant = remnantsOf(context, player).get(0);
		ServerPlayerEntity respawned = TestPlayers.respawn(player);
		ReliquaryManager.setStack(respawned, SlotKinds.ARTIFACT, 0, new ItemStack(OTHER_ARTIFACT));

		remnant.interact(respawned, Hand.MAIN_HAND);
		context.assertTrue(ReliquaryManager.get(respawned).get(SlotKinds.ARTIFACT, 0).isOf(OTHER_ARTIFACT), "slot overwritten");
		context.assertTrue(respawned.getInventory().count(ARTIFACT) == 1, "artifact not in the inventory");
		TestPlayers.complete(context, respawned);
	}

	/** Under keepInventory the accessories stay worn through death. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "reliquary_keep_inventory")
	public void keepInventoryKeepsAccessories(TestContext context) {
		GameRules.BooleanRule rule = context.getWorld().getGameRules().get(GameRules.KEEP_INVENTORY);
		boolean before = rule.get();
		rule.set(true, context.getWorld().getServer());
		try {
			ServerPlayerEntity player = joinPlayer(context, new RecordingConnection());
			ReliquaryManager.setStack(player, SlotKinds.RING, 0, new ItemStack(RING));
			player.kill();
			ServerPlayerEntity respawned = TestPlayers.respawn(player);
			context.assertTrue(ReliquaryManager.has(respawned, RING), "ring lost under keepInventory");
			context.assertTrue(remnantsOf(context, player).isEmpty(), "remnant under keepInventory");
			TestPlayers.complete(context, respawned);
		} finally {
			rule.set(before, context.getWorld().getServer());
		}
	}

	/**
	 * Bonus points (here from a test source standing in for artifacts) count in stat effects, not in the stat value,
	 * the level or the level cost.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void bonusPointsSkipLevelAndCost(TestContext context) {
		ServerPlayerEntity player = joinPlayer(context, new RecordingConnection());
		int level = PlayerStats.level(player);
		int cost = PlayerStats.levelUpCost(player);
		float health = player.getMaxHealth();

		TEST_BONUS.put(player.getUuid(), 3);
		StatEffects.apply(player);
		context.assertTrue(PlayerStats.get(player, Stat.VITALITY) == Stat.BASE, "own VIT changed");
		context.assertTrue(PlayerStats.effective(player, Stat.VITALITY) == Stat.BASE + 3, "effective VIT "
				+ PlayerStats.effective(player, Stat.VITALITY));
		context.assertTrue(player.getMaxHealth() == health + 3, "max health " + player.getMaxHealth() + ", expected " + (health + 3));
		context.assertTrue(PlayerStats.level(player) == level, "level changed: " + PlayerStats.level(player));
		context.assertTrue(PlayerStats.levelUpCost(player) == cost, "level cost changed: " + PlayerStats.levelUpCost(player));

		TEST_BONUS.remove(player.getUuid());
		StatEffects.apply(player);
		context.assertTrue(player.getMaxHealth() == health, "max health not back: " + player.getMaxHealth());
		TestPlayers.complete(context, player);
	}

	/** VIT bonus per test player, read by a bonus source registered once for these tests. */
	private static final Map<UUID, Integer> TEST_BONUS = new ConcurrentHashMap<>();

	static {
		PlayerStats.registerBonusSource((player, bonus) ->
				bonus[Stat.VITALITY.ordinal()] += TEST_BONUS.getOrDefault(player.getUuid(), 0));
	}

	static List<? extends AshRemnantEntity> remnantsOf(TestContext context, ServerPlayerEntity player) {
		return context.getWorld().getEntitiesByType(ModEntities.ASH_REMNANT,
				remnant -> player.getUuid().equals(remnant.getOwner()));
	}

	// --- helpers -----------------------------------------------------------------------------------------------

	/** Menu index of player inventory slot {@code slot} (9–35 main, 0–8 hotbar), after the accessory slots. */
	static int inventoryIndex(int slot) {
		return slot >= 9 ? slot - 9 : 27 + slot;
	}

	static ReliquaryScreenHandler openMenu(TestContext context, ServerPlayerEntity player) {
		ReliquaryManager.open(player);
		context.assertTrue(player.currentScreenHandler instanceof ReliquaryScreenHandler,
				"Reliquary menu not open: " + player.currentScreenHandler);
		return (ReliquaryScreenHandler) player.currentScreenHandler;
	}

	/** A survival player joined through the real player manager on {@code connection}, on a stone floor. */
	static ServerPlayerEntity joinPlayer(TestContext context, ClientConnection connection) {
		ServerPlayerEntity player = newPlayer(context);
		connect(context, player, connection);
		return player;
	}

	static ServerPlayerEntity newPlayer(TestContext context) {
		return new ServerPlayerEntity(context.getWorld().getServer(), context.getWorld(),
				new GameProfile(UUID.randomUUID(), "reliquary-test"));
	}

	static void connect(TestContext context, ServerPlayerEntity player, ClientConnection connection) {
		for (int dx = 1; dx <= 3; dx++) {
			for (int dz = 1; dz <= 3; dz++) {
				context.setBlockState(dx, 0, dz, Blocks.STONE);
			}
		}
		MinecraftServer server = context.getWorld().getServer();
		server.getPlayerManager().onPlayerConnect(connection, player);
		player.changeGameMode(GameMode.SURVIVAL);
		Vec3d pos = context.getAbsolute(Vec3d.ofBottomCenter(new BlockPos(2, 1, 2)));
		player.teleport(context.getWorld(), pos.x, pos.y, pos.z, 0, 0);
	}

	/** A connection that records the Reliquary sync packets sent to it instead of sending anything. */
	static final class RecordingConnection extends ClientConnection {
		final List<Reliquary> reliquaries = new ArrayList<>();

		RecordingConnection() {
			super(NetworkSide.SERVERBOUND);
		}

		@Override
		public void send(Packet<?> packet) {
			send(packet, null);
		}

		@Override
		public void send(Packet<?> packet, @Nullable PacketCallbacks callbacks) {
			if (packet instanceof CustomPayloadS2CPacket payload && payload.getChannel().equals(ReliquaryManager.SYNC_PACKET)) {
				reliquaries.add(Reliquary.read(new PacketByteBuf(payload.getData().copy())));
			}
		}
	}
}
