package com.embervoid.entity;

import com.embervoid.config.EmbervoidConfig;
import com.embervoid.player.PlayerData;
import com.embervoid.player.PlayerDataManager;
import com.embervoid.player.XpUtil;
import net.minecraft.block.piston.PistonBehavior;
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
 * What a player leaves behind on death: their items (by inventory slot) and XP points. It does not move, cannot be
 * hurt, killed, blown up or lost to the void, and is saved with the chunk like any entity. Only its owner can reclaim
 * it, by right-clicking it.
 *
 * <p>{@link RemnantRegistry} records each player's current remnant. When a player dies again, the previous one is
 * retired per {@code remnant.secondDeath}: with {@code burn_xp} its XP burns and it stays as an item-only remnant
 * until picked up, with {@code burn_all} it burns. A remnant that is not loaded at that moment finds out when its
 * chunk loads and next ticks, and resolves itself then.
 */
public class AshRemnantEntity extends Entity {
	private DefaultedList<ItemStack> items = DefaultedList.of();
	private int experience;
	@Nullable
	private UUID owner;
	private String ownerName = "";
	/** Retired by a later death under {@code burn_xp}: holds items only and no longer checks the registry. */
	private boolean itemOnly;
	/** Ticks until the next check against the registry; 0 so a freshly loaded remnant checks at once. */
	private int registryCheckDelay;

	public AshRemnantEntity(EntityType<? extends AshRemnantEntity> type, World world) {
		super(type, world);
		setNoGravity(true);
	}

	/**
	 * Moves the player's items and/or XP into a new remnant at {@code pos} and records it as their current remnant.
	 * Call {@link #retireCurrent} first. The caller decides what to take (config {@code remnant.mode}) and where.
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
			remnant.experience = XpUtil.getPoints(player);
			XpUtil.setPoints(player, 0);
		}

		RemnantRegistry.get(player.getServer()).setRemnant(player.getUuid(), remnant.getUuid());
		player.getServerWorld().spawnEntity(remnant);
		PlayerDataManager.get(player).remnantId = remnant.getUuid();
		PlayerDataManager.sync(player);
		return remnant;
	}

	/** What {@link #retireCurrent} did. */
	public enum Retired {
		/** The player had no current remnant. */
		NONE,
		/** Its XP burned; its items stay (or will, once its chunk loads). */
		XP_BURNED,
		/** It burned with everything in it (or will, once its chunk loads). */
		BURNED
	}

	/**
	 * Retires the player's current remnant per {@code remnant.secondDeath}: at once if it is loaded, otherwise when
	 * its chunk loads (it is no longer the registry's current one).
	 */
	public static Retired retireCurrent(ServerPlayerEntity player) {
		RemnantRegistry registry = RemnantRegistry.get(player.getServer());
		UUID current = registry.getRemnant(player.getUuid());
		if (current == null) {
			return Retired.NONE;
		}
		registry.clearRemnant(player.getUuid(), current);
		for (ServerWorld world : player.getServer().getWorlds()) {
			if (world.getEntity(current) instanceof AshRemnantEntity remnant) {
				remnant.retire();
			}
		}
		PlayerDataManager.get(player).remnantId = null;
		PlayerDataManager.sync(player);
		return EmbervoidConfig.get().remnant().secondDeath() == EmbervoidConfig.SecondDeath.BURN_ALL
				? Retired.BURNED : Retired.XP_BURNED;
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
		XpUtil.addPoints(player, experience);
		experience = 0;

		RemnantRegistry.get(player.getServer()).clearRemnant(player.getUuid(), getUuid());
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

	/** Applies {@code remnant.secondDeath} to this no-longer-current remnant. */
	private void retire() {
		if (EmbervoidConfig.get().remnant().secondDeath() == EmbervoidConfig.SecondDeath.BURN_ALL || isEmptyOfItems()) {
			burn();
			return;
		}
		if (experience > 0 && getWorld() instanceof ServerWorld world) {
			world.spawnParticles(ParticleTypes.SMOKE, getX(), getY() + 0.6, getZ(), 12, 0.25, 0.4, 0.25, 0.01);
			world.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5f, 1.2f);
		}
		experience = 0;
		itemOnly = true;
	}

	private boolean isEmptyOfItems() {
		return items.stream().allMatch(ItemStack::isEmpty);
	}

	/** Destroys the remnant and everything in it. */
	public void burn() {
		if (getWorld() instanceof ServerWorld world) {
			world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.5, getZ(), 15, 0.3, 0.4, 0.3, 0.01);
			world.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.8f, 0.6f);
		}
		items.clear();
		experience = 0;
		discard();
	}

	@Override
	public void tick() {
		super.tick();
		World world = getWorld();
		if (world instanceof ServerWorld serverWorld && owner != null && !itemOnly && --registryCheckDelay <= 0) {
			registryCheckDelay = 20;
			if (!getUuid().equals(RemnantRegistry.get(serverWorld.getServer()).getRemnant(owner))) {
				retire();
				if (isRemoved()) {
					return;
				}
			}
		}
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

	/** {@code /kill} and similar do nothing; only reclaiming or retiring removes a remnant. */
	@Override
	public void kill() {
	}

	@Override
	protected void tickInVoid() {
	}

	@Override
	public boolean isImmuneToExplosion() {
		return true;
	}

	@Override
	public PistonBehavior getPistonBehavior() {
		return PistonBehavior.IGNORE;
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
		itemOnly = nbt.getBoolean("ItemOnly");
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
		nbt.putBoolean("ItemOnly", itemOnly);
	}
}
