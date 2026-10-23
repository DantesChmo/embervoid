package com.embervoid.sanctum.boss;

import com.embervoid.boss.AreaHazard;
import com.embervoid.boss.BossFight;
import com.embervoid.boss.BossHits;
import com.embervoid.boss.Telegraph;
import com.embervoid.boss.TelegraphShape;
import com.embervoid.fx.CombatFx;
import com.embervoid.fx.SwingArc;
import com.embervoid.reach.boss.ReachBoss;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Fallen Sister, the Ruined Sanctum's boss (DESIGN §2, PLAN 16.3): the Ashen Maiden's burnt-out twin, 3 blocks tall,
 * a censer on a long chain, Lv 10. The gatekeeper of the mod: fought by a level 1 player in iron with an iron sword.
 * Built to DESIGN §5.5 against that player (light moves ≤ 20 % of their health, heavy ≤ 35 %, the spin her biggest;
 * every move behind a marked shape) and to about 20 % uptime: her combos chain close and her recoveries are short.
 *
 * <ul>
 *   <li><b>P1.</b> Censer arcs: the chain winds around her arm, then two wide cone swings, right to left and back.
 *   Slam: the censer comes down on a ring in front of her and leaves a puddle of embers there for 4 s. Backstep: a
 *   player close behind her makes her turn and hop away.</li>
 *   <li><b>P2 at 50 %.</b> The veil burns away (2 s, she takes no damage while it burns); 3 crying souls circle her,
 *   then 4 every 20 s once they are gone, and dive at her target one by one, each with a shriek and a trail of soul
 *   fire marking its line first ({@link CryingSoulEntity}); one hit kills a soul. The censer spin, a ring around her,
 *   joins her moves.</li>
 * </ul>
 *
 * Base damage per move (level 10 and Normal scale it): arc 4, slam 6, spin 8, soul dive 3, puddle 1 per 0.5 s. Base HP
 * {@value #BASE_HEALTH}. The arena's braziers light one by one as she loses health; they burn on once she is beaten.
 */
public class FallenSisterEntity extends ReachBoss {
	public static final double BASE_HEALTH = 115;
	/** Windup of each move in ticks (the balance model reads them, PLAN 18.1); the second arc follows the first. */
	public static final int ARC_1_WINDUP = 18;
	public static final int ARC_2_WINDUP = 12;
	public static final int SLAM_WINDUP = 24;
	public static final int SPIN_WINDUP = 26;
	public static final int BACKSTEP_WINDUP = 8;
	/** Ticks until the brain picks the next move after one. */
	public static final int ARC_COOLDOWN = 18;
	public static final int SLAM_COOLDOWN = 22;
	public static final int SPIN_COOLDOWN = 20;
	public static final int BACKSTEP_COOLDOWN = 16;
	/** The veil burning at 50 %: she stands, untouchable, for this long. */
	public static final int VEIL_BURN_TICKS = 40;
	/** How long her body stays for the death animation. */
	public static final int DEATH_TICKS = 50;
	/** The slam's ember puddle: how long it burns, and how often it hurts whoever stands in it. */
	public static final int PUDDLE_TICKS = 80;
	public static final int PUDDLE_INTERVAL = 10;
	/** Crying souls: the first flock, the later ones, the time from one flock to the next, the time between dives. */
	public static final int FIRST_FLOCK = 3;
	public static final int LATER_FLOCK = 4;
	public static final int FLOCK_INTERVAL = 400;
	public static final int DIVE_INTERVAL = 50;

	public static final String ARC_1 = "attack.censer_arc_1";
	public static final String ARC_2 = "attack.censer_arc_2";
	public static final String SLAM = "attack.slam";
	public static final String SPIN = "attack.spin";
	public static final String BACKSTEP = "misc.backstep";
	public static final String VEIL_BURN = "misc.veil_burn";
	public static final String HURT = "misc.hurt";
	public static final String DEATH = "misc.death";
	/** The controller of the hit reaction, under the move animations. */
	public static final String HURT_CONTROLLER = "hurt";

	private static final float ARC_DAMAGE = 4;
	private static final float SLAM_DAMAGE = 6;
	private static final float SPIN_DAMAGE = 8;
	private static final float PUDDLE_DAMAGE = 1;
	/** A crying soul's dive; dealt by her ({@link #diveHit}), so her level scales it. */
	static final float DIVE_DAMAGE = 3;
	private static final double ARC_RANGE = 4.5;
	private static final double ARC_HALF_ANGLE = 70;
	private static final double SLAM_REACH = 2.5;
	private static final double SLAM_RADIUS = 2.5;
	private static final double SPIN_RADIUS = 4.5;
	/** She picks a move with her target this close, and backsteps from a target this close behind her. */
	private static final double MELEE_RANGE = 4.5;
	private static final double BEHIND_RANGE = 4;
	/** The arena's braziers are campfires within this far beyond the fog wall. */
	private static final int BRAZIER_MARGIN = 4;

	private static final TrackedData<Boolean> VEIL_BURNT = DataTracker.registerData(FallenSisterEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	/** Braziers of the arena, in the order they light; their state comes back when the fight ends without a kill. */
	private final List<BlockPos> braziers = new ArrayList<>();
	/** Where her fight is (the arena floor centre), kept for the clearing after the fight has ended. */
	@Nullable
	private BlockPos arenaCenter;
	private int veilBurnLeft;
	private int flockTimer;
	private int diveTimer;
	private int flocks;
	private int lastHurtAnimation = -100;
	@Nullable
	private Vec3d slamAt;

	public FallenSisterEntity(EntityType<? extends FallenSisterEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 60;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, BASE_HEALTH)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, ARC_DAMAGE)
				.add(EntityAttributes.GENERIC_ARMOR, 4)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8);
	}

	@Override
	protected void initDataTracker() {
		super.initDataTracker();
		dataTracker.startTracking(VEIL_BURNT, false);
	}

	/** Whether the veil has burnt away (phase 2, after the burn); the model hides it then. */
	public boolean veilBurnt() {
		return dataTracker.get(VEIL_BURNT);
	}

	@Override
	public List<String> toggledBones() {
		return List.of("veil");
	}

	@Override
	public boolean showsBone(String bone) {
		return !bone.equals("veil") || !veilBurnt();
	}

	@Override
	public float[] phaseThresholds() {
		return new float[]{0.5f};
	}

	@Override
	public void onPhaseChange(int phase) {
		super.onPhaseChange(phase);
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		BossFight fight = fight();
		if (phase == 0) {
			if (fight != null) {
				arenaCenter = fight.center();
				findBraziers(fight);
			}
			return;
		}
		Telegraph.cancel(this);
		moveCooldown = VEIL_BURN_TICKS;
		veilBurnLeft = VEIL_BURN_TICKS;
		playAnimation(VEIL_BURN);
		world.playSound(null, getX(), getY(), getZ(), SanctumBossSounds.FALLEN_SISTER_VEIL_BURN, SoundCategory.HOSTILE, 2.0f, 1.0f);
		world.spawnParticles(ParticleTypes.FLAME, getX(), getY() + 2.6, getZ(), 30, 0.3, 0.3, 0.3, 0.02);
	}

	@Override
	protected void tickBoss() {
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		BossFight fight = fight();
		if (!veilBurnt() && age % 8 == 0) {
			// Embers drip from under the veil.
			world.spawnParticles(ParticleTypes.FALLING_LAVA, getX(), getY() + 2.3, getZ(), 1, 0.15, 0, 0.15, 0);
		}
		if (age % 5 == 0) {
			Vec3d censer = censerPos();
			world.spawnParticles(ParticleTypes.SMOKE, censer.x, censer.y, censer.z, 1, 0.05, 0.05, 0.05, 0.01);
		}
		if (veilBurnLeft > 0) {
			world.spawnParticles(ParticleTypes.FLAME, getX(), getY() + 2.6, getZ(), 3, 0.25, 0.25, 0.25, 0.01);
			if (--veilBurnLeft == 0) {
				dataTracker.set(VEIL_BURNT, true);
				world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 2.8, getZ(), 20, 0.3, 0.3, 0.3, 0.02);
				if (fight != null) {
					summonFlock(fight, FIRST_FLOCK);
				}
			}
		}
		if (fight == null) {
			return;
		}
		if (veilBurnt()) {
			tickSouls(fight);
		}
		if (age % 10 == 0) {
			lightBraziers(fight);
		}
	}

	@Override
	protected boolean isBusy() {
		return veilBurnLeft > 0;
	}

	@Override
	protected double keepDistance() {
		return 2.5;
	}

	// --- Moves ---

	@Override
	protected int chooseMove(LivingEntity target, double squaredDistance) {
		if (squaredDistance > MELEE_RANGE * MELEE_RANGE) {
			return -1;
		}
		if (squaredDistance <= BEHIND_RANGE * BEHIND_RANGE && isBehind(target)) {
			backstep(target);
			return BACKSTEP_COOLDOWN;
		}
		boolean late = veilBurnt();
		int crowd = Telegraph.playersIn(this, TelegraphShape.ring(getPos(), SPIN_RADIUS)).size();
		if (late && (crowd >= 2 || random.nextInt(3) == 0)) {
			spin();
			return SPIN_COOLDOWN;
		}
		if (random.nextInt(late ? 3 : 8) < (late ? 1 : 3)) {
			slam(target);
			return SLAM_COOLDOWN;
		}
		arc(target, 1);
		return ARC_COOLDOWN;
	}

	/** Whether {@code target} stands behind her (more than 110° from where her body faces). */
	private boolean isBehind(LivingEntity target) {
		Vec3d facing = Vec3d.fromPolar(0, bodyYaw).multiply(1, 0, 1).normalize();
		return facing.dotProduct(directionTo(target.getPos())) < -0.34;
	}

	/** One of the two censer arcs: a wide cone in front of her; the first has the longer windup (the chain winding). */
	public void arc(LivingEntity target, int hit) {
		face(target.getPos());
		TelegraphShape cone = TelegraphShape.cone(getPos(), directionTo(target.getPos()), ARC_RANGE, ARC_HALF_ANGLE);
		telegraph(cone, hit == 1 ? ARC_1_WINDUP : ARC_2_WINDUP, hit == 1 ? ARC_1 : ARC_2, SanctumBossSounds.FALLEN_SISTER_CENSER_WIND,
				ParticleTypes.FLAME, player -> player.damage(getDamageSources().mobAttack(this), ARC_DAMAGE));
	}

	/** The censer brought down on a ring in front of her, where a puddle of embers then burns. */
	public void slam(LivingEntity target) {
		face(target.getPos());
		Vec3d at = getPos().add(directionTo(target.getPos()).multiply(SLAM_REACH));
		slamAt = at;
		telegraph(TelegraphShape.ring(at, SLAM_RADIUS), SLAM_WINDUP, SLAM, SanctumBossSounds.FALLEN_SISTER_SLAM, ParticleTypes.FLAME,
				player -> {
					player.damage(getDamageSources().mobAttack(this), SLAM_DAMAGE);
					player.addVelocity(0, 0.3, 0);
					player.velocityModified = true;
				});
	}

	/** Phase 2: the censer swung around her on its full chain, a ring. */
	public void spin() {
		telegraph(TelegraphShape.ring(getPos(), SPIN_RADIUS), SPIN_WINDUP, SPIN, SanctumBossSounds.FALLEN_SISTER_SPIN, ParticleTypes.FLAME,
				player -> {
					player.damage(getDamageSources().mobAttack(this), SPIN_DAMAGE);
					knockAway(player, 0.9, 0.3);
				});
	}

	/** A player close behind her: she turns to them and hops away. No damage. */
	public void backstep(LivingEntity target) {
		face(target.getPos());
		Telegraph.windup(this, BACKSTEP_WINDUP, BACKSTEP, SanctumBossSounds.FALLEN_SISTER_BACKSTEP, ParticleTypes.SMOKE, 0, () -> {
			Vec3d away = directionTo(target.getPos()).multiply(-1);
			setVelocity(away.x * 0.9, 0.35, away.z * 0.9);
			velocityModified = true;
			if (getWorld() instanceof ServerWorld world) {
				world.spawnParticles(ParticleTypes.WHITE_ASH, getX(), getY() + 0.2, getZ(), 20, 0.5, 0.1, 0.5, 0.02);
			}
		});
	}

	@Override
	protected void afterStrike(String animation) {
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		LivingEntity target = getTarget();
		switch (animation) {
			case ARC_1 -> {
				CombatFx.trail(this, SwingArc.sweep((float) ARC_RANGE, 1.8f, false, 5, CombatFx.EMBER));
				if (target != null && target.isAlive()) {
					arc(target, 2);
				}
			}
			case ARC_2 -> CombatFx.trail(this, SwingArc.sweep((float) ARC_RANGE, 1.8f, true, 5, CombatFx.EMBER));
			case SLAM -> {
				Vec3d at = slamAt != null ? slamAt : getPos();
				CombatFx.trail(this, SwingArc.overhead((float) SLAM_REACH + 1, 2.4f, 5, CombatFx.EMBER));
				CombatFx.impact(world, at, 0.8f, (float) SLAM_RADIUS, 24, CombatFx.EMBER);
				world.playSound(null, at.x, at.y, at.z, SanctumBossSounds.FALLEN_SISTER_SLAM_IMPACT, SoundCategory.HOSTILE, 1.5f, 1.0f);
				world.spawnParticles(ParticleTypes.LAVA, at.x, at.y + 0.2, at.z, 12, 1.0, 0.1, 1.0, 0);
				BossFight fight = fight();
				if (fight != null) {
					fight.addHazard(new AreaHazard(TelegraphShape.ring(at, SLAM_RADIUS), PUDDLE_TICKS, PUDDLE_INTERVAL,
							BossHits.source(this, DamageTypes.IN_FIRE), PUDDLE_DAMAGE, ParticleTypes.SMALL_FLAME, null));
				}
			}
			case SPIN -> {
				CombatFx.trail(this, new SwingArc(-180, 180, 0, (float) SPIN_RADIUS, 1.2f, 10, CombatFx.EMBER));
				CombatFx.impact(world, getPos(), 0.6f, 1.5f, 16, CombatFx.EMBER);
			}
			default -> {
			}
		}
	}

	/** Roughly where the censer hangs, in front of her to the right. */
	private Vec3d censerPos() {
		Vec3d forward = Vec3d.fromPolar(0, bodyYaw).multiply(1, 0, 1).normalize();
		Vec3d right = new Vec3d(-forward.z, 0, forward.x);
		return getPos().add(forward.multiply(0.55)).add(right.multiply(0.3)).add(0, 0.45, 0);
	}

	// --- Crying souls ---

	private void summonFlock(BossFight fight, int count) {
		flocks++;
		flockTimer = FLOCK_INTERVAL;
		diveTimer = DIVE_INTERVAL;
		for (int i = 0; i < count; i++) {
			double angle = Math.PI * 2 * i / count;
			Vec3d at = getPos().add(Math.cos(angle) * CryingSoulEntity.ORBIT_RADIUS, CryingSoulEntity.ORBIT_HEIGHT,
					Math.sin(angle) * CryingSoulEntity.ORBIT_RADIUS);
			CryingSoulEntity soul = fight.spawnAdd(SanctumBossEntities.CRYING_SOUL, at);
			if (soul != null) {
				soul.bind(this, i, count);
				soul.refreshPositionAndAngles(at.x, at.y, at.z, 0, 0);
			}
		}
		if (getWorld() instanceof ServerWorld world) {
			world.playSound(null, getX(), getY(), getZ(), SanctumBossSounds.CRYING_SOUL_SHRIEK, SoundCategory.HOSTILE, 1.5f, 0.8f);
			world.spawnParticles(ParticleTypes.SOUL, getX(), getY() + 2.5, getZ(), 20, 0.6, 0.4, 0.6, 0.03);
		}
	}

	/** Her souls in this fight that are still there. */
	public List<CryingSoulEntity> souls() {
		BossFight fight = fight();
		if (fight == null) {
			return List.of();
		}
		List<CryingSoulEntity> souls = new ArrayList<>();
		for (MobEntity add : fight.adds()) {
			if (add instanceof CryingSoulEntity soul) {
				souls.add(soul);
			}
		}
		return souls;
	}

	/** One soul dives at a time, one every {@value #DIVE_INTERVAL} ticks; a new flock once the last is gone and its time has come. */
	private void tickSouls(BossFight fight) {
		List<CryingSoulEntity> souls = souls();
		flockTimer--;
		if (souls.isEmpty()) {
			if (flockTimer <= 0) {
				summonFlock(fight, LATER_FLOCK);
			}
			return;
		}
		if (souls.stream().anyMatch(soul -> !soul.isCircling()) || --diveTimer > 0) {
			return;
		}
		LivingEntity target = getTarget() instanceof PlayerEntity player && player.isAlive() ? player
				: fight.fighters().stream().min(Comparator.comparingDouble(this::squaredDistanceTo)).orElse(null);
		if (target == null) {
			return;
		}
		CryingSoulEntity diver = souls.stream().filter(CryingSoulEntity::isCircling)
				.min(Comparator.comparingDouble(soul -> soul.squaredDistanceTo(target))).orElse(null);
		if (diver != null && diver.startDive(target)) {
			diveTimer = DIVE_INTERVAL;
		}
	}

	/** A soul's dive struck {@code player}: the hit is hers, at her level. */
	void diveHit(PlayerEntity player) {
		player.damage(getDamageSources().mobAttack(this), DIVE_DAMAGE);
	}

	/** Flocks of souls summoned so far in this fight (tests). */
	public int flocks() {
		return flocks;
	}

	// --- Braziers ---

	/** Every campfire in the arena room, nearest the door first; remembered so a reset puts them out again. */
	private void findBraziers(BossFight fight) {
		braziers.clear();
		BlockPos center = fight.center();
		int r = fight.spec().radius() + BRAZIER_MARGIN;
		for (BlockPos pos : BlockPos.iterate(center.add(-r, 0, -r), center.add(r, 4, r))) {
			if (fight.world().getBlockState(pos).isOf(Blocks.CAMPFIRE)) {
				braziers.add(pos.toImmutable());
				fight.blocks().remember(pos);
			}
		}
		braziers.sort(Comparator.comparingDouble((BlockPos pos) -> -pos.getSquaredDistance(center)).thenComparingInt(BlockPos::getX)
				.thenComparingInt(BlockPos::getZ));
	}

	public List<BlockPos> braziers() {
		return List.copyOf(braziers);
	}

	/** How many braziers burn at a health fraction: one more each seventh of her health lost, all of them near the end. */
	public static int litBraziers(float fraction, int braziers) {
		return Math.max(0, Math.min(braziers, (int) Math.floor((1 - fraction) * (braziers + 1) + 1e-4)));
	}

	private void lightBraziers(BossFight fight) {
		int wanted = litBraziers(getHealth() / getMaxHealth(), braziers.size());
		for (int i = 0; i < wanted; i++) {
			BlockPos pos = braziers.get(i);
			BlockState state = fight.world().getBlockState(pos);
			if (state.getBlock() instanceof CampfireBlock && !state.get(CampfireBlock.LIT)) {
				fight.blocks().set(pos, state.with(CampfireBlock.LIT, true));
				kindle(fight.world(), pos);
			}
		}
	}

	private static void kindle(ServerWorld world, BlockPos pos) {
		world.playSound(null, pos, SanctumBossSounds.FALLEN_SISTER_BRAZIER, SoundCategory.BLOCKS, 1.0f, 1.0f);
		world.spawnParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 10, 0.2, 0.2, 0.2, 0.02);
	}

	// --- Damage, death ---

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (veilBurnLeft > 0 && !source.isOf(DamageTypes.GENERIC_KILL) && !source.isOf(DamageTypes.OUT_OF_WORLD)) {
			return false;
		}
		boolean hurt = super.damage(source, amount);
		// A flinch between moves; never over a windup.
		if (hurt && isAlive() && !Telegraph.isWindingUp(this) && age - lastHurtAnimation > 10) {
			lastHurtAnimation = age;
			triggerAnim(HURT_CONTROLLER, HURT);
		}
		return hurt;
	}

	/**
	 * Beaten: the fight has ended and credited the fighters (its {@code AFTER_DEATH} hook runs inside the super call);
	 * the dungeon is marked cleared and its braziers light for good.
	 */
	@Override
	public void onDeath(DamageSource source) {
		super.onDeath(source);
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		Telegraph.cancel(this);
		playAnimation(DEATH);
		if (arenaCenter != null) {
			SanctumBossArena.markCleared(world, arenaCenter);
		}
		for (BlockPos pos : braziers) {
			BlockState state = world.getBlockState(pos);
			if (state.getBlock() instanceof CampfireBlock && !state.get(CampfireBlock.LIT)) {
				world.setBlockState(pos, state.with(CampfireBlock.LIT, true));
				kindle(world, pos);
			}
		}
	}

	@Override
	protected int deathTicks() {
		return DEATH_TICKS;
	}

	@Override
	protected void updatePostDeath() {
		deathTime++;
		if (!(getWorld() instanceof ServerWorld world) || isRemoved()) {
			return;
		}
		if (deathTime % 4 == 0) {
			world.spawnParticles(ParticleTypes.SMALL_FLAME, getX(), getY() + 1.2, getZ(), 3, 0.3, 0.6, 0.3, 0.01);
		}
		if (deathTime >= DEATH_TICKS) {
			// The last embers go out: what is left of her falls to ash.
			world.spawnParticles(ParticleTypes.WHITE_ASH, getX(), getY() + 0.8, getZ(), 60, 0.5, 0.6, 0.5, 0.02);
			world.spawnParticles(ParticleTypes.SOUL, getX(), getY() + 0.8, getZ(), 12, 0.4, 0.4, 0.4, 0.04);
			world.sendEntityStatus(this, (byte) 60);
			remove(RemovalReason.KILLED);
		}
	}

	@Override
	protected List<String> actionAnimations() {
		return List.of(ARC_1, ARC_2, SLAM, SPIN, BACKSTEP, VEIL_BURN, DEATH);
	}

	/** Walk / idle first, then the hit reaction, then the moves, so a move's animation wins over a flinch. */
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(DefaultAnimations.genericWalkIdleController(this));
		controllers.add(new AnimationController<>(this, HURT_CONTROLLER, 0, state -> PlayState.STOP)
				.triggerableAnim(HURT, RawAnimation.begin().thenPlay(HURT)));
		AnimationController<FallenSisterEntity> action = new AnimationController<>(this, ACTION, 0, state -> PlayState.STOP);
		for (String name : actionAnimations()) {
			action.triggerableAnim(name, RawAnimation.begin().thenPlay(name));
		}
		controllers.add(action);
	}

	/** Not a giant: her sounds carry like a player's. */
	@Override
	protected float getSoundVolume() {
		return 1.2f;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SanctumBossSounds.FALLEN_SISTER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SanctumBossSounds.FALLEN_SISTER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SanctumBossSounds.FALLEN_SISTER_DEATH;
	}

	@Override
	protected SoundEvent stepSound() {
		return SanctumBossSounds.FALLEN_SISTER_STEP;
	}
}
