package com.embervoid.entity;

import com.embervoid.player.PlayerData;
import com.embervoid.player.PlayerDataManager;
import com.embervoid.registry.ModEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * What a player leaves behind on death: their items (by inventory slot) and XP. It does not move, cannot be hurt and
 * is saved with the chunk like any entity. Only its owner can reclaim it, by right-clicking it.
 */
public class AshRemnantEntity extends Entity {
	private DefaultedList<ItemStack> items = DefaultedList.of();
	private int experience;
	@Nullable
	private UUID owner;
	private String ownerName = "";

	public AshRemnantEntity(EntityType<? extends AshRemnantEntity> type, World world) {
		super(type, world);
		setNoGravity(true);
	}

	/**
	 * Moves the player's items and/or XP into a new remnant at {@code pos} and records it as their remnant.
	 * The caller decides which of the two to take (config {@code remnant.mode}) and where to put it.
	 */
	public static AshRemnantEntity create(ServerPlayerEntity player, Vec3d pos, boolean takeItems, boolean takeExperience) {
		AshRemnantEntity remnant = new AshRemnantEntity(ModEntities.ASH_REMNANT, player.getServerWorld());
		remnant.setPosition(pos);
		remnant.owner = player.getUuid();
		remnant.ownerName = player.getGameProfile().getName();
		remnant.setCustomName(Text.translatable("embervoid.remnant.name", remnant.ownerName));
		remnant.setCustomNameVisible(true);

		PlayerInventory inventory = player.getInventory();
		remnant.items = DefaultedList.ofSize(inventory.size(), ItemStack.EMPTY);
		if (takeItems) {
			for (int i = 0; i < inventory.size(); i++) {
				remnant.items.set(i, inventory.removeStack(i));
			}
		}
		if (takeExperience) {
			remnant.experience = totalExperience(player);
			player.experienceProgress = 0;
			player.totalExperience = 0;
			player.setExperienceLevel(0);
		}

		player.getServerWorld().spawnEntity(remnant);
		PlayerDataManager.get(player).remnantId = remnant.getUuid();
		PlayerDataManager.sync(player);
		return remnant;
	}

	/** Exact XP points for the player's level and bar progress (vanilla's totalExperience drifts after /xp). */
	public static int totalExperience(PlayerEntity player) {
		int total = 0;
		for (int level = 0; level < player.experienceLevel; level++) {
			total += experienceToNextLevel(level);
		}
		return total + Math.round(player.experienceProgress * experienceToNextLevel(player.experienceLevel));
	}

	private static int experienceToNextLevel(int level) {
		if (level >= 30) {
			return 112 + (level - 30) * 9;
		}
		return level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
	}

	@Nullable
	public UUID getOwner() {
		return owner;
	}

	@Override
	public ActionResult interact(PlayerEntity player, Hand hand) {
		if (getWorld().isClient) {
			return ActionResult.SUCCESS;
		}
		if (!player.getUuid().equals(owner)) {
			player.sendMessage(Text.translatable("embervoid.remnant.not_yours", ownerName).formatted(Formatting.GRAY), true);
			return ActionResult.CONSUME;
		}
		giveBack((ServerPlayerEntity) player);
		return ActionResult.CONSUME;
	}

	/**
	 * Returns items to their original slots where those are free, then the rest to any free slot or the ground
	 * (two passes, so an overflowing item never takes the slot of one still waiting), then XP.
	 */
	private void giveBack(ServerPlayerEntity player) {
		PlayerInventory inventory = player.getInventory();
		List<ItemStack> overflow = new ArrayList<>();
		for (int i = 0; i < items.size(); i++) {
			ItemStack stack = items.get(i);
			if (stack.isEmpty()) {
				continue;
			}
			if (i < inventory.size() && inventory.getStack(i).isEmpty()) {
				inventory.setStack(i, stack);
			} else {
				overflow.add(stack);
			}
		}
		overflow.forEach(inventory::offerOrDrop);
		items.clear();
		player.addExperience(experience);
		experience = 0;

		PlayerData data = PlayerDataManager.get(player);
		if (getUuid().equals(data.remnantId)) {
			data.remnantId = null;
			PlayerDataManager.sync(player);
		}
		player.sendMessage(Text.translatable("embervoid.remnant.recovered").formatted(Formatting.GOLD), true);
		ServerWorld world = player.getServerWorld();
		world.playSound(null, getX(), getY(), getZ(), SoundEvents.PARTICLE_SOUL_ESCAPE, SoundCategory.PLAYERS, 1.0f, 0.8f);
		world.spawnParticles(ParticleTypes.SOUL, getX(), getY() + 0.6, getZ(), 20, 0.3, 0.4, 0.3, 0.02);
		discard();
	}

	@Override
	public void tick() {
		super.tick();
		World world = getWorld();
		if (world.isClient && random.nextInt(3) == 0) {
			double x = getX() + (random.nextDouble() - 0.5) * 0.6;
			double z = getZ() + (random.nextDouble() - 0.5) * 0.6;
			world.addParticle(ParticleTypes.ASH, x, getY() + random.nextDouble() * 1.2, z, 0, 0.02, 0);
			if (random.nextInt(4) == 0) {
				world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, getY() + 0.1, z, 0, 0.03, 0);
			}
			if (random.nextInt(6) == 0) {
				world.addParticle(ParticleTypes.SMOKE, x, getY() + 0.4, z, 0, 0.04, 0);
			}
		}
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean handleAttack(Entity attacker) {
		return true;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	/** Lets the crosshair target it so it can be right-clicked. */
	@Override
	public boolean canHit() {
		return !isRemoved();
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void initDataTracker() {
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		items = DefaultedList.ofSize(nbt.getInt("Size"), ItemStack.EMPTY);
		Inventories.readNbt(nbt, items);
		experience = nbt.getInt("Experience");
		owner = nbt.containsUuid("Owner") ? nbt.getUuid("Owner") : null;
		ownerName = nbt.getString("OwnerName");
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
		nbt.putInt("Size", items.size());
		Inventories.writeNbt(nbt, items);
		nbt.putInt("Experience", experience);
		if (owner != null) {
			nbt.putUuid("Owner", owner);
		}
		nbt.putString("OwnerName", ownerName);
	}
}
