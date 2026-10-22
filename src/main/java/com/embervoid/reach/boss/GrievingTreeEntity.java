package com.embervoid.reach.boss;

import com.embervoid.boss.BossFight;
import com.embervoid.boss.Telegraph;
import com.embervoid.boss.TelegraphShape;
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
 *   <li><b>P1.</b> Rooted in the middle of the grove. Its trunk takes {@value #TRUNK_DAMAGE}× damage unless it is
 *   stunned. {@value #LANTERNS} lanterns hang in its branches ({@link TreeLanternEntity}): a projectile breaks one,
 *   and so does a melee hit on the branch it lowers after a branch sweep. A broken lantern stuns it for 5 s (trunk
 *   ×{@value #STUNNED_DAMAGE}, no moves) and lifts the fog for 6 s. Moves: root lines bursting from the ground at
 *   the fighters, a branch sweep in front, Withered Thralls dropping from the branches.</li>
 *   <li><b>P2 at 50 %.</b> It uproots and walks slowly; new lanterns hang on its own branches and move with it; the
 *   fog thickens; root cages close around a fighter.</li>
 * </ul>
 *
 * Base damage (Lv 21 on Normal ×2.5): root line 7, branch sweep 8, root cage 4. Base HP 170 (~374 at Lv 21).
 */
public class GrievingTreeEntity extends ReachBoss {
	public static final float TRUNK_DAMAGE = 0.3f;
	public static final float STUNNED_DAMAGE = 1.5f;
	public static final int LANTERNS = 5;
	public static final int P2_LANTERNS = 4;
	public static final int STUN_TICKS = 100;
	public static final int FOG_LIFT_TICKS = 120;
	/** Arena fog: grey-green, thicker once it walks. */
	public static final float FOG_DENSITY = 0.55f;
	public static final float THICK_FOG_DENSITY = 0.75f;
	public static final int FOG_COLOR = 0x3c4640;
	/** How long a branch stays lowered after a sweep. */
	public static final int LOWERED_TICKS = 60;

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
	private static final int THRALL_INTERVAL = 500;
	private static final int REHANG_INTERVAL = 600;

	private final List<TreeLanternEntity> lanterns = new ArrayList<>();
	private int stunTicks;
	private int fogLiftTicks;
	private int thrallTimer = THRALL_INTERVAL / 2;
	private int rehangTimer = REHANG_INTERVAL;
	private int lanternsBroken;

	public GrievingTreeEntity(EntityType<? extends GrievingTreeEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 280;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 170)
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

	public boolean isStunned() {
		return dataTracker.get(STUNNED_FLAG);
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
		moveCooldown = 40;
		playAnimation(UPROOT);
		world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 2.0f, 0.4f);
		world.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_ROOTED_DIRT_BREAK, SoundCategory.HOSTILE, 2.0f, 0.5f);
		world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.getDefaultState()),
				getX(), getY() + 0.3, getZ(), 120, 2.5, 0.3, 2.5, 0.2);
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

	/** A lantern broke: stunned 5 s, the fog lifts 6 s. */
	void onLanternBroken(TreeLanternEntity lantern) {
		lanterns.remove(lantern);
		lanternsBroken++;
		BossFight fight = fight();
		Telegraph.cancel(this);
		stunTicks = STUN_TICKS;
		dataTracker.set(STUNNED_FLAG, true);
		playAnimation(STUNNED);
		getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_WARDEN_HURT, SoundCategory.HOSTILE, 2.0f, 0.5f);
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
				moveCooldown = Math.max(cooldown, 20);
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
			return 40;
		}
		if (!isRooted() && random.nextInt(3) == 0) {
			rootCage(target);
			return 45;
		}
		rootLines();
		return 50;
	}

	// --- Moves ---

	/** Root lines burst from its base towards every fighter (or its target alone outside a fight). */
	public void rootLines() {
		List<? extends LivingEntity> targets = fight() != null ? fight().fighters() : getTarget() != null ? List.of(getTarget()) : List.of();
		boolean first = true;
		for (LivingEntity target : targets) {
			Vec3d direction = directionTo(target.getPos());
			TelegraphShape line = TelegraphShape.line(getPos(), getPos().add(direction.multiply(ROOT_LENGTH)), 1.0);
			telegraph(line, 26, first ? ROOT_LINES : null, first ? SoundEvents.BLOCK_ROOTED_DIRT_BREAK : null, ParticleTypes.MYCELIUM,
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
		telegraph(TelegraphShape.cone(getPos(), direction, SWEEP_RANGE, 50), 20, BRANCH_SWEEP, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
				ParticleTypes.MYCELIUM, player -> {
					player.damage(getDamageSources().mobAttack(this), SWEEP_DAMAGE);
					knockAway(player, 1.1, 0.4);
				});
		Telegraph.windup(this, 20, null, null, ParticleTypes.MYCELIUM, 0, () -> {
			Vec3d reach = getPos().add(direction.multiply(4));
			lanterns().stream().min(Comparator.comparingDouble(l -> l.squaredDistanceTo(reach))).ifPresent(l -> l.lower(LOWERED_TICKS));
		});
	}

	/** P2: roots close in a ring around the target; anyone inside is held fast. */
	public void rootCage(LivingEntity target) {
		Vec3d at = target.getPos();
		telegraph(TelegraphShape.ring(at, 2.5), 24, ROOT_CAGE, SoundEvents.BLOCK_ROOTED_DIRT_PLACE, ParticleTypes.MYCELIUM, player -> {
			player.damage(getDamageSources().mobAttack(this), CAGE_DAMAGE);
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 5), this);
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
		getWorld().playSound(null, getX(), getY() + 6, getZ(), SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.HOSTILE, 1.5f, 0.8f);
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (!getWorld().isClient && !source.isOf(DamageTypes.GENERIC_KILL) && !source.isOf(DamageTypes.OUT_OF_WORLD)) {
			amount *= isStunned() ? STUNNED_DAMAGE : TRUNK_DAMAGE;
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
}
