package com.embervoid.reach.gear;

import com.embervoid.Embervoid;
import com.embervoid.registry.ModBlocks;
import com.embervoid.registry.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;

/**
 * Realm consumables (DESIGN §4.5 "Consumables"): Ash Bomb, Smoldering Resin and cinder fuel from the Fields; Amber Balm
 * and Amber Lantern Oil from the Woods; the Whetstone from Necropolis grave iron.
 */
public final class GearConsumables {
	/** Game time until which a weapon's resin burns, on the weapon stack. */
	public static final String RESIN_KEY = "EmbervoidResinUntil";
	/** Game time until which an ember lantern's oil burns, on the lantern stack. */
	public static final String OIL_KEY = "EmbervoidOilUntil";

	public static final Item ASH_BOMB = ModItems.register("ash_bomb", new AshBombItem(new Item.Settings().maxCount(16)));
	public static final Item SMOLDERING_RESIN = ModItems.register("smoldering_resin",
			new SmolderingResinItem(new Item.Settings().maxCount(16)));
	public static final Item AMBER_BALM = ModItems.register("amber_balm",
			new AmberBalmItem(new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON)));
	public static final Item AMBER_LANTERN_OIL = ModItems.register("amber_lantern_oil",
			new LanternOilItem(new Item.Settings().maxCount(16)));
	public static final Item WHETSTONE = ModItems.register("whetstone", new WhetstoneItem(new Item.Settings().maxCount(16)));

	public static final EntityType<AshBombEntity> ASH_BOMB_ENTITY = Registry.register(Registries.ENTITY_TYPE, Embervoid.id("ash_bomb"),
			FabricEntityTypeBuilder.<AshBombEntity>create(SpawnGroup.MISC, AshBombEntity::new)
					.dimensions(EntityDimensions.fixed(0.25f, 0.25f))
					.trackRangeBlocks(4).trackedUpdateRate(10)
					.build());

	/** A weapon the resin can coat: a melee weapon that hits with its edge. */
	public static boolean resinWeapon(ItemStack stack) {
		Item item = stack.getItem();
		return item instanceof GearWeaponItem || item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem;
	}

	/** Whether the stack's resin still burns at this game time. */
	public static boolean resinBurning(ItemStack stack, long time) {
		return stack.getNbt() != null && ConsumableRules.burning(stack.getNbt().getLong(RESIN_KEY), time);
	}

	/** Whether this is an ember lantern whose oil still burns at this game time. */
	public static boolean lanternBurning(ItemStack stack, long time) {
		return stack.isOf(ModBlocks.EMBER_LANTERN.asItem()) && stack.getNbt() != null && ConsumableRules.burning(stack.getNbt().getLong(OIL_KEY), time);
	}

	/**
	 * Whether the player carries a lit ember lantern in the offhand (Amber Lantern Oil). Other tracks may treat such a
	 * player as a light source (the Crypt Keeper's darkness, Crypt Moths).
	 */
	public static boolean carriesLitLantern(PlayerEntity player) {
		return lanternBurning(player.getOffHandStack(), player.getWorld().getTime());
	}

	/** A lit lantern lets its bearer see in the dark (Night Vision, refreshed while it burns). */
	static void tickLantern(ServerPlayerEntity player) {
		if (carriesLitLantern(player)) {
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 260, 0, true, false, true));
		}
	}

	/** {@link #tickLantern} for GameTests. */
	public static void tickLanternForTest(ServerPlayerEntity player) {
		tickLantern(player);
	}

	/** The other hand's stack. */
	static ItemStack otherHand(PlayerEntity player, Hand hand) {
		return player.getStackInHand(hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);
	}

	static void register() {
		FuelRegistry.INSTANCE.add(GearItems.CINDER_SHARD, GearConfig.get().consumables().cinderFuelTicks());
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % 20 == 0) {
				server.getPlayerManager().getPlayerList().forEach(GearConsumables::tickLantern);
			}
		});
	}

	private GearConsumables() {}
}
