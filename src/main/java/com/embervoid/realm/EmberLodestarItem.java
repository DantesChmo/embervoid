package com.embervoid.realm;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Ember Lodestar: in a realm, points like a compass to the nearest arena whose boss the holder has not beaten.
 * The server writes the target into the stack; the client's "angle" predicate reads it.
 */
public class EmberLodestarItem extends Item {
	private static final String TARGET = "LodestarTarget";
	private static final String DIMENSION = "LodestarDimension";
	private static final int UPDATE_INTERVAL = 20;

	public EmberLodestarItem(Settings settings) {
		super(settings);
	}

	@Override
	public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
		if (entity instanceof ServerPlayerEntity player && world.getTime() % UPDATE_INTERVAL == 0) {
			RealmArenas.Location target = Realms.isRealm(world) ? RealmArenas.nearestUnbeaten(player) : null;
			setTarget(stack, target == null ? null : GlobalPos.create(world.getRegistryKey(), target.pos()));
		}
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (user instanceof ServerPlayerEntity player) {
			if (!Realms.isRealm(world)) {
				player.sendMessage(Text.translatable("embervoid.lodestar.cold"), true);
			} else {
				RealmArenas.Location target = RealmArenas.nearestUnbeaten(player);
				if (target == null) {
					player.sendMessage(Text.translatable("embervoid.lodestar.none"), true);
				} else {
					int distance = (int) Math.sqrt(target.pos().getSquaredDistance(player.getX(), 0, player.getZ()));
					player.sendMessage(Text.translatable("embervoid.lodestar.distance", distance), true);
				}
			}
		}
		return TypedActionResult.success(stack, world.isClient);
	}

	/** Where the needle points, or null to let it spin. */
	@Nullable
	public static GlobalPos getTarget(ItemStack stack) {
		NbtCompound nbt = stack.getNbt();
		if (nbt == null || !nbt.contains(TARGET) || !nbt.contains(DIMENSION)) {
			return null;
		}
		Identifier dimension = Identifier.tryParse(nbt.getString(DIMENSION));
		return dimension == null ? null : GlobalPos.create(RegistryKey.of(RegistryKeys.WORLD, dimension), BlockPos.fromLong(nbt.getLong(TARGET)));
	}

	private static void setTarget(ItemStack stack, @Nullable GlobalPos target) {
		GlobalPos current = getTarget(stack);
		if (target == null ? current == null : target.equals(current)) {
			return; // unchanged: do not resend the stack every second
		}
		if (target == null) {
			stack.removeSubNbt(TARGET);
			stack.removeSubNbt(DIMENSION);
		} else {
			stack.getOrCreateNbt().putLong(TARGET, target.getPos().asLong());
			stack.getOrCreateNbt().putString(DIMENSION, target.getDimension().getValue().toString());
		}
	}
}
