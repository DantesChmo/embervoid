package com.embervoid.boss;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * Placeholder boss that proves the framework: zombie model, two phases (enrages at half health), and a telegraphed
 * ground slam. Repeat kills drop {@code embervoid:entities/test_boss}. Brings a zombie per extra fighter. GameTests
 * give it scripted phases ({@link #setScriptedPhases}) to test them.
 */
public class TestBossEntity extends ZombieEntity implements ArenaBoss {
	private static final int SLAM_WINDUP = 25;
	private static final double SLAM_RADIUS = 4.5;
	private static final float SLAM_DAMAGE = 10.0f;

	private int slamCooldown = 80;
	private boolean enraged;
	private int scriptedPhases;
	private int lastPhase;

	public TestBossEntity(EntityType<? extends ZombieEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 200;
		this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
		this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			this.setEquipmentDropChance(slot, 0);
		}
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return createZombieAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 200.0)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0)
				.add(EntityAttributes.GENERIC_ARMOR, 6.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.26)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8);
	}

	@Override
	protected void initCustomGoals() {
		this.goalSelector.add(2, new ZombieAttackGoal(this, 1.0, false));
		this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		ArenaManager.tickBoss(this);
		if (isRemoved()) {
			return;
		}
		if (slamCooldown > 0) {
			slamCooldown--;
		} else if (getTarget() != null && squaredDistanceTo(getTarget()) < 36 && !Telegraph.isWindingUp(this)) {
			slamCooldown = enraged ? 60 : 100;
			Telegraph.windup(this, SLAM_WINDUP, "slam", SoundEvents.ENTITY_RAVAGER_ROAR, ParticleTypes.FLAME, SLAM_RADIUS, this::slam);
		}
	}

	private void slam() {
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.0f, 0.7f);
		world.spawnParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.5, getZ(), 6, SLAM_RADIUS / 2, 0.2, SLAM_RADIUS / 2, 0);
		for (PlayerEntity player : world.getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(SLAM_RADIUS, 2, SLAM_RADIUS),
				p -> p.squaredDistanceTo(this) <= SLAM_RADIUS * SLAM_RADIUS && !p.isSpectator())) {
			player.damage(getDamageSources().mobAttack(this), SLAM_DAMAGE);
			player.addVelocity(0, 0.8, 0);
			player.velocityModified = true;
		}
	}

	@Override
	public float[] phaseThresholds() {
		return new float[]{0.5f};
	}

	@Override
	public int scriptedPhases() {
		return scriptedPhases;
	}

	/** Test hook: the first {@code count} phases end only by {@code BossFight.advancePhase}. */
	public void setScriptedPhases(int count) {
		scriptedPhases = count;
	}

	/** The last phase started (tests). */
	public int lastPhase() {
		return lastPhase;
	}

	@Override
	public EntityType<? extends MobEntity> scalingAdd() {
		return EntityType.ZOMBIE;
	}

	@Override
	public void onPhaseChange(int phase) {
		lastPhase = phase;
		if (phase == scriptedPhases + 1) {
			enraged = true;
			getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.32);
			getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 2.0f, 0.6f);
			setOnFireFor(3);
		}
	}

	@Override
	public Identifier music() {
		return new Identifier("music.dragon");
	}

	@Override
	protected boolean burnsInDaylight() {
		return false;
	}

	@Override
	protected boolean canConvertInWater() {
		return false;
	}

	@Override
	public boolean cannotDespawn() {
		return true;
	}
}
