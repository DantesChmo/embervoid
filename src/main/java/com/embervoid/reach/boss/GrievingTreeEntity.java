package com.embervoid.reach.boss;

import com.embervoid.boss.BossFight;
import com.embervoid.boss.Telegraph;
import com.embervoid.boss.TelegraphShape;
import com.embervoid.fx.CombatFx;
import com.embervoid.fx.SwingArc;
import com.embervoid.reach.mob.ReachMobs;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Grieving Tree, boss of the Royal Woods (DESIGN §4.5, PLAN 12.5): the king's oak, ~10 blocks, hanged lanterns in its
 * branches, Lv 21, in arena fog. What it teaches: shoot the lanterns, keep orientation in fog.
 *
 * <ul>
 *   <li><b>P1.</b> Rooted in the middle of the grove. Its bark turns every blade and arrow unless it is stunned
 *   (PLAN 18.4: a trunk that took a little was most of the fight's openings). {@value #LANTERNS} lanterns hang in its
 *   branches ({@link TreeLanternEntity}): a projectile breaks one, and so does a melee hit on the branch it lowers
 *   after a branch sweep. A broken lantern stuns it for 2.2 s (trunk ×{@value #STUNNED_DAMAGE}, no moves) and lifts
 *   the fog for 6 s. Moves: root lines bursting from the ground at
 *   the fighters, a branch sweep in front, Withered Thralls dropping from the branches.</li>
 *   <li><b>P2 at 50 %.</b> It uproots and walks slowly; new lanterns hang on its own branches and move with it; the
 *   fog thickens; root cages close around a fighter.</li>
 * </ul>
 *
 * Base damage (Lv 21 on Normal ×2.5): root line 7, branch sweep 8, root cage 4. Base HP 165 (~363 at Lv 21).
 */
public class GrievingTreeEntity extends ReachBoss {
	/** Damage the trunk takes outside a stun: none, the bark turns it (PLAN 18.4). */
	public static final float TRUNK_DAMAGE = 0;
	public static final float STUNNED_DAMAGE = 1.5f;
	public static final int LANTERNS = 5;
	public static final int P2_LANTERNS = 4;
	public static final int STUN_TICKS = 44;
	public static final int FOG_LIFT_TICKS = 120;
	/** Arena fog: grey-green, thicker once it walks. */
	public static final float FOG_DENSITY = 0.55f;
	public static final float THICK_FOG_DENSITY = 0.75f;
	public static final int FOG_COLOR = 0x3c4640;
	/** How long a branch stays lowered after a sweep. */
	public static final int LOWERED_TICKS = 60;
	/** Windup of each move in ticks (the balance model reads them, PLAN 18.1). */
	public static final int ROOT_WINDUP = 26;
	public static final int SWEEP_WINDUP = 20;
	public static final int CAGE_WINDUP = 24;
	/** Ticks until the brain picks the next move after one; rooted, never fewer than {@value #ROOTED_MIN_COOLDOWN}. */
	public static final int SWEEP_COOLDOWN = 40;
	public static final int CAGE_COOLDOWN = 45;
	public static final int ROOT_COOLDOWN = 50;
	public static final int ROOTED_MIN_COOLDOWN = 20;
	/** The pause after uprooting (P2 starts). */
	public static final int UPROOT_PAUSE = 40;
	/**
	 * Ticks between two drops of Withered Thralls, and between two lanterns growing back while fewer than two hang
	 * (PLAN 18.4: about one per stun the fight needs; at 600 the short stuns ran the branches bare).
	 */
	public static final int THRALL_INTERVAL = 500;
	public static final int REHANG_INTERVAL = 160;

	public static final String ROOT_LINES = "attack.roots";
	public static final String BRANCH_SWEEP = "attack.branch";
	public static final String ROOT_CAGE = "attack.cage";
	public static final String THRALLS = "misc.thralls";
	public static final String STUNNED = "misc.stunned";
	public static final String UPROOT = "misc.uproot";

	private static final TrackedData<Boolean> STUNNED_FLAG = DataTracker.registerData(GrievingTreeEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final float ROOT_DAMAGE = 7;
	private static final float SWEEP_DAMAGE = 8;
	private static final float CAGE_DAMAGE = 4;
	private static final double ROOT_LENGTH = 18;
	private static final double SWEEP_RANGE = 8;

	private final List<TreeLanternEntity> lanterns = new ArrayList<>();
	private int stunTicks;
	private int fogLiftTicks;
	private int thrallTimer = THRALL_INTERVAL / 2;
	private int rehangTimer = REHANG_INTERVAL;
	private int lanternsBroken;

	public GrievingTreeEntity(EntityType<? extends GrievingTreeEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 170;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 165)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, SWEEP_DAMAGE)
				.add(EntityAttributes.GENERIC_ARMOR, 6)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected void initDataTracker() {
		super.initDataTracker();
		dataTracker.startTracking(STUNNED_FLAG, false);
	}

	/** How long the body stays after death (the topple in {@code misc.death}, 3 s); the trunk is its own stagger ({@link #STUNNED}). */
	public static final int DEATH_TICKS = 60;

	public boolean isStunned() {
		return dataTracker.get(STUNNED_FLAG);
	}

	/** A hit of this share of its health (only possible while stunned) jolts the whole tree (visual only). */
	public static final float STAGGER_SHARE = 0.1f;

	@Override
	protected String hurtAnimation(DamageSource source, float amount) {
		return amount >= getMaxHealth() * STAGGER_SHARE ? STAGGER : HURT;
	}

	@Override
	protected List<String> hurtAnimations() {
		return List.of(HURT, STAGGER);
	}

	@Override
	public int deathTicks() {
		return DEATH_TICKS;
	}

	/** It groans, sap and leaves fall, then the trunk cracks and the whole oak crashes down (the fall lands at 2.4 s). */
	@Override
	protected void tickDeath(ServerWorld world, int deathTime) {
		if (deathTime % 3 == 0) {
			world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.getDefaultState()),
					getX(), getY() + 8, getZ(), 8, 3.0, 1.0, 3.0, 0.05);
			world.spawnParticles(ParticleTypes.FALLING_HONEY, getX(), getY() + 5.5, getZ(), 2, 0.5, 0.3, 0.5, 0); // TODO own particle (amber mote)
		}
		if (deathTime == 30) {
			world.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.HOSTILE, 2.0f, 0.4f);
			world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.STRIPPED_OAK_LOG.getDefaultState()),
					getX(), getY() + 1, getZ(), 40, 1.5, 0.8, 1.5, 0.1);
		}
		if (deathTime == 48) {
			// The crown hits the ground about seven blocks ahead of the base.
			Vec3d crash = getPos().add(Vec3d.fromPolar(0, getYaw()).multiply(7));
			world.playSound(null, crash.x, crash.y, crash.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.5f, 0.4f);
			CombatFx.impact(world, crash, 1.3f, 6f, 48, 0xC97A2A);
			world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.getDefaultState()),
					crash.x, crash.y + 1, crash.z, 150, 4.0, 1.0, 4.0, 0.15);
			world.spawnParticles(ParticleTypes.MYCELIUM, crash.x, crash.y + 0.5, crash.z, 60, 4.0, 0.3, 4.0, 0.05); // TODO own particle (ash flake)
		}
		if (deathTime == DEATH_TICKS) {
			world.spawnParticles(ParticleTypes.SOUL, getX(), getY() + 4, getZ(), 30, 1.5, 3.0, 1.5, 0.04); // TODO own particle (soul wisp)
			world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 2, getZ(), 40, 2.0, 2.0, 2.0, 0.02);
		}
	}

	public boolean isRooted() {
		return phase() == 0;
	}

	@Override
	public float[] phaseThresholds() {
		return new float[]{0.5f};
	}

	@Override
	public EntityType<? extends MobEntity> scalingAdd() {
		return ReachMobs.WITHERED_THRALL;
	}

	@Override
	public void onPhaseChange(int phase) {
		super.onPhaseChange(phase);
		BossFight fight = fight();
		if (fight == null || !(getWorld() instanceof ServerWorld world)) {
			return;
		}
		if (phase == 0) {
			hangLanterns(LANTERNS, 7, 5.5);
			fight.setFog(FOG_DENSITY, FOG_COLOR);
			return;
		}
		// P2: uproots. The old lanterns fall; new ones hang on its own, lower branches and walk with it.
		Telegraph.cancel(this);
		endStun();
		moveCooldown = UPROOT_PAUSE;
		playAnimation(UPROOT);
		world.playSound(null, getX(), getY(), getZ(), ReachBossSounds.GRIEVING_TREE_UPROOT, SoundCategory.HOSTILE, 2.0f, 1.0f);
		world.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_ROOTED_DIRT_BREAK, SoundCategory.HOSTILE, 2.0f, 0.5f);
		world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.getDefaultState()),
				getX(), getY() + 0.3, getZ(), 120, 2.5, 0.3, 2.5, 0.2);
		// The roots tear free: the grove shakes and leaves rain out of the canopy.
		CombatFx.impact(world, getPos(), 1.2f, 4.0f, 48, 0xC97A2A);
		world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.getDefaultState()),
				getX(), getY() + 8, getZ(), 80, 3.5, 1.0, 3.5, 0.1);
		for (TreeLanternEntity lantern : List.copyOf(lanterns)) {
			lantern.discard();
		}
		lanterns.clear();
		hangLanterns(P2_LANTERNS, 5.5, 3.5);
		fight.setFog(THICK_FOG_DENSITY, FOG_COLOR);
		getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.12);
	}

	private void hangLanterns(int count, double height, double distance) {
		for (int i = 0; i < count; i++) {
			hangLantern(Math.PI * 2 * i / count + random.nextDouble() * 0.4, height + random.nextDouble() * 1.5, distance);
		}
	}

	private void hangLantern(double angle, double height, double distance) {
		TreeLanternEntity lantern = ReachBossEntities.TREE_LANTERN.create(getWorld());
		if (lantern == null) {
			return;
		}
		lantern.hangOn(this, angle, distance, height);
		getWorld().spawnEntity(lantern);
		lanterns.add(lantern);
	}

	/** Lanterns still hanging. */
	public List<TreeLanternEntity> lanterns() {
		lanterns.removeIf(TreeLanternEntity::isRemoved);
		return List.copyOf(lanterns);
	}

	public int lanternsBroken() {
		return lanternsBroken;
	}

	/** A lantern broke: stunned 2.2 s, the fog lifts 6 s. */
	void onLanternBroken(TreeLanternEntity lantern) {
		lanterns.remove(lantern);
		lanternsBroken++;
		BossFight fight = fight();
		Telegraph.cancel(this);
		stunTicks = STUN_TICKS;
		dataTracker.set(STUNNED_FLAG, true);
		playAnimation(STUNNED);
		getWorld().playSound(null, getX(), getY(), getZ(), ReachBossSounds.GRIEVING_TREE_STUN, SoundCategory.HOSTILE, 2.0f, 1.0f);
		if (getWorld() instanceof ServerWorld world) {
			// The lurch: a shudder through the arena and a rain of leaves from the shaken canopy.
			CombatFx.shake(world, getPos(), 0.5f, 16, 32);
			world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.getDefaultState()),
					getX(), getY() + 8, getZ(), 40, 3.0, 1.0, 3.0, 0.08);
			world.spawnParticles(ParticleTypes.FALLING_HONEY, getX(), getY() + 5.3, getZ(), 6, 0.3, 0.2, 0.3, 0); // TODO own particle (amber mote)
		}
		if (fight != null) {
			fogLiftTicks = FOG_LIFT_TICKS;
			fight.clearFog();
		}
	}

	private void endStun() {
		stunTicks = 0;
		dataTracker.set(STUNNED_FLAG, false);
	}

	@Override
	protected void tickBoss() {
		BossFight fight = fight();
		if (fight == null) {
			return;
		}
		if (stunTicks > 0 && --stunTicks == 0) {
			endStun();
		}
		if (fogLiftTicks > 0 && --fogLiftTicks == 0) {
			fight.setFog(isRooted() ? FOG_DENSITY : THICK_FOG_DENSITY, FOG_COLOR);
		}
		if (!isStunned() && --thrallTimer <= 0) {
			thrallTimer = THRALL_INTERVAL;
			dropThralls();
		}
		// Lanterns grow back on the branches when few are left, so a stun is always within reach.
		if (lanterns().size() < 2 && --rehangTimer <= 0) {
			rehangTimer = REHANG_INTERVAL;
			hangLantern(random.nextDouble() * Math.PI * 2, isRooted() ? 7 : 5.5, isRooted() ? 5.5 : 3.5);
		}
		if (isRooted()) {
			// Rooted: it never moves from its spot.
			setVelocity(0, getVelocity().y, 0);
		}
	}

	@Override
	protected boolean isBusy() {
		return isStunned() || isRooted();
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		// Rooted or stunned it still picks moves (busy only stops walking).
		if (isRooted() && !isStunned() && !Telegraph.isWindingUp(this) && getTarget() != null && getTarget().isAlive()) {
			if (moveCooldown > 0) {
				moveCooldown--;
			} else {
				int cooldown = chooseMove(getTarget(), squaredDistanceTo(getTarget()));
				moveCooldown = Math.max(cooldown, ROOTED_MIN_COOLDOWN);
			}
		}
	}

	@Override
	protected double keepDistance() {
		return 5;
	}

	@Override
	protected int chooseMove(LivingEntity target, double squaredDistance) {
		if (isStunned()) {
			return -1;
		}
		if (squaredDistance <= SWEEP_RANGE * SWEEP_RANGE && random.nextInt(3) != 0) {
			branchSweep(target);
			return SWEEP_COOLDOWN;
		}
		if (!isRooted() && random.nextInt(3) == 0) {
			rootCage(target);
			return CAGE_COOLDOWN;
		}
		rootLines();
		return ROOT_COOLDOWN;
	}

	// --- Moves ---

	/** The weight of each strike as it lands: the sweep's trail and the ground torn by roots and cages. */
	@Override
	protected void afterStrike(String animation) {
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		int amber = 0xC97A2A;
		switch (animation) {
			case BRANCH_SWEEP -> {
				CombatFx.trail(this, SwingArc.sweep((float) SWEEP_RANGE, 2.5f, true, 7, amber));
				Vec3d front = getPos().add(getRotationVector().multiply(1, 0, 1).normalize().multiply(4));
				CombatFx.impact(world, front, 0.8f, 3.0f, 32, amber);
				world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.getDefaultState()),
						front.x, front.y + 0.5, front.z, 40, 2.5, 0.3, 2.5, 0.1);
			}
			case ROOT_LINES -> {
				CombatFx.shake(world, getPos(), 0.7f, CombatFx.shakeTicks(0.7f), 40);
				world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.getDefaultState()),
						getX(), getY() + 0.3, getZ(), 60, 3.0, 0.3, 3.0, 0.15);
			}
			case ROOT_CAGE -> CombatFx.shake(world, getPos(), 0.4f, CombatFx.shakeTicks(0.4f), 32);
			default -> {
			}
		}
	}

	/** Root lines burst from its base towards every fighter (or its target alone outside a fight). */
	public void rootLines() {
		List<? extends LivingEntity> targets = fight() != null ? fight().fighters() : getTarget() != null ? List.of(getTarget()) : List.of();
		boolean first = true;
		for (LivingEntity target : targets) {
			Vec3d direction = directionTo(target.getPos());
			TelegraphShape line = TelegraphShape.line(getPos(), getPos().add(direction.multiply(ROOT_LENGTH)), 1.0);
			telegraph(line, ROOT_WINDUP, first ? ROOT_LINES : null, first ? ReachBossSounds.GRIEVING_TREE_ROOT_LINES : null, ParticleTypes.MYCELIUM,
					player -> {
						player.damage(getDamageSources().mobAttack(this), ROOT_DAMAGE);
						player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 2), this);
						player.addVelocity(0, 0.5, 0);
						player.velocityModified = true;
					});
			first = false;
		}
	}

	/** A branch sweeps the ground in front; afterwards the lantern nearest the sweep hangs low for a while. */
	public void branchSweep(LivingEntity target) {
		face(target.getPos());
		Vec3d direction = directionTo(target.getPos());
		telegraph(TelegraphShape.cone(getPos(), direction, SWEEP_RANGE, 50), SWEEP_WINDUP, BRANCH_SWEEP, ReachBossSounds.GRIEVING_TREE_BRANCH_SWEEP,
				ParticleTypes.MYCELIUM, player -> {
					player.damage(getDamageSources().mobAttack(this), SWEEP_DAMAGE);
					knockAway(player, 1.1, 0.4);
				});
		Telegraph.windup(this, SWEEP_WINDUP, null, null, ParticleTypes.MYCELIUM, 0, () -> {
			Vec3d reach = getPos().add(direction.multiply(4));
			lanterns().stream().min(Comparator.comparingDouble(l -> l.squaredDistanceTo(reach))).ifPresent(l -> l.lower(LOWERED_TICKS));
		});
	}

	/** P2: roots close in a ring around the target; anyone inside is held fast. */
	public void rootCage(LivingEntity target) {
		Vec3d at = target.getPos();
		telegraph(TelegraphShape.ring(at, 2.5), CAGE_WINDUP, ROOT_CAGE, ReachBossSounds.GRIEVING_TREE_ROOT_CAGE, ParticleTypes.MYCELIUM, player -> {
			player.damage(getDamageSources().mobAttack(this), CAGE_DAMAGE);
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 5), this);
		});
		Telegraph.windup(this, CAGE_WINDUP, null, null, ParticleTypes.MYCELIUM, 0, () -> {
			if (getWorld() instanceof ServerWorld world) {
				// The roots burst up round the mark.
				CombatFx.flash(world, at, 2.5f, 0xC97A2A);
				world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.getDefaultState()),
						at.x, at.y + 0.4, at.z, 50, 2.0, 0.3, 2.0, 0.15);
			}
		});
	}

	/** Two Withered Thralls drop from its branches (fight adds). */
	public void dropThralls() {
		BossFight fight = fight();
		if (fight == null) {
			return;
		}
		playAnimation(THRALLS);
		for (int i = 0; i < 2; i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			fight.spawnAdd(ReachMobs.WITHERED_THRALL, getPos().add(Math.cos(angle) * 4, 6, Math.sin(angle) * 4));
		}
		getWorld().playSound(null, getX(), getY() + 6, getZ(), ReachBossSounds.GRIEVING_TREE_THRALLS, SoundCategory.HOSTILE, 1.5f, 1.0f);
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (!getWorld().isClient && !source.isOf(DamageTypes.GENERIC_KILL) && !source.isOf(DamageTypes.OUT_OF_WORLD)) {
			amount *= isStunned() ? STUNNED_DAMAGE : TRUNK_DAMAGE;
			if (amount <= 0) {
				// The bark turns it: a dull knock tells the player to go for the lanterns.
				if (source.getAttacker() != null) {
					getWorld().playSound(null, getX(), getY() + 2, getZ(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 1.0f, 0.6f);
				}
				return false;
			}
		}
		return super.damage(source, amount);
	}

	@Override
	public void remove(RemovalReason reason) {
		for (TreeLanternEntity lantern : lanterns) {
			lantern.discard();
		}
		lanterns.clear();
		super.remove(reason);
	}

	@Override
	public List<String> toggledBones() {
		return List.of("roots");
	}

	@Override
	public boolean showsBone(String bone) {
		return isRooted();
	}

	@Override
	protected List<String> actionAnimations() {
		return List.of(ROOT_LINES, BRANCH_SWEEP, ROOT_CAGE, THRALLS, STUNNED, UPROOT);
	}

	/** Test hook: the stun left, in ticks. */
	public int stunTicks() {
		return stunTicks;
	}

	@Override
	protected void pushAway(Entity entity) {
		if (!isRooted()) {
			super.pushAway(entity);
		}
	}

	@Override
	public boolean isPushedByFluids() {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ReachBossSounds.GRIEVING_TREE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ReachBossSounds.GRIEVING_TREE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ReachBossSounds.GRIEVING_TREE_DEATH;
	}

	@Override
	protected SoundEvent stepSound() {
		return ReachBossSounds.GRIEVING_TREE_STEP;
	}
}
