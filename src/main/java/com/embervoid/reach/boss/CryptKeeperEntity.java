package com.embervoid.reach.boss;

import com.embervoid.boss.AreaHazard;
import com.embervoid.boss.BossFight;
import com.embervoid.boss.BossHits;
import com.embervoid.boss.FloodHazard;
import com.embervoid.boss.Telegraph;
import com.embervoid.boss.TelegraphShape;
import com.embervoid.reach.gear.GearConsumables;
import com.embervoid.reach.mob.ReachMobSounds;
import com.embervoid.reach.mob.ReachMobs;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import com.embervoid.fx.CombatFx;
import com.embervoid.fx.particle.EmberParticles;
import com.embervoid.fx.SwingArc;
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
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
 * Crypt Keeper, boss of the Sunken Necropolis (DESIGN §4.5): 4 blocks tall, halberd and lantern, Lv 24.
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
 * a fire pool of 2 per second. Base HP 102 (~243 at Lv 24). Every block it changes (water, braziers) comes back when
 * the fight ends.
 */
public class CryptKeeperEntity extends ReachBoss {
	/** Block light where it stands at or above which it takes full damage in P2 and P3. */
	public static final int LIT_LEVEL = KeeperRules.LIT_LEVEL;
	/** A player carrying a lit lantern this close lights it up. */
	public static final double LANTERN_REACH = 4;
	/** Damage taken in the dark (P2, P3). */
	public static final float DARK_DAMAGE = KeeperRules.DARK_DAMAGE;
	/** Windup of each move in ticks (the balance model reads them); the combo's hits follow each other. */
	public static final int SWING_1_WINDUP = 14;
	public static final int SWING_2_WINDUP = 10;
	public static final int OVERHEAD_WINDUP = 22;
	public static final int SWEEP_WINDUP = 24;
	public static final int LANTERN_WINDUP = 20;
	public static final int THRUST_WINDUP = 18;
	/**
	 * Ticks until the brain picks the next move after one, close and at range, in P1 and from P2 on (faster). Close it
	 * presses on (longer cooldowns leave too many openings for the diamond sword and the Scythe to share a band).
	 */
	public static final int CLOSE_COOLDOWN = KeeperRules.CLOSE_COOLDOWN;
	public static final int CLOSE_COOLDOWN_LATE = KeeperRules.CLOSE_COOLDOWN_LATE;
	public static final int RANGED_COOLDOWN = KeeperRules.RANGED_COOLDOWN;
	public static final int RANGED_COOLDOWN_LATE = KeeperRules.RANGED_COOLDOWN_LATE;
	public static final int PHASE_PAUSE = 30;
	/** Ticks between two waves of moths while a brazier burns. */
	public static final int MOTH_INTERVAL = 300;
	/** Restless Bones rising in P3, plus one per fighter beyond the first. */
	public static final int RISEN_BONES = 3;

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
	private static final double SWING_RANGE = KeeperRules.SWING_RANGE;
	private static final double SWEEP_RADIUS = 5;
	private static final double FIRE_POOL_RADIUS = 2.5;

	/** Water: the flood reaches this far from the centre (the sinkhole floor, up to the pit wall). */
	private static final int FLOOD_MARGIN = 7;
	private static final int MAX_MOTHS = 4;
	/** How close a moth gets to a lit brazier to smother it. */
	private static final double SMOTHER_DISTANCE = 1.6;

	private static final DustParticleEffect UNBLOCKABLE = new DustParticleEffect(new Vector3f(0.85f, 0.1f, 0.1f), 1.5f);

	/** Braziers of the arena (found when the fight starts); their lit state is undone with the fight. */
	private final List<BlockPos> braziers = new ArrayList<>();
	private boolean braziersFound;
	private int mothTimer = MOTH_INTERVAL / 2;
	private int risen;

	public CryptKeeperEntity(EntityType<? extends CryptKeeperEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 190;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 102)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, SWING_DAMAGE)
				.add(EntityAttributes.GENERIC_ARMOR, 4)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.22)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
	}

	/** How long the body lies there after death (the fall in {@code misc.death}, 3 s). */
	public static final int DEATH_TICKS = 60;

	/** Whether it still carries its lantern (P1); the model hides the lantern after it shatters. */
	public boolean hasLantern() {
		return KeeperRules.hasLantern(phase());
	}

	/** In the dark phases a hit that lands on it in the light staggers it (visual only); any other hit is a flinch. */
	@Override
	protected String hurtAnimation(DamageSource source, float amount) {
		return KeeperRules.late(phase()) && isLit() ? STAGGER : HURT;
	}

	@Override
	protected List<String> hurtAnimations() {
		return List.of(HURT, STAGGER);
	}

	@Override
	public int deathTicks() {
		return DEATH_TICKS;
	}

	/** Souls leak out of the robe as it kneels; it hits the floor at 2 s; what is left goes out in cold flame. */
	@Override
	protected void tickDeath(ServerWorld world, int deathTime) {
		if (deathTime % 4 == 0) {
			EmberParticles.souls(world, new Vec3d(getX(), getY() + 1.5, getZ()), new Vec3d(getX(), getY() + 1.5 + 3.0, getZ()), 2);
			world.spawnParticles(EmberParticles.COLD_FLAME, getX(), getY() + 1.0, getZ(), 2, 0.5, 0.5, 0.5, 0.01);
		}
		if (deathTime == 40) {
			world.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_CHAIN_FALL, SoundCategory.HOSTILE, 1.5f, 0.6f);
			CombatFx.impact(world, getPos(), 0.7f, 2.5f, 28, 0x7FD0F5);
			world.spawnParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 20, 1.2, 0.2, 1.2, 0.02);
		}
		if (deathTime == DEATH_TICKS) {
			world.spawnParticles(EmberParticles.COLD_FLAME, getX(), getY() + 0.6, getZ(), 60, 1.2, 0.5, 1.2, 0.06);
			EmberParticles.souls(world, new Vec3d(getX(), getY() + 0.8, getZ()), new Vec3d(getX(), getY() + 0.8 + 3.0, getZ()), 30);
			world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.8, getZ(), 20, 1.0, 0.5, 1.0, 0.02);
		}
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
		moveCooldown = PHASE_PAUSE;
		int flood = fight.spec().radius() + FLOOD_MARGIN;
		if (phase == 1) {
			playAnimation(LANTERN_SHATTER);
			world.playSound(null, getX(), getY(), getZ(), ReachBossSounds.CRYPT_KEEPER_LANTERN_SHATTER, SoundCategory.HOSTILE, 2.0f, 1.0f);
			world.spawnParticles(EmberParticles.COLD_FLAME, getX(), getY() + 2.5, getZ(), 40, 0.6, 0.6, 0.6, 0.05);
			world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 2.5, getZ(), 30, 0.6, 0.6, 0.6, 0.02);
			EmberParticles.souls(world, new Vec3d(getX(), getY() + 2.5, getZ()), new Vec3d(getX(), getY() + 2.5 + 3.0, getZ()), 40);
			// The flash of the lantern bursting (misc.lantern_shatter holds it up at 0.55 s) and the dark rushing in.
			CombatFx.impact(world, getPos().add(0, 2.5, 0), 0.8f, 3.5f, 40, 0x7FD0F5);
			fight.addHazard(new FloodHazard(flood, 1, 60));
			darken(fight);
			getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.24);
		} else if (phase == 2) {
			playAnimation(RAISE_DEAD);
			world.playSound(null, getX(), getY(), getZ(), ReachBossSounds.CRYPT_KEEPER_RAISE_DEAD, SoundCategory.HOSTILE, 2.0f, 1.0f);
			CombatFx.shake(world, getPos(), 0.6f, 40, 48);
			EmberParticles.souls(world, new Vec3d(getX(), getY() + 1, getZ()), new Vec3d(getX(), getY() + 1 + 3.0, getZ()), 60);
			fight.addHazard(new FloodHazard(flood, 2, 60));
			raiseDead(fight);
			getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.26);
		}
	}

	@Override
	protected void tickBoss() {
		BossFight fight = fight();
		if (fight == null || !KeeperRules.late(phase())) {
			return;
		}
		if (age % 40 == 0) {
			darken(fight);
		}
		tickMoths(fight);
	}

	@Override
	protected int chooseMove(LivingEntity target, double squaredDistance) {
		KeeperRules.Move move = KeeperRules.pick(squaredDistance, phase(),
				() -> Telegraph.playersIn(this, TelegraphShape.ring(getPos(), SWEEP_RADIUS)).size(),
				() -> random.nextInt(3) == 0);
		if (move == null) {
			return -1;
		}
		switch (move) {
			case SWEEP -> sweep();
			case COMBO -> comboSwing(target, 1);
			case THRUST -> thrust(target);
			case LANTERN_THROW -> lanternThrow(target);
		}
		return KeeperRules.cooldown(move, phase());
	}

	/** One of the three combo hits: 1 and 2 are cone swings, 3 the unblockable overhead. */
	public void comboSwing(LivingEntity target, int hit) {
		face(target.getPos());
		Vec3d facing = directionTo(target.getPos());
		if (hit < 3) {
			TelegraphShape cone = TelegraphShape.cone(getPos(), facing, SWING_RANGE, 55);
			int windup = hit == 1 ? SWING_1_WINDUP : SWING_2_WINDUP;
			telegraph(cone, windup, hit == 1 ? SWING_1 : SWING_2, ReachBossSounds.CRYPT_KEEPER_SWING, ParticleTypes.SOUL_FIRE_FLAME,
					player -> player.damage(getDamageSources().mobAttack(this), SWING_DAMAGE));
		} else {
			TelegraphShape line = TelegraphShape.line(getPos(), getPos().add(facing.multiply(6.5)), 1.1);
			telegraph(line, OVERHEAD_WINDUP, OVERHEAD, ReachBossSounds.CRYPT_KEEPER_OVERHEAD, UNBLOCKABLE, player -> {
				BossHits.shieldBreaking(this, player, OVERHEAD_DAMAGE);
				player.addVelocity(0, 0.3, 0);
				player.velocityModified = true;
			});
		}
	}

	/** The halberd's trail on every swing, the floor cracking under the overhead and the sweep; then the combo goes on. */
	@Override
	protected void afterStrike(String animation) {
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		int cold = 0x7FD0F5;
		switch (animation) {
			case SWING_1 -> CombatFx.trail(this, SwingArc.sweep((float) SWING_RANGE, 2.4f, true, 5, cold));
			case SWING_2 -> CombatFx.trail(this, SwingArc.sweep((float) SWING_RANGE, 2.4f, false, 5, cold));
			case OVERHEAD -> {
				Vec3d end = getPos().add(getRotationVector().multiply(1, 0, 1).normalize().multiply(4));
				world.spawnParticles(ParticleTypes.EXPLOSION, end.x, end.y + 0.2, end.z, 2, 0.5, 0, 0.5, 0);
				world.playSound(null, end.x, end.y, end.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.0f, 0.8f);
				CombatFx.trail(this, SwingArc.overhead(6.5f, 3.0f, 6, cold));
				CombatFx.impact(world, end, 0.9f, 2.5f, 32, cold);
				world.spawnParticles(EmberParticles.COLD_FLAME, end.x, end.y + 0.3, end.z, 20, 1.5, 0.2, 1.5, 0.03);
			}
			case SWEEP -> {
				CombatFx.trail(this, new SwingArc(-180, 180, 0, (float) SWEEP_RADIUS, 1.4f, 10, cold));
				CombatFx.impact(world, getPos(), 0.6f, 2.0f, 24, cold);
				world.spawnParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 30, SWEEP_RADIUS / 2, 0.1, SWEEP_RADIUS / 2, 0.02);
			}
			case THRUST -> {
				Vec3d point = getPos().add(getRotationVector().multiply(1, 0, 1).normalize().multiply(5)).add(0, 1.2, 0);
				CombatFx.flash(world, point, 1.5f, cold);
				CombatFx.shake(world, getPos(), 0.5f, CombatFx.shakeTicks(0.5f), 24);
			}
			case LANTERN_THROW -> CombatFx.trail(this, SwingArc.overhead(4.0f, 3.2f, 6, cold));
			default -> {
			}
		}
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive()) {
			return;
		}
		if (animation.equals(SWING_1)) {
			comboSwing(target, 2);
		} else if (animation.equals(SWING_2)) {
			comboSwing(target, 3);
		}
	}

	public void sweep() {
		Vec3d at = getPos();
		telegraph(TelegraphShape.ring(at, SWEEP_RADIUS), SWEEP_WINDUP, SWEEP, ReachBossSounds.CRYPT_KEEPER_SWEEP, ParticleTypes.SOUL_FIRE_FLAME,
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
		telegraph(shadow, LANTERN_WINDUP, LANTERN_THROW, ReachBossSounds.CRYPT_KEEPER_LANTERN_THROW, ParticleTypes.FLAME, player -> {
			player.damage(getDamageSources().mobAttack(this), LANTERN_DAMAGE);
			player.setOnFireFor(2);
		});
		// The pool itself starts when the throw lands (after the hit above).
		Telegraph.windup(this, LANTERN_WINDUP, null, null, ParticleTypes.FLAME, 0, () -> {
			BossFight fight = fight();
			if (fight != null && getWorld() instanceof ServerWorld world) {
				world.playSound(null, landing.x, landing.y, landing.z, ReachMobSounds.FIRE_FLASK_IGNITE, SoundCategory.HOSTILE, 1.5f, 1.0f);
				CombatFx.impact(world, landing, 0.5f, (float) FIRE_POOL_RADIUS, 20, CombatFx.EMBER);
				world.spawnParticles(EmberParticles.CINDER_SPARK, landing.x, landing.y + 0.3, landing.z, 10, 1.0, 0.2, 1.0, 0);
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
		telegraph(TelegraphShape.line(getPos(), end, 1.0), THRUST_WINDUP, THRUST, ReachBossSounds.CRYPT_KEEPER_THRUST, ParticleTypes.SOUL_FIRE_FLAME,
				player -> {
					player.damage(getDamageSources().mobAttack(this), THRUST_DAMAGE);
					knockAway(player, 0.6, 0.2);
				});
		// The lunge carries it forward with the thrust.
		Telegraph.windup(this, THRUST_WINDUP, null, null, ParticleTypes.SOUL_FIRE_FLAME, 0, () -> {
			setVelocity(facing.x * 1.4, 0.1, facing.z * 1.4);
			velocityModified = true;
		});
	}

	/**
	 * Whether it stands in light: block light ≥ {@value #LIT_LEVEL} where it stands (lit braziers, a torch, lava), or
	 * a player within {@value #LANTERN_REACH} blocks carrying an Ember Lantern lit with Amber Lantern Oil (DESIGN §4.5:
	 * the oil is "for the Necropolis darkness and the Crypt Keeper").
	 */
	public boolean isLit() {
		World world = getWorld();
		BlockPos feet = getBlockPos();
		if (BossRules.lit(world.getLightLevel(LightType.BLOCK, feet), world.getLightLevel(LightType.BLOCK, feet.up(2)), LIT_LEVEL)) {
			return true;
		}
		return !world.getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(LANTERN_REACH),
				player -> !player.isSpectator() && GearConsumables.carriesLitLantern(player)).isEmpty();
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (!getWorld().isClient && KeeperRules.late(phase()) && !source.isOf(DamageTypes.GENERIC_KILL)
				&& !source.isOf(DamageTypes.OUT_OF_WORLD)) {
			amount *= KeeperRules.damageFactor(phase(), isLit());
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
		fight.fireArenaEvent("raise_dead"); // the ossuary's bell tolls
		int count = RISEN_BONES + Math.max(0, fight.fighters().size() - 1);
		for (int i = 0; i < count; i++) {
			double angle = Math.PI * 2 * i / count + random.nextDouble() * 0.5;
			double distance = 7 + random.nextDouble() * (fight.spec().radius() - 12);
			@Nullable MobEntity bones = fight.spawnAdd(ReachMobs.RESTLESS_BONES,
					fight.centerPos().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance));
			if (bones != null) {
				risen++;
			}
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

	/** Restless Bones raised so far (tests: they may fall again before anyone looks). */
	public int risenBones() {
		return risen;
	}

	public boolean anyBrazierLit() {
		return braziers.stream().anyMatch(this::isBurning);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ReachBossSounds.CRYPT_KEEPER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ReachBossSounds.CRYPT_KEEPER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ReachBossSounds.CRYPT_KEEPER_DEATH;
	}

	@Override
	protected SoundEvent stepSound() {
		return ReachBossSounds.CRYPT_KEEPER_STEP;
	}
}
