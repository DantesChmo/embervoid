package com.embervoid.gametest;

import com.embervoid.entity.AshRemnantEntity;
import com.embervoid.gametest.mixin.ServerPlayerEntityAccessor;
import com.embervoid.player.XpUtil;
import com.embervoid.registry.ModEntities;
import com.embervoid.reliquary.ArtifactItem;
import com.embervoid.reliquary.Reliquary;
import com.embervoid.reliquary.ReliquaryConfig;
import com.embervoid.reliquary.ReliquaryItems;
import com.embervoid.reliquary.ReliquaryLoot;
import com.embervoid.reliquary.ReliquaryManager;
import com.embervoid.reliquary.ReliquaryScreenHandler;
import com.embervoid.reliquary.RingEffects;
import com.embervoid.reliquary.RingItem;
import com.embervoid.reliquary.SlotKinds;
import com.embervoid.stats.PlayerStats;
import com.embervoid.stats.Stat;
import com.embervoid.stats.StatEffects;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.GameTestException;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
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
import java.util.function.Supplier;

/**
 * The Reliquary on real server players. The gametest mod tags vanilla stand-ins as accessories
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
		context.assertFalse(menu.getSlot(secondArtifactSlot).canInsert(menu.getCursorStack()), "same artifact twice");
		context.assertTrue(menu.getSlot(secondArtifactSlot).canInsert(new ItemStack(OTHER_ARTIFACT)), "other artifact refused");
		context.assertFalse(ReliquaryManager.canEquip(player, SlotKinds.ARTIFACT, 1, new ItemStack(ARTIFACT)),
				"canEquip allows the same artifact twice");
		menu.setCursorStack(ItemStack.EMPTY);
		TestPlayers.complete(context, player);
	}

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

	/**
	 * Each artifact is an artifact-slot item; worn, it raises its stats by the config points (the Seal
	 * every stat by 1) through the stat system: max health and luck follow, the level and level cost do not.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void artifactsGiveStatPointsNotLevels(TestContext context) {
		ServerPlayerEntity player = joinPlayer(context, new RecordingConnection());
		int level = PlayerStats.level(player);
		int cost = PlayerStats.levelUpCost(player);
		float health = player.getMaxHealth();
		double luck = player.getAttributeValue(EntityAttributes.GENERIC_LUCK);
		int points = ReliquaryConfig.get().artifacts().points();
		int sealPoints = ReliquaryConfig.get().artifacts().sealPoints();
		context.assertTrue(ReliquaryItems.ARTIFACTS.size() == 8, "expected 8 artifacts");

		for (ArtifactItem artifact : ReliquaryItems.ARTIFACTS) {
			ItemStack stack = new ItemStack(artifact);
			context.assertTrue(SlotKinds.ARTIFACT.accepts(stack), artifact + " is not in #embervoid:artifacts");
			context.assertFalse(SlotKinds.RING.accepts(stack), artifact + " fits a ring slot");
			context.assertTrue(ReliquaryManager.canEquip(player, SlotKinds.ARTIFACT, 0, stack), artifact + " cannot be equipped");
			ReliquaryManager.setStack(player, SlotKinds.ARTIFACT, 0, stack);
			boolean seal = artifact == ReliquaryItems.SEAL_OF_THE_ASHEN_KING;
			for (Stat stat : Stat.values()) {
				int expected = artifact.points().getOrDefault(stat, 0);
				context.assertTrue(PlayerStats.effective(player, stat) == Stat.BASE + expected
								&& PlayerStats.get(player, stat) == Stat.BASE,
						artifact + ": " + stat + " effective " + PlayerStats.effective(player, stat) + ", expected +" + expected);
			}
			context.assertTrue(artifact.points().size() == (seal ? Stat.COUNT : 1), artifact + " raises " + artifact.points());
			context.assertTrue(artifact.points().values().stream().allMatch(v -> v == (seal ? sealPoints : points)),
					artifact + " points " + artifact.points());
			context.assertTrue(PlayerStats.level(player) == level && PlayerStats.levelUpCost(player) == cost,
					artifact + " changed level/cost: " + PlayerStats.level(player) + "/" + PlayerStats.levelUpCost(player));
			int vit = artifact.points().getOrDefault(Stat.VITALITY, 0);
			context.assertTrue(player.getMaxHealth() == health + vit, artifact + ": max health " + player.getMaxHealth());
			int fortune = artifact.points().getOrDefault(Stat.FORTUNE, 0);
			double expectedLuck = luck + PlayerStats.derived(player).luck();
			context.assertTrue(Math.abs(player.getAttributeValue(EntityAttributes.GENERIC_LUCK) - expectedLuck) < 1e-6
					&& (fortune == 0 || player.getAttributeValue(EntityAttributes.GENERIC_LUCK) > luck),
					artifact + ": luck " + player.getAttributeValue(EntityAttributes.GENERIC_LUCK));
		}

		// Two artifacts add up; taking them off restores everything.
		ReliquaryManager.setStack(player, SlotKinds.ARTIFACT, 0, new ItemStack(ReliquaryItems.LAST_HARVEST_GRAIN));
		ReliquaryManager.setStack(player, SlotKinds.ARTIFACT, 1, new ItemStack(ReliquaryItems.SEAL_OF_THE_ASHEN_KING));
		context.assertTrue(PlayerStats.effective(player, Stat.VITALITY) == Stat.BASE + points + sealPoints,
				"grain + seal VIT " + PlayerStats.effective(player, Stat.VITALITY));
		ReliquaryManager.setStack(player, SlotKinds.ARTIFACT, 0, ItemStack.EMPTY);
		ReliquaryManager.setStack(player, SlotKinds.ARTIFACT, 1, ItemStack.EMPTY);
		context.assertTrue(player.getMaxHealth() == health, "max health not back: " + player.getMaxHealth());
		context.assertTrue(player.getAttributeValue(EntityAttributes.GENERIC_LUCK) == luck, "luck not back");
		TestPlayers.complete(context, player);
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void ringsFitRingSlotsOnly(TestContext context) {
		context.assertTrue(ReliquaryItems.RINGS.size() == 6, "expected 6 rings");
		for (RingItem ring : ReliquaryItems.RINGS) {
			context.assertTrue(SlotKinds.RING.accepts(new ItemStack(ring)), ring + " is not in #embervoid:rings");
			context.assertFalse(SlotKinds.ARTIFACT.accepts(new ItemStack(ring)), ring + " fits an artifact slot");
		}
		context.complete();
	}

	/** Courier's Ring: sprinting is 15 % faster; walking, or sprinting without the ring, is not. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void couriersRingSpeedsUpSprinting(TestContext context) {
		ServerPlayerEntity player = joinPlayer(context, new RecordingConnection());
		player.setSprinting(true);
		double sprint = player.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
		ReliquaryManager.setStack(player, SlotKinds.RING, 0, new ItemStack(ReliquaryItems.COURIERS_RING));
		double withRing = player.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
		float bonus = ReliquaryConfig.get().rings().courierSprintBonus();
		context.assertTrue(Math.abs(withRing - sprint * (1 + bonus)) < 1e-6, "sprint " + sprint + " -> " + withRing);
		player.setSprinting(false);
		double walk = player.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
		context.assertTrue(Math.abs(walk - player.getAttributeBaseValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)) < 1e-6,
				"walking speed with the ring " + walk);
		player.setSprinting(true);
		ReliquaryManager.setStack(player, SlotKinds.RING, 0, ItemStack.EMPTY);
		context.assertTrue(Math.abs(player.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) - sprint) < 1e-6,
				"ring taken off while sprinting: " + player.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
		TestPlayers.complete(context, player);
	}

	/** Ring of Ash Feathers: no fall damage up to 8 blocks, less beyond; a capped fall speed unless sneaking. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void ashFeathersSoftenFalls(TestContext context) {
		ServerPlayerEntity player = joinPlayer(context, new RecordingConnection());
		((ServerPlayerEntityAccessor) player).embervoid$setJoinInvulnerabilityTicks(0);
		float health = player.getHealth();
		player.handleFallDamage(7, 1, player.getDamageSources().fall());
		context.assertTrue(player.getHealth() < health, "a 7-block fall without the ring did no damage");
		player.setHealth(player.getMaxHealth());
		player.timeUntilRegen = 0;
		health = player.getHealth();

		ReliquaryManager.setStack(player, SlotKinds.RING, 1, new ItemStack(ReliquaryItems.RING_OF_ASH_FEATHERS));
		player.handleFallDamage(7.9f, 1, player.getDamageSources().fall());
		context.assertTrue(player.getHealth() == health, "a 7.9-block fall hurt with the ring: " + player.getHealth());
		player.handleFallDamage(10, 1, player.getDamageSources().fall());
		float taken = health - player.getHealth();
		context.assertTrue(taken > 0 && taken <= 2.01f, "a 10-block fall with the ring took " + taken + ", expected 2");

		player.setOnGround(false);
		context.assertTrue(RingEffects.maxFallSpeed(player) == ReliquaryConfig.get().rings().featherMaxFallSpeed(),
				"no fall speed cap in the air");
		player.setSneaking(true);
		context.assertTrue(RingEffects.maxFallSpeed(player) == 0, "fall speed capped while sneaking");
		TestPlayers.complete(context, player);
	}

	/** Ring of the Fog Stalker: the fog API clearing factor is 2 with the ring, 1 without. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void fogStalkerClearsFog(TestContext context) {
		ServerPlayerEntity player = joinPlayer(context, new RecordingConnection());
		context.assertTrue(RingEffects.fogClearing(player) == 1, "fog cleared without the ring");
		ReliquaryManager.setStack(player, SlotKinds.RING, 0, new ItemStack(ReliquaryItems.RING_OF_THE_FOG_STALKER));
		context.assertTrue(RingEffects.fogClearing(player) == 2, "clearing " + RingEffects.fogClearing(player));
		TestPlayers.complete(context, player);
	}

	/**
	 * Delver's Ring: Haste I (×1.2) mining realm ores, nothing on other blocks. The gametest mod tags iron ore as a
	 * realm ore.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void delversRingHastesRealmOres(TestContext context) {
		ServerPlayerEntity player = joinPlayer(context, new RecordingConnection());
		BlockState ore = Blocks.IRON_ORE.getDefaultState();
		BlockState stone = Blocks.STONE.getDefaultState();
		context.assertTrue(ore.isIn(RingEffects.REALM_ORES), "iron ore not tagged as a realm ore in the test pack");
		float oreSpeed = player.getBlockBreakingSpeed(ore);
		float stoneSpeed = player.getBlockBreakingSpeed(stone);
		ReliquaryManager.setStack(player, SlotKinds.RING, 0, new ItemStack(ReliquaryItems.DELVERS_RING));
		context.assertTrue(Math.abs(player.getBlockBreakingSpeed(ore) - oreSpeed * 1.2f) < 1e-4,
				"ore speed " + oreSpeed + " -> " + player.getBlockBreakingSpeed(ore));
		context.assertTrue(player.getBlockBreakingSpeed(stone) == stoneSpeed, "stone sped up");
		TestPlayers.complete(context, player);
	}

	/**
	 * Drowned Man's Ring: the wearer keeps full air under water. Test players are not ticked by a connection, so the
	 * test runs their base tick (where air is counted) itself.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void drownedMansRingBreathesUnderwater(TestContext context) {
		ServerPlayerEntity player = joinPlayer(context, new RecordingConnection());
		for (int y = 1; y <= 3; y++) {
			context.setBlockState(2, y, 2, Blocks.WATER);
		}
		Vec3d pos = context.getAbsolute(new Vec3d(2.5, 1, 2.5));
		player.teleport(context.getWorld(), pos.x, pos.y, pos.z, 0, 0);
		context.assertFalse(player.canBreatheInWater(), "breathes under water without the ring");
		for (int i = 0; i < 20; i++) {
			player.baseTick();
		}
		context.assertTrue(player.isSubmergedInWater(), "player not under water");
		context.assertTrue(player.getAir() < player.getMaxAir(), "no air lost without the ring");

		player.setAir(player.getMaxAir());
		ReliquaryManager.setStack(player, SlotKinds.RING, 0, new ItemStack(ReliquaryItems.DROWNED_MANS_RING));
		context.assertTrue(player.canBreatheInWater(), "ring does not give water breathing");
		for (int i = 0; i < 20; i++) {
			player.baseTick();
		}
		context.assertTrue(player.getAir() == player.getMaxAir(), "air " + player.getAir() + " under water with the ring");
		TestPlayers.complete(context, player);
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void ringOfEmbersIgnites(TestContext context) {
		ServerPlayerEntity player = joinPlayer(context, new RecordingConnection());
		CowEntity plain = context.spawnEntity(EntityType.COW, 4, 1, 2);
		player.attack(plain);
		context.assertFalse(plain.isOnFire(), "set on fire without the ring");

		ReliquaryManager.setStack(player, SlotKinds.RING, 0, new ItemStack(ReliquaryItems.RING_OF_EMBERS));
		CowEntity target = context.spawnEntity(EntityType.COW, 4, 1, 4);
		player.attack(target);
		context.assertTrue(target.isOnFire(), "melee hit did not ignite");
		context.assertTrue(target.getFireTicks() <= ReliquaryConfig.get().rings().embersFireSeconds() * 20,
				"burns too long: " + target.getFireTicks());
		plain.discard();
		target.discard();
		TestPlayers.complete(context, player);
	}

	/**
	 * Each zone's treasure chest rolls an artifact at about 8 % and a ring at about 5 %, only of that
	 * zone's accessories.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void treasureChestsRollZoneAccessories(TestContext context) {
		ServerWorld world = context.getWorld();
		ReliquaryConfig.Drops drops = ReliquaryConfig.get().drops();
		int rolls = 4000;
		for (ReliquaryLoot.Zone zone : ReliquaryLoot.ZONES) {
			LootTable table = world.getServer().getLootManager().getLootTable(zone.treasureChest());
			context.assertTrue(table != LootTable.EMPTY, "no table " + zone.treasureChest());
			LootContextParameterSet params = new LootContextParameterSet.Builder(world)
					.add(LootContextParameters.ORIGIN, context.getAbsolute(new Vec3d(2, 1, 2)))
					.build(LootContextTypes.CHEST);
			int[] counts = count(zone, rolls, () -> table.generateLoot(params));
			assertRate(context, zone.name() + " chest artifacts", counts[0], rolls, drops.chestArtifact());
			assertRate(context, zone.name() + " chest rings", counts[1], rolls, drops.chestRing());
		}
		context.complete();
	}

	/**
	 * Elite tables get the zone's accessories at about 2 % / 1 %, from player kills only. An elite table that does not
	 * exist is checked on an empty table with the same injection.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void elitesRollZoneAccessoriesOnPlayerKills(TestContext context) {
		ServerWorld world = context.getWorld();
		ServerPlayerEntity player = joinPlayer(context, new RecordingConnection());
		ReliquaryConfig.Drops drops = ReliquaryConfig.get().drops();
		CowEntity victim = context.spawnEntity(EntityType.COW, 4, 1, 4);
		context.assertFalse(ReliquaryLoot.inject(new Identifier("minecraft", "entities/cow"), LootTable.builder()),
				"injected into an unrelated table");
		int rolls = 10_000;
		for (ReliquaryLoot.Zone zone : ReliquaryLoot.ZONES) {
			if (zone.eliteTable() == null) {
				continue;
			}
			LootTable table = world.getServer().getLootManager().getLootTable(zone.eliteTable());
			if (table == LootTable.EMPTY) {
				LootTable.Builder builder = LootTable.builder().type(LootContextTypes.ENTITY);
				context.assertTrue(ReliquaryLoot.inject(zone.eliteTable(), builder), "not injected into " + zone.eliteTable());
				table = builder.build();
			}
			LootTable loot = table;
			LootContextParameterSet.Builder params = new LootContextParameterSet.Builder(world)
					.add(LootContextParameters.THIS_ENTITY, victim)
					.add(LootContextParameters.ORIGIN, victim.getPos())
					.add(LootContextParameters.DAMAGE_SOURCE, world.getDamageSources().playerAttack(player));
			LootContextParameterSet byPlayer = params.add(LootContextParameters.LAST_DAMAGE_PLAYER, player)
					.add(LootContextParameters.KILLER_ENTITY, player)
					.build(LootContextTypes.ENTITY);
			int[] counts = count(zone, rolls, () -> loot.generateLoot(byPlayer));
			assertRate(context, zone.name() + " elite artifacts", counts[0], rolls, drops.eliteArtifact());
			assertRate(context, zone.name() + " elite rings", counts[1], rolls, drops.eliteRing());

			LootContextParameterSet noPlayer = new LootContextParameterSet.Builder(world)
					.add(LootContextParameters.THIS_ENTITY, victim)
					.add(LootContextParameters.ORIGIN, victim.getPos())
					.add(LootContextParameters.DAMAGE_SOURCE, world.getDamageSources().generic())
					.build(LootContextTypes.ENTITY);
			int[] none = count(zone, 2_000, () -> loot.generateLoot(noPlayer));
			context.assertTrue(none[0] + none[1] == 0, zone.name() + ": elite dropped accessories without a player kill");
		}
		victim.discard();
		TestPlayers.complete(context, player);
	}

	/** Artifacts and rings of {@code zone} in {@code rolls} loot rolls; fails on another zone's accessory. */
	private static int[] count(ReliquaryLoot.Zone zone, int rolls, Supplier<List<ItemStack>> loot) {
		int[] counts = new int[2];
		for (int i = 0; i < rolls; i++) {
			for (ItemStack stack : loot.get()) {
				if (zone.artifacts().contains(stack.getItem())) {
					counts[0]++;
				} else if (zone.rings().contains(stack.getItem())) {
					counts[1]++;
				} else if (stack.getItem() instanceof ArtifactItem || stack.getItem() instanceof RingItem) {
					throw new GameTestException(zone.name() + " dropped another zone's " + stack);
				}
			}
		}
		return counts;
	}

	/** {@code hits} out of {@code rolls} within five standard deviations of {@code chance}. */
	private static void assertRate(TestContext context, String what, int hits, int rolls, float chance) {
		double expected = rolls * chance;
		double tolerance = 5 * Math.sqrt(rolls * chance * (1 - chance));
		context.assertTrue(Math.abs(hits - expected) <= tolerance, what + ": " + hits + " in " + rolls
				+ ", expected about " + Math.round(expected));
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
