package com.embervoid.reach.gear;

import com.embervoid.fx.CombatFx;
import com.embervoid.fx.particle.EmberParticles;
import com.embervoid.reach.mob.ReachMob;
import com.embervoid.mixin.GearLivingEntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Arm;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Warden's Scourge (DESIGN §6.1, PLAN 17.4): a whip that hits every enemy along its lash ({@link WhipArc}) out to its
 * reach; the tip, the last {@code gear.weapons.whipTipBand} blocks, cracks for {@code whipTipMultiplier} with its own
 * sound and a flash. Bleeding on every hit; Lash.
 *
 * <p>A swing, a click on a mob or a swing at the air ({@link GearReach}), is one {@link #crack}: each mob on the lash
 * gets a normal attack at the swing's own charge (enchantments, stat scaling and Bleeding included), and the swing
 * costs one durability however many it hits. The lash stops at the first block in its way.
 *
 * <p>The crack is the weapon's signature (docs/art-direction.md §4): the tip's report is a burst, a shockwave ring on
 * the ground, a spray of cinder sparks and a kick of the camera, every mob it bit sprays blood, and a mob hit with
 * the tip is stopped in its tracks for {@code gear.weapons.whipTipStunTicks} with its wind-up broken. A crack that
 * meets nothing still snaps in the air at the lash's end.
 */
public class WardensScourgeItem extends GearWeaponItem {
	/** Pale yellow, the crack's flash. */
	private static final int CRACK_COLOR = 0xFFE8B0;
	private static final Map<Double, WhipArc> ARCS = new HashMap<>();
	/** The mob a crack is attacking with the tip, while that attack is dealt. */
	private static final Map<PlayerEntity, Entity> TIP = new WeakHashMap<>();
	/** Players in the middle of a crack, and whether it has cost its durability yet. */
	private static final Map<PlayerEntity, Boolean> WORN = new WeakHashMap<>();

	/** A mob on the lash: how far along the lash (from the hand) it reached into the mob, and that point. */
	public record Hit(LivingEntity target, double along, Vec3d at) {
	}

	public WardensScourgeItem(Settings settings, Spec spec) {
		super(settings, spec, GearAbilities.LASH);
	}

	public static boolean holds(PlayerEntity player) {
		return player.getMainHandStack().getItem() instanceof WardensScourgeItem;
	}

	/** This whip's lash. */
	public WhipArc arc() {
		return arc(spec().reach());
	}

	/** The lash of a whip reaching {@code reach} blocks. */
	public static WhipArc arc(double reach) {
		return ARCS.computeIfAbsent(reach, WhipArc::new);
	}

	/** What a swing meets: the mobs on the lash and where the lash ends (its tip, or the block that stopped it). */
	public record Lash(List<Hit> hits, Vec3d end) {
	}

	/**
	 * The mobs on the lash of a swing {@code player} makes now, from the hand out; each with the furthest point along
	 * the lash that reached it. The lash stops at the first block between one sample and the next (or between the eyes
	 * and the hand).
	 */
	public static List<Hit> struck(PlayerEntity player) {
		return lash(player).hits();
	}

	/** {@link #struck}, with the point the lash ends at. */
	public static Lash lash(PlayerEntity player) {
		WhipArc arc = arc(GearReach.reach(player));
		World world = player.getWorld();
		Vec3d eye = player.getEyePos();
		boolean leftHanded = player.getMainArm() == Arm.LEFT;
		Map<LivingEntity, Hit> hits = new LinkedHashMap<>();
		Vec3d from = eye;
		for (WhipArc.Sample sample : arc.samples()) {
			Vec3d at = WhipArc.toWorld(eye, player.getYaw(), player.getPitch(), leftHanded, sample.local());
			HitResult block = world.raycast(new RaycastContext(from, at, RaycastContext.ShapeType.COLLIDER,
					RaycastContext.FluidHandling.NONE, player));
			if (block.getType() != HitResult.Type.MISS) {
				from = block.getPos();
				break;
			}
			for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, new Box(at, at).expand(WhipArc.RADIUS),
					e -> canStrike(player, e))) {
				hits.put(target, new Hit(target, sample.along(), at));
			}
			from = at;
		}
		return new Lash(List.copyOf(hits.values()), from);
	}

	/** Whether the lash hits {@code target}: not the player, their pets, their mount or a teammate. */
	static boolean canStrike(PlayerEntity player, Entity target) {
		return target != player && target.isAlive() && !target.isSpectator() && target.canHit() && !player.isTeammate(target)
				&& !player.isConnectedThroughVehicle(target)
				&& !(target instanceof Tameable pet && pet.getOwner() == player)
				&& !(target instanceof ArmorStandEntity stand && stand.isMarker());
	}

	/**
	 * One swing: attacks every mob on the lash, and {@code aimed} (the mob the click was on, if any) even where the lash
	 * passed by it. The swing that follows it is no second one ({@link GearReach#noteAttack}).
	 */
	public static void crack(ServerPlayerEntity player, @Nullable Entity aimed) {
		GearReach.noteAttack(player);
		Lash lash = lash(player);
		List<Hit> hits = new ArrayList<>(lash.hits());
		if (aimed instanceof LivingEntity living && canStrike(player, living) && hits.stream().noneMatch(hit -> hit.target() == living)) {
			Vec3d eye = player.getEyePos();
			Box box = living.getBoundingBox();
			Vec3d nearest = new Vec3d(MathHelper.clamp(eye.x, box.minX, box.maxX), MathHelper.clamp(eye.y, box.minY, box.maxY),
					MathHelper.clamp(eye.z, box.minZ, box.maxZ));
			hits.add(0, new Hit(living, eye.distanceTo(nearest), nearest));
		}
		ServerWorld world = player.getServerWorld();
		if (hits.isEmpty()) {
			airCrack(world, player, lash.end());
			return;
		}
		WhipArc arc = arc(GearReach.reach(player));
		float band = GearConfig.get().weapons().whipTipBand();
		GearLivingEntityAccessor cooldown = (GearLivingEntityAccessor) player;
		// Every vanilla attack resets the cooldown: each mob gets the charge the swing had.
		int charged = cooldown.embervoid$lastAttackedTicks();
		Vec3d crackAt = null;
		WORN.put(player, false);
		try {
			for (Hit hit : hits) {
				cooldown.embervoid$setLastAttackedTicks(charged);
				if (GearReach.beforeAttack(player, hit.target()) != ActionResult.PASS) {
					continue;
				}
				boolean tip = arc.inTip(hit.along(), band);
				if (tip) {
					TIP.put(player, hit.target());
				}
				float before = hit.target().getHealth();
				player.attack(hit.target());
				TIP.remove(player);
				boolean landed = hit.target().getHealth() < before || !hit.target().isAlive();
				if (landed) {
					bite(world, hit, tip);
				}
				if (tip && landed) {
					stun(hit.target());
				}
				if (tip && crackAt == null) {
					crackAt = hit.at();
				}
			}
		} finally {
			WORN.remove(player);
			TIP.remove(player);
		}
		if (crackAt != null) {
			crackEffects(world, player, crackAt, 1);
		} else {
			airCrack(world, player, lash.end());
		}
	}

	/** The lash bit {@code hit}: blood where it caught the mob, a hot spray of sparks at a tip hit. */
	private static void bite(ServerWorld world, Hit hit, boolean tip) {
		Vec3d at = hit.at();
		EmberParticles.throwAll(world, EmberParticles.BLOOD_DROP, at, tip ? 10 : 5, new Vec3d(0, 0.15, 0), 0.18);
		if (tip) {
			EmberParticles.throwAll(world, EmberParticles.CINDER_SPARK, at, 8, new Vec3d(0, 0.2, 0), 0.3);
		}
	}

	/** A tip hit stops the mob where it stands and breaks its wind-up ({@code gear.weapons.whipTipStunTicks}). */
	static void stun(LivingEntity target) {
		int ticks = GearConfig.get().weapons().whipTipStunTicks();
		if (ticks <= 0 || !target.isAlive()) {
			return;
		}
		target.setVelocity(0, Math.min(0, target.getVelocity().y), 0);
		target.velocityModified = true;
		target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, ticks, 5, false, false, false));
		if (target instanceof ReachMob mob) {
			mob.cancelMove();
		}
	}

	/** The crack hit nothing: the tip still snaps where the lash ends, a little quieter. */
	private static void airCrack(ServerWorld world, ServerPlayerEntity player, Vec3d end) {
		crackEffects(world, player, end, 0.55f);
	}

	/**
	 * The tip's report at {@code at}: the crack's sound, a burst flash, a shockwave ring on the ground below, sparks
	 * thrown out, a soot puff, and a kick of the swinger's camera; {@code strength} 1 for a hit, less for air.
	 */
	private static void crackEffects(ServerWorld world, ServerPlayerEntity player, Vec3d at, float strength) {
		world.playSound(null, at.x, at.y, at.z, GearSounds.WHIP_CRACK, SoundCategory.PLAYERS, 0.6f + 0.5f * strength,
				1.0f + (1 - strength) * 0.3f);
		EmberParticles.burst(world, at, 0.5f + 0.7f * strength, CRACK_COLOR);
		Vec3d ground = groundBelow(world, player, at);
		if (ground != null) {
			EmberParticles.shockwave(world, ground, 0.8f + 1.4f * strength, CRACK_COLOR);
		}
		EmberParticles.ringOut(world, EmberParticles.CINDER_SPARK, at, Math.round(6 + 12 * strength), 0.28, 0.12);
		EmberParticles.spawn(world, EmberParticles.SOOT, at, 2, 0.1, 0.02);
		if (strength >= 1) {
			CombatFx.flash(world, at, 0.5f, CRACK_COLOR);
			CombatFx.shake(world, at, 0.45f, 7, 8);
		}
		CombatFx.shake(player, 0.25f * strength, 5);
	}

	/** The ground within three blocks under {@code at}, or null. */
	@Nullable
	private static Vec3d groundBelow(ServerWorld world, PlayerEntity player, Vec3d at) {
		HitResult hit = world.raycast(new RaycastContext(at, at.add(0, -3, 0), RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE, player));
		return hit.getType() == HitResult.Type.MISS ? null : hit.getPos();
	}

	/** Whether the attack {@code player} is dealing to {@code target} right now is a hit with the tip. */
	public static boolean tipHit(PlayerEntity player, Entity target) {
		return TIP.get(player) == target;
	}

	/** Whether {@code player} is in the middle of a crack: its own attacks must not start another one. */
	public static boolean cracking(PlayerEntity player) {
		return WORN.containsKey(player);
	}

	/** The first mob a crack hits costs the durability; the others are the same swing. */
	@Override
	protected void wear(ItemStack stack, LivingEntity attacker) {
		Boolean worn = attacker instanceof PlayerEntity player ? WORN.get(player) : null;
		if (worn == null) {
			super.wear(stack, attacker);
		} else if (!worn) {
			WORN.put((PlayerEntity) attacker, true);
			super.wear(stack, attacker);
		}
	}
}
