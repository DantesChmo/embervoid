package com.embervoid.reach.gear;

import com.embervoid.fx.CombatFx;
import com.embervoid.mixin.GearLivingEntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
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

	/**
	 * The mobs on the lash of a swing {@code player} makes now, from the hand out; each with the furthest point along
	 * the lash that reached it. The lash stops at the first block between one sample and the next (or between the eyes
	 * and the hand).
	 */
	public static List<Hit> struck(PlayerEntity player) {
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
				break;
			}
			for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, new Box(at, at).expand(WhipArc.RADIUS),
					e -> canStrike(player, e))) {
				hits.put(target, new Hit(target, sample.along(), at));
			}
			from = at;
		}
		return List.copyOf(hits.values());
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
		List<Hit> hits = new ArrayList<>(struck(player));
		if (aimed instanceof LivingEntity living && canStrike(player, living) && hits.stream().noneMatch(hit -> hit.target() == living)) {
			Vec3d eye = player.getEyePos();
			Box box = living.getBoundingBox();
			Vec3d nearest = new Vec3d(MathHelper.clamp(eye.x, box.minX, box.maxX), MathHelper.clamp(eye.y, box.minY, box.maxY),
					MathHelper.clamp(eye.z, box.minZ, box.maxZ));
			hits.add(0, new Hit(living, eye.distanceTo(nearest), nearest));
		}
		if (hits.isEmpty()) {
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
				player.attack(hit.target());
				TIP.remove(player);
				if (tip && crackAt == null) {
					crackAt = hit.at();
				}
			}
		} finally {
			WORN.remove(player);
			TIP.remove(player);
		}
		if (crackAt != null) {
			crackEffects(player.getServerWorld(), crackAt);
		}
	}

	private static void crackEffects(ServerWorld world, Vec3d at) {
		world.playSound(null, at.x, at.y, at.z, GearSounds.WHIP_CRACK, SoundCategory.PLAYERS, 1.0f, 1.0f);
		world.spawnParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 12, 0.1, 0.1, 0.1, 0.4);
		CombatFx.flash(world, at, 0.4f, CRACK_COLOR);
	}

	/** Whether the attack {@code player} is dealing to {@code target} right now is a hit with the tip. */
	public static boolean tipHit(PlayerEntity player, Entity target) {
		return TIP.get(player) == target;
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
