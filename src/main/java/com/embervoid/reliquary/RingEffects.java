package com.embervoid.reliquary;

import com.embervoid.Embervoid;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

import java.util.UUID;

/**
 * What the six Realm 1 rings do while worn (PLAN 11.12, DESIGN §4.5). Numbers in config {@code reliquary.rings}.
 * Movement is decided by the client for its own player, so the movement rings work on both sides: the client reads
 * its synced Reliquary. Called from {@code ReliquaryLivingEntityMixin}, {@code ReliquaryPlayerEntityMixin}, the
 * client tick ({@code ReliquaryClient}) and the damage event below.
 */
public final class RingEffects {
	/** Realm ores (track reach-world, PLAN 9.5): Delver's Ring mines them faster. */
	public static final TagKey<Block> REALM_ORES = TagKey.of(RegistryKeys.BLOCK, Embervoid.id("realm_ores"));

	private static final UUID COURIER_SPEED = UUID.fromString("7c1e2a40-5b6d-4f3a-9e21-3d8b0c4f6a11");

	public static boolean wears(PlayerEntity player, Item ring) {
		return ReliquaryManager.has(player, ring);
	}

	private static ReliquaryConfig.Rings config() {
		return ReliquaryConfig.get().rings();
	}

	/**
	 * Courier's Ring: sprinting is {@code courierSprintBonus} faster. A movement speed modifier on top of vanilla's
	 * sprint modifier, added and removed with sprinting (both sides) and when the ring is put on or off.
	 */
	public static void updateSprint(PlayerEntity player) {
		EntityAttributeInstance speed = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
		if (speed == null) {
			return;
		}
		boolean active = player.isSprinting() && wears(player, ReliquaryItems.COURIERS_RING);
		if (active == (speed.getModifier(COURIER_SPEED) != null)) {
			return;
		}
		speed.removeModifier(COURIER_SPEED);
		if (active) {
			speed.addTemporaryModifier(new EntityAttributeModifier(COURIER_SPEED, "embervoid:couriers_ring",
					config().courierSprintBonus(), EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
		}
	}

	/**
	 * Ring of Ash Feathers: blocks of fall that never hurt (vanilla 3). Subtracted from the fall distance before
	 * vanilla's own 3, so falls up to {@code featherSafeFall} are free and longer ones hurt as much less.
	 */
	public static float ignoredFallDistance(LivingEntity entity) {
		if (entity instanceof PlayerEntity player && wears(player, ReliquaryItems.RING_OF_ASH_FEATHERS)) {
			return Math.max(0, config().featherSafeFall() - 3);
		}
		return 0;
	}

	/**
	 * Ring of Ash Feathers: the fastest the player falls, in blocks per tick (vanilla terminal speed ~3.9), or 0
	 * when the ring does nothing now. Not while sneaking (to drop fast on purpose), flying, gliding or swimming.
	 */
	public static double maxFallSpeed(PlayerEntity player) {
		if (!wears(player, ReliquaryItems.RING_OF_ASH_FEATHERS) || player.isSneaking() || player.isOnGround()
				|| player.getAbilities().flying || player.isFallFlying() || player.isTouchingWater() || player.isInLava()
				|| player.isClimbing()) {
			return 0;
		}
		return config().featherMaxFallSpeed();
	}

	/**
	 * Ring of the Fog Stalker: how much further out the realm fog starts and ends (the 9.1 fog API's clearing
	 * factor; boss arena fog is not clearable, so arenas stay as they are). 1 without the ring.
	 */
	public static float fogClearing(PlayerEntity player) {
		return wears(player, ReliquaryItems.RING_OF_THE_FOG_STALKER) ? config().fogStalkerClearing() : 1;
	}

	/** Delver's Ring: mining speed multiplier on realm ores, as Haste of {@code delverHasteLevel}. */
	public static float miningMultiplier(PlayerEntity player, BlockState state) {
		if (state.isIn(REALM_ORES) && wears(player, ReliquaryItems.DELVERS_RING)) {
			return 1 + 0.2f * config().delverHasteLevel();
		}
		return 1;
	}

	/** Drowned Man's Ring: breathes under water like water breathing. */
	public static boolean breathesUnderwater(LivingEntity entity) {
		return entity instanceof PlayerEntity player && wears(player, ReliquaryItems.DROWNED_MANS_RING);
	}

	static void register() {
		// Ring of Embers: the wearer's melee hits set the target on fire.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (source.isOf(DamageTypes.PLAYER_ATTACK) && source.getAttacker() instanceof PlayerEntity player
					&& source.getSource() == player && wears(player, ReliquaryItems.RING_OF_EMBERS)
					&& !entity.isFireImmune()) {
				entity.setOnFireFor(config().embersFireSeconds());
			}
			return true;
		});
		ReliquaryManager.CHANGED.register(RingEffects::updateSprint);
	}

	private RingEffects() {}
}
