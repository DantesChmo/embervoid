package com.embervoid.reach.boss;

import com.embervoid.boss.AreaHazard;
import com.embervoid.boss.BossFight;
import com.embervoid.boss.BossHits;
import com.embervoid.boss.FloodHazard;
import com.embervoid.boss.Telegraph;
import com.embervoid.boss.TelegraphShape;
import com.embervoid.reach.mob.ReachMobs;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Crypt Keeper, boss of the Sunken Necropolis (DESIGN §4.5, PLAN 12.3): 4 blocks tall, halberd and lantern, Lv 24.
 * What it teaches: light — braziers against darkness and moths.
 *
 * <ul>
 *   <li><b>P1.</b> Halberd combo: two cone swings, then an unblockable overhead along a line that breaks a raised
 *   shield. Sweep: a ring around it. Lantern throw: a falling shadow at the target, then a fire pool there.</li>
 *   <li><b>P2 at 60 %.</b> The lantern shatters: Darkness on the fighters, the water rises one block on the lower
 *   floor, and the four braziers on the pedestals can be lit (flint and steel, fire charge, a burning arrow). In
 *   the dark it takes half damage, in brazier light (block light ≥ {@value #LIT_LEVEL} where it stands) full.
 *   Crypt Moths come for the lit braziers and smother them. The lantern throw becomes a lunging thrust.</li>
 *   <li><b>P3 at 30 %.</b> The water reaches two blocks (only walkways, the dais and the pedestals stay dry) and
 *   Restless Bones rise from it.</li>
 * </ul>
 *
 * Base damage per move (level 24 and Normal scale it ×2.69): swings 5, thrust 7, sweep 7, overhead 10, lantern 5 +
 * a fire pool of 2 per second. Base HP 180 (~428 at Lv 24). Every block it changes (water, braziers) comes back when
 * the fight ends.
 */
public class CryptKeeperEntity extends ReachBoss {
	/** Block light where it stands at or above which it takes full damage in P2 and P3. */
	public static final int LIT_LEVEL = 8;
	/** Damage taken in the dark (P2, P3). */
	public static final float DARK_DAMAGE = 0.5f;

	public static final String SWING_1 = "attack.halberd_1";
	public static final String SWING_2 = "attack.halberd_2";
	public static final String OVERHEAD = "attack.overhead";
	public static final String SWEEP = "attack.sweep";
	public static final String LANTERN_THROW = "attack.lantern_throw";
	public static final String THRUST = "attack.thrust";
	public static final String LANTERN_SHATTER = "misc.lantern_shatter";
	public static final String RAISE_DEAD = "misc.raise_dead";

	private static final float SWING_DAMAGE = 5;
	private static final float OVERHEAD_DAMAGE = 10;
	private static final float SWEEP_DAMAGE = 7;
	private static final float THRUST_DAMAGE = 7;
	private static final float LANTERN_DAMAGE = 5;
	private static final float FIRE_POOL_DAMAGE = 2;
	private static final double SWING_RANGE = 4.8;
	private static final double SWEEP_RADIUS = 5;
	private static final double FIRE_POOL_RADIUS = 2.5;

	/** Water: the flood reaches this far from the centre (the sinkhole floor, up to the pit wall). */
	private static final int FLOOD_MARGIN = 7;
	/** Ticks between two waves of moths while a brazier burns, and the most at once. */
	private static final int MOTH_INTERVAL = 300;
	private static final int MAX_MOTHS = 4;
	/** How close a moth gets to a lit brazier to smother it. */
	private static final double SMOTHER_DISTANCE = 1.6;
	/** Restless Bones rising in P3, plus one per fighter beyond the first. */
	private static final int RISEN_BONES = 3;

	private static final DustParticleEffect UNBLOCKABLE = new DustParticleEffect(new Vector3f(0.85f, 0.1f, 0.1f), 1.5f);

	/** Braziers of the arena (found when the fight starts); their lit state is undone with the fight. */
	private final List<BlockPos> braziers = new ArrayList<>();
	private boolean braziersFound;
	private int mothTimer = MOTH_INTERVAL / 2;

	public CryptKeeperEntity(EntityType<? extends CryptKeeperEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 300;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 180)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, SWING_DAMAGE)
				.add(EntityAttributes.GENERIC_ARMOR, 8)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.22)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
	}

	/** Whether it still carries its lantern (P1); the model hides the lantern after it shatters. */
	public boolean hasLantern() {
		return phase() == 0;
	}

	@Override
	public List<String> toggledBones() {
		return List.of("lantern");
	}

	@Override
	public boolean showsBone(String bone) {
		return !bone.equals("lantern") || hasLantern();
	}

	@Override
	public float[] phaseThresholds() {
		return new float[]{0.6f, 0.3f};
	}

	@Override
	public EntityType<? extends MobEntity> scalingAdd() {
		return ReachMobs.RESTLESS_BONES;
	}

	@Override
	public void onPhaseChange(int phase) {
		super.onPhaseChange(phase);
		BossFight fight = fight();
		if (!(getWorld() instanceof ServerWorld world) || fight == null) {
			return;
		}
		if (phase == 0) {
			findBraziers(fight);
			return;
		}
		Telegraph.cancel(this);
		moveCooldown = 30;
		int flood = fight.spec().radius() + FLOOD_MARGIN;
		if (phase == 1) {
			playAnimation(LANTERN_SHATTER);
			world.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 2.0f, 0.6f);
			world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_WARDEN_EMERGE, SoundCategory.HOSTILE, 1.5f, 1.2f);
			world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 2.5, getZ(), 40, 0.6, 0.6, 0.6, 0.05);
			world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 2.5, getZ(), 30, 0.6, 0.6, 0.6, 0.02);
			fight.addHazard(new FloodHazard(flood, 1, 60));
			darken(fight);
			getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.24);
		} else if (phase == 2) {
			playAnimation(RAISE_DEAD);
			world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_SKELETON_HORSE_DEATH, SoundCategory.HOSTILE, 2.0f, 0.5f);
			fight.addHazard(new FloodHazard(flood, 2, 60));
			raiseDead(fight);
			getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.26);
		}
	}

	@Override
	protected void tickBoss() {
		BossFight fight = fight();
		if (fight == null || phase() == 0) {
			return;
		}
		if (age % 40 == 0) {
			darken(fight);
		}
		tickMoths(fight);
	}

	// --- Moves ---

	@Override
	protected int chooseMove(LivingEntity target, double squaredDistance) {
		boolean late = phase() > 0;
		if (squaredDistance <= SWING_RANGE * SWING_RANGE) {
			int crowd = Telegraph.playersIn(this, TelegraphShape.ring(getPos(), SWEEP_RADIUS)).size();
			if (crowd >= 2 || random.nextInt(3) == 0) {
				sweep();
			} else {
				comboSwing(target, 1);
			}
			return late ? 25 : 35;
		}
		if (squaredDistance <= 16 * 16) {
			if (late) {
				thrust(target);
			} else {
				lanternThrow(target);
			}
			return late ? 30 : 40;
		}
		return -1;
	}

	/** One of the three combo hits: 1 and 2 are cone swings, 3 the unblockable overhead. */
	public void comboSwing(LivingEntity target, int hit) {
		face(target.getPos());
		Vec3d facing = directionTo(target.getPos());
		if (hit < 3) {
			TelegraphShape cone = TelegraphShape.cone(getPos(), facing, SWING_RANGE, 55);
			int windup = hit == 1 ? 14 : 10;
			telegraph(cone, windup, hit == 1 ? SWING_1 : SWING_2, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, ParticleTypes.SOUL_FIRE_FLAME,
					player -> player.damage(getDamageSources().mobAttack(this), SWING_DAMAGE));
		} else {
			TelegraphShape line = TelegraphShape.line(getPos(), getPos().add(facing.multiply(6.5)), 1.1);
			telegraph(line, 22, OVERHEAD, SoundEvents.ENTITY_RAVAGER_ROAR, UNBLOCKABLE, player -> {
				BossHits.shieldBreaking(this, player, OVERHEAD_DAMAGE);
				player.addVelocity(0, 0.3, 0);
				player.velocityModified = true;
			});
		}
	}

	@Override
	protected void afterStrike(String animation) {
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive() || !(getWorld() instanceof ServerWorld world)) {
			return;
		}
		if (animation.equals(SWING_1)) {
			comboSwing(target, 2);
		} else if (animation.equals(SWING_2)) {
			comboSwing(target, 3);
		} else if (animation.equals(OVERHEAD)) {
			Vec3d end = getPos().add(getRotationVector().multiply(1, 0, 1).normalize().multiply(4));
			world.spawnParticles(ParticleTypes.EXPLOSION, end.x, end.y + 0.2, end.z, 2, 0.5, 0, 0.5, 0);
			world.playSound(null, end.x, end.y, end.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.0f, 0.8f);
		}
	}

	public void sweep() {
		Vec3d at = getPos();
		telegraph(TelegraphShape.ring(at, SWEEP_RADIUS), 24, SWEEP, SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK, ParticleTypes.SOUL_FIRE_FLAME,
				player -> {
					player.damage(getDamageSources().mobAttack(this), SWEEP_DAMAGE);
					knockAway(player, 1.0, 0.35);
				});
	}

	/** P1: the lantern's fire, thrown where the target stands; a fire pool burns there for 5 s. */
	public void lanternThrow(LivingEntity target) {
		face(target.getPos());
		Vec3d landing = target.getPos();
		TelegraphShape shadow = TelegraphShape.shadow(landing, FIRE_POOL_RADIUS);
		telegraph(shadow, 20, LANTERN_THROW, SoundEvents.ENTITY_BLAZE_SHOOT, ParticleTypes.FLAME, player -> {
			player.damage(getDamageSources().mobAttack(this), LANTERN_DAMAGE);
			player.setOnFireFor(2);
		});
		// The pool itself starts when the throw lands (after the hit above).
		Telegraph.windup(this, 20, null, null, ParticleTypes.FLAME, 0, () -> {
			BossFight fight = fight();
			if (fight != null && getWorld() instanceof ServerWorld world) {
				world.playSound(null, landing.x, landing.y, landing.z, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.HOSTILE, 1.5f, 0.8f);
				fight.addHazard(new AreaHazard(TelegraphShape.ring(landing, FIRE_POOL_RADIUS), 100, 10,
						BossHits.source(this, DamageTypes.IN_FIRE), FIRE_POOL_DAMAGE, ParticleTypes.FLAME, player -> player.setOnFireFor(2)));
			}
		});
	}

	/** P2, P3: a lunging thrust along a line at a far target. */
	public void thrust(LivingEntity target) {
		face(target.getPos());
		Vec3d facing = directionTo(target.getPos());
		Vec3d end = getPos().add(facing.multiply(9));
		telegraph(TelegraphShape.line(getPos(), end, 1.0), 18, THRUST, SoundEvents.ENTITY_RAVAGER_ATTACK, ParticleTypes.SOUL_FIRE_FLAME,
				player -> {
					player.damage(getDamageSources().mobAttack(this), THRUST_DAMAGE);
					knockAway(player, 0.6, 0.2);
				});
		// The lunge carries it forward with the thrust.
		Telegraph.windup(this, 18, null, null, ParticleTypes.SOUL_FIRE_FLAME, 0, () -> {
			setVelocity(facing.x * 1.4, 0.1, facing.z * 1.4);
			velocityModified = true;
		});
	}

	// --- Light, moths, the dead ---

	/** Whether it stands in light (block light ≥ {@value #LIT_LEVEL}): lit braziers, a torch, lava. */
	public boolean isLit() {
		World world = getWorld();
		BlockPos feet = getBlockPos();
		return Math.max(world.getLightLevel(LightType.BLOCK, feet), world.getLightLevel(LightType.BLOCK, feet.up(2))) >= LIT_LEVEL;
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (!getWorld().isClient && phase() > 0 && !isLit() && !source.isOf(DamageTypes.GENERIC_KILL)
				&& !source.isOf(DamageTypes.OUT_OF_WORLD)) {
			amount *= DARK_DAMAGE;
		}
		return super.damage(source, amount);
	}

	/** Darkness on every fighter (refreshed while P2 and P3 last). */
	private void darken(BossFight fight) {
		for (ServerPlayerEntity player : fight.fighters()) {
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 80, 0, false, false), this);
		}
	}

	/** The braziers on the pedestals: every campfire within the arena; their state comes back when the fight ends. */
	private void findBraziers(BossFight fight) {
		if (braziersFound) {
			return;
		}
		braziersFound = true;
		BlockPos center = fight.center();
		int r = fight.spec().radius();
		for (BlockPos pos : BlockPos.iterate(center.add(-r, 0, -r), center.add(r, 6, r))) {
			if (fight.world().getBlockState(pos).isOf(Blocks.CAMPFIRE) || fight.world().getBlockState(pos).isOf(Blocks.SOUL_CAMPFIRE)) {
				braziers.add(pos.toImmutable());
				fight.blocks().remember(pos);
			}
		}
	}

	public List<BlockPos> braziers() {
		return List.copyOf(braziers);
	}

	private boolean isBurning(BlockPos brazier) {
		BlockState state = getWorld().getBlockState(brazier);
		return state.getBlock() instanceof CampfireBlock && state.get(CampfireBlock.LIT);
	}

	/**
	 * While a brazier burns, Crypt Moths come (a wave every 15 s, at most four at once) and fly at the nearest lit
	 * brazier; one that reaches it smothers it.
	 */
	private void tickMoths(BossFight fight) {
		List<BlockPos> lit = braziers.stream().filter(this::isBurning).toList();
		List<MobEntity> moths = fight.adds().stream().filter(add -> add.getType() == ReachMobs.CRYPT_MOTH).toList();
		if (!lit.isEmpty() && --mothTimer <= 0) {
			mothTimer = MOTH_INTERVAL;
			int wave = Math.min(lit.size() >= 2 ? 2 : 1, MAX_MOTHS - moths.size());
			for (int i = 0; i < wave; i++) {
				double angle = random.nextDouble() * Math.PI * 2;
				Vec3d from = fight.centerPos().add(Math.cos(angle) * (fight.spec().radius() - 3), 6, Math.sin(angle) * (fight.spec().radius() - 3));
				fight.spawnAdd(ReachMobs.CRYPT_MOTH, from);
			}
		}
		for (MobEntity moth : moths) {
			BlockPos nearest = null;
			for (BlockPos brazier : lit) {
				if (nearest == null || moth.squaredDistanceTo(Vec3d.ofCenter(brazier)) < moth.squaredDistanceTo(Vec3d.ofCenter(nearest))) {
					nearest = brazier;
				}
			}
			if (nearest == null) {
				continue;
			}
			Vec3d flame = Vec3d.ofCenter(nearest).add(0, 0.6, 0);
			if (moth.getPos().distanceTo(flame) <= SMOTHER_DISTANCE) {
				smother(fight, nearest);
			} else {
				// Drawn to the flame: steer it there whatever its own goals want.
				Vec3d toward = flame.subtract(moth.getPos()).normalize().multiply(0.12);
				moth.setVelocity(moth.getVelocity().multiply(0.7).add(toward));
				moth.velocityModified = true;
			}
		}
	}

	/** A moth reached a lit brazier: it goes out. */
	public void smother(BossFight fight, BlockPos brazier) {
		BlockState state = getWorld().getBlockState(brazier);
		if (!(state.getBlock() instanceof CampfireBlock) || !state.get(CampfireBlock.LIT)) {
			return;
		}
		fight.blocks().set(brazier, state.with(CampfireBlock.LIT, false));
		if (getWorld() instanceof ServerWorld world) {
			world.spawnParticles(ParticleTypes.LARGE_SMOKE, brazier.getX() + 0.5, brazier.getY() + 0.8, brazier.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.01);
			world.playSound(null, brazier, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0f, 1.0f);
		}
	}

	/** P3: Restless Bones rise from the flooded floor, one more per fighter beyond the first. */
	private void raiseDead(BossFight fight) {
		int count = RISEN_BONES + Math.max(0, fight.fighters().size() - 1);
		for (int i = 0; i < count; i++) {
			double angle = Math.PI * 2 * i / count + random.nextDouble() * 0.5;
			double distance = 7 + random.nextDouble() * (fight.spec().radius() - 12);
			@Nullable MobEntity bones = fight.spawnAdd(ReachMobs.RESTLESS_BONES,
					fight.centerPos().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance));
			if (bones != null && getWorld() instanceof ServerWorld world) {
				world.spawnParticles(ParticleTypes.SOUL, bones.getX(), bones.getY() + 0.5, bones.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
			}
		}
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("MothTimer", mothTimer);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		mothTimer = nbt.getInt("MothTimer");
	}

	@Override
	protected List<String> actionAnimations() {
		return List.of(SWING_1, SWING_2, OVERHEAD, SWEEP, LANTERN_THROW, THRUST, LANTERN_SHATTER, RAISE_DEAD);
	}

	@Override
	public boolean isFireImmune() {
		return true;
	}

	@Override
	public boolean canBreatheInWater() {
		return true;
	}

	/** Test hook: whether any of its arena's braziers burns. */
	public boolean anyBrazierLit() {
		return braziers.stream().anyMatch(this::isBurning);
	}
}
