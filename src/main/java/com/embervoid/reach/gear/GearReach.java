package com.embervoid.reach.gear;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Weapon reach other than vanilla's (1.20.1 has no reach attribute) and the charge of the swing a weapon effect
 * belongs to.
 *
 * <p>Longer reach (the Scourge's 6 blocks) is a server raycast on the swing: the client only attacks entities within 3
 * blocks, but it always sends the swing, so {@code GearServerPlayNetworkHandlerMixin} hands it here and an entity in
 * reach and in sight gets a normal {@link PlayerEntity#attack}. A swing that already came with a vanilla attack this
 * tick (or the one before, if the packets straddle a tick) is left alone. Shorter reach (the Rapier's 2.5) refuses vanilla attacks beyond it.
 */
public final class GearReach {
	/** Vanilla survival attack reach. */
	public static final double VANILLA_REACH = 3.0;

	private static final Map<PlayerEntity, Long> LAST_ATTACK_TICK = new WeakHashMap<>();
	private static final Map<PlayerEntity, Float> LAST_CHARGE = new WeakHashMap<>();

	/** The attack charge (0–1) of the player's latest melee attack, read by weapon effects after the hit. */
	public static float lastCharge(PlayerEntity player) {
		return LAST_CHARGE.getOrDefault(player, 1.0f);
	}

	static double reach(PlayerEntity player) {
		return player.getMainHandStack().getItem() instanceof GearWeaponItem weapon && weapon.spec().reach() > 0
				? weapon.spec().reach() : VANILLA_REACH;
	}

	/** Called before every server-side melee attack of a player, vanilla or ours. */
	static ActionResult beforeAttack(PlayerEntity player, Entity target) {
		if (player.getWorld().isClient() || player.isSpectator()) {
			return ActionResult.PASS;
		}
		double reach = reach(player);
		if (reach < VANILLA_REACH && !player.getAbilities().creativeMode) {
			Box box = target.getBoundingBox();
			if (box.squaredMagnitude(player.getEyePos()) > reach * reach) {
				return ActionResult.FAIL;
			}
		}
		LAST_ATTACK_TICK.put(player, player.getWorld().getTime());
		LAST_CHARGE.put(player, player.getAttackCooldownProgress(0.5f));
		return ActionResult.PASS;
	}

	/** {@link #beforeAttack} for GameTests: what the attack event answers for this player and target. */
	public static ActionResult beforeAttackForTest(PlayerEntity player, Entity target) {
		return beforeAttack(player, target);
	}

	/** A swing packet: attacks what a long-reach weapon points at, unless the swing already hit something. */
	public static void onSwing(ServerPlayerEntity player, Hand hand) {
		double reach = reach(player);
		if (hand != Hand.MAIN_HAND || reach <= VANILLA_REACH || player.isSpectator()
				|| player.getWorld().getTime() - LAST_ATTACK_TICK.getOrDefault(player, Long.MIN_VALUE / 2) <= 1) {
			return;
		}
		LivingEntity target = raycast(player, reach);
		if (target != null && beforeAttack(player, target) == ActionResult.PASS) {
			player.attack(target);
		}
	}

	/** The first living entity along the player's look within {@code reach}, not behind a block. */
	@Nullable
	public static LivingEntity raycast(PlayerEntity player, double reach) {
		Vec3d eye = player.getEyePos();
		Vec3d end = eye.add(player.getRotationVec(1.0f).multiply(reach));
		HitResult block = player.getWorld().raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE, player));
		if (block.getType() != HitResult.Type.MISS) {
			end = block.getPos();
		}
		Box area = player.getBoundingBox().stretch(end.subtract(eye)).expand(1);
		EntityHitResult hit = ProjectileUtil.raycast(player, eye, end, area,
				e -> e instanceof LivingEntity living && living.isAlive() && !e.isSpectator() && e.canHit() && !player.isTeammate(e),
				eye.squaredDistanceTo(end));
		return hit == null ? null : (LivingEntity) hit.getEntity();
	}

	static void register() {
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> beforeAttack(player, entity));
	}

	private GearReach() {}
}
