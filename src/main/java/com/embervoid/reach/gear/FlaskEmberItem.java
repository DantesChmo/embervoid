package com.embervoid.reach.gear;

import com.embervoid.Embervoid;
import com.embervoid.item.HealingFlaskItem;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Flask Ember: the Smoldering Lord's first-kill gift (DESIGN §4.5). Used once per player, it makes every flask sip heal
 * 15 % more for good. Stored in the player's own attachment (persistent, kept through death); the owner's client gets
 * a copy for the flask tooltip.
 */
public class FlaskEmberItem extends LoreItem {
	public static final Identifier SYNC_PACKET = Embervoid.id("flask_ember_sync");

	@SuppressWarnings("UnstableApiUsage")
	private static final AttachmentType<Boolean> USED = AttachmentRegistry.<Boolean>builder()
			.persistent(Codec.BOOL)
			.copyOnDeath()
			.buildAndRegister(Embervoid.id("flask_ember"));

	/** The local player's copy, set by the client from {@link #SYNC_PACKET}. */
	public static volatile boolean clientUsed;

	public FlaskEmberItem(Settings settings) {
		super(settings);
	}

	/** Whether the player has fed a Flask Ember to their flask (on a client: the local player, as last synced). */
	@SuppressWarnings("UnstableApiUsage")
	public static boolean used(PlayerEntity player) {
		if (player.getWorld().isClient()) {
			return clientUsed;
		}
		return Boolean.TRUE.equals(player.getAttached(USED));
	}

	@SuppressWarnings("UnstableApiUsage")
	static void setUsed(ServerPlayerEntity player, boolean used) {
		player.setAttached(USED, used);
		sync(player);
	}

	static void sync(ServerPlayerEntity player) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeBoolean(used(player));
		ServerPlayNetworking.send(player, SYNC_PACKET, buf);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (used(user)) {
			if (!world.isClient()) {
				user.sendMessage(Text.translatable("embervoid.gear.flask_ember.already").formatted(Formatting.GRAY), true);
			}
			return TypedActionResult.fail(stack);
		}
		if (user instanceof ServerPlayerEntity player) {
			setUsed(player, true);
			world.playSound(null, user.getX(), user.getY(), user.getZ(), GearSounds.FLASK_EMBER_USE,
					SoundCategory.PLAYERS, 1.0f, 1.0f);
			player.sendMessage(Text.translatable("embervoid.gear.flask_ember.used").formatted(Formatting.GOLD), true);
			if (!player.getAbilities().creativeMode) {
				stack.decrement(1);
			}
		}
		return TypedActionResult.success(stack, world.isClient());
	}

	static void register() {
		HealingFlaskItem.addHealMultiplier(player -> used(player) ? 1 + GearConfig.get().consumables().flaskEmberBonus() : 1);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> sync(newPlayer));
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> sync(player));
	}
}
