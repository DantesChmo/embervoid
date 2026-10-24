package com.embervoid.reach.mob;

import com.embervoid.fx.particle.EmberParticles;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.List;

/**
 * Necropolis fodder (DESIGN §4.5): a skeleton with a rusty sword (10-tick windup). A blow that would kill it
 * instead collapses it into a bone pile with {@link #PILE_HEALTH} of {@link #BASE_HEALTH} of its max health; the
 * pile re-forms after {@link #REFORM_TICKS} at half health. Finishing the pile kills it for good, and so does light:
 * a pile at block light ≥ {@link #LIGHT_TO_STAY_DEAD} crumbles to dust (credited to whoever knocked it down). Damage
 * that bypasses invulnerability ({@code /kill}, the void) kills it outright. Grave Wardens re-form piles with their
 * bell ({@link #reform}).
 */
public class RestlessBonesEntity extends ReachMob {
	public static final float BASE_HEALTH = 5;
	public static final float PILE_HEALTH = 2;
	public static final int REFORM_TICKS = 100;
	public static final int LIGHT_TO_STAY_DEAD = 8;
	public static final int SWING_WINDUP = 10;
	/** Ticks the body stays for {@code misc.death}: the heap sinks into dust, 1.6 s. */
	public static final int DEATH_TICKS = 32;
	static final Move MELEE = new Move("attack.swing", SWING_WINDUP, 30, ReachMobSounds.RESTLESS_BONES_WINDUP, 1.0f);
	private static final byte STATE_PILE = 1;
	private static final RawAnimation PILE = RawAnimation.begin().thenLoop("misc.pile");
	private static final EntityDimensions PILE_SIZE = EntityDimensions.changing(0.8f, 0.5f);

	private int reformTicks;
	/** The source of the damage being applied right now (see {@link #setHealth}). */
	@Nullable
	private DamageSource applying;

	public RestlessBonesEntity(EntityType<? extends RestlessBonesEntity> type, World world) {
		super(type, world);
		experiencePoints = 8;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, BASE_HEALTH)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32);
	}

	public boolean isPile() {
		return getState() == STATE_PILE;
	}

	/** Ticks until the pile stands up (0 when standing). */
	public int reformTicks() {
		return isPile() ? reformTicks : 0;
	}

	@Override
	protected void applyDamage(DamageSource source, float amount) {
		applying = source;
		try {
			super.applyDamage(source, amount);
		} finally {
			applying = null;
		}
	}

	/** A lethal blow on a standing skeleton collapses it instead (damage being applied only, never other callers). */
	@Override
	public void setHealth(float health) {
		if (health <= 0 && applying != null && !isPile() && isAlive() && !getWorld().isClient
				&& !applying.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			collapse();
			return;
		}
		super.setHealth(health);
	}

	private void collapse() {
		cancelMove();
		getNavigation().stop();
		setState(STATE_PILE);
		super.setHealth(getMaxHealth() * PILE_HEALTH / BASE_HEALTH);
		reformTicks = REFORM_TICKS;
		triggerAnim(ACTION, "misc.collapse");
		playSound(ReachMobSounds.RESTLESS_BONES_COLLAPSE, 1.0f, 1.0f);
		rattle(30);
	}

	/** Stands the pile up again at half health (after {@link #REFORM_TICKS}, or at a Grave Warden's bell). */
	public void reform() {
		if (!isPile() || !isAlive()) {
			return;
		}
		setState(STATE_NONE);
		super.setHealth(getMaxHealth() / 2);
		reformTicks = 0;
		triggerAnim(ACTION, "misc.reform");
		playSound(ReachMobSounds.RESTLESS_BONES_REFORM, 1.0f, 1.0f);
		rattle(20);
	}

	private void rattle(int count) {
		if (getWorld() instanceof ServerWorld world) {
			world.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.BONE)),
					getX(), getY() + 0.3, getZ(), count, 0.3, 0.2, 0.3, 0.05);
			// Bone dust: ash flakes thrown out of the heap.
			EmberParticles.ringOut(world, EmberParticles.ASH_FLAKE, getPos(), count / 2, 0.1, 0.1);
		}
	}

	/** Standing it flinches; as a pile a hit only jolts the heap (the stagger keys the pile pose, so it never stands up). */
	@Override
	protected String hurtAnimation(DamageSource source, float amount) {
		return isPile() ? STAGGER : HURT;
	}

	@Override
	public int deathTicks() {
		return DEATH_TICKS;
	}

	/** The heap crumbles: bone dust drifts off it while it sinks. */
	@Override
	protected void tickDeath(ServerWorld world, int deathTime) {
		if (deathTime == 2) {
			EmberParticles.ringOut(world, EmberParticles.ASH_FLAKE, getPos(), 16, 0.12, 0.15);
		} else if (deathTime % 4 == 0) {
			EmberParticles.spawn(world, EmberParticles.ASH_FLAKE, getPos().add(0, 0.25, 0), 2, 0.4, 0.02);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient || !isPile() || !isAlive()) {
			return;
		}
		if (getWorld().getLightLevel(LightType.BLOCK, getBlockPos()) >= LIGHT_TO_STAY_DEAD) {
			crumble();
		} else if (--reformTicks <= 0) {
			reform();
		}
	}

	/** The lit pile turns to dust: a real death, credited to whoever knocked it down. */
	private void crumble() {
		LivingEntity attacker = getAttacker();
		DamageSource source = attacker instanceof PlayerEntity player ? getDamageSources().playerAttack(player)
				: attacker != null ? getDamageSources().mobAttack(attacker) : getDamageSources().magic();
		timeUntilRegen = 0;
		playSound(ReachMobSounds.RESTLESS_BONES_CRUMBLE, 1.0f, 1.0f);
		if (getWorld() instanceof ServerWorld world) {
			world.spawnParticles(ParticleTypes.WHITE_ASH, getX(), getY() + 0.2, getZ(), 30, 0.4, 0.2, 0.4, 0.02);
		}
		damage(source, Float.MAX_VALUE);
	}

	@Override
	protected void mobTick() {
		if (isPile()) {
			getNavigation().stop();
			return;
		}
		super.mobTick();
	}

	@Override
	protected boolean canStartMove(Move next) {
		return super.canStartMove(next) && !isPile();
	}

	@Override
	protected void onStateChanged() {
		calculateDimensions();
	}

	@Override
	public EntityDimensions getDimensions(EntityPose pose) {
		return isPile() ? PILE_SIZE : super.getDimensions(pose);
	}

	@Override
	public boolean isPushable() {
		return !isPile() && super.isPushable();
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		if (isPile()) {
			nbt.putInt("ReformTicks", reformTicks);
		}
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.contains("ReformTicks")) {
			setState(STATE_PILE);
			reformTicks = nbt.getInt("ReformTicks");
		}
	}

	@Nullable
	@Override
	protected RawAnimation stateAnimation(byte state) {
		return state == STATE_PILE ? PILE : null;
	}

	@Override
	public Move meleeMove() {
		return MELEE;
	}

	@Override
	protected List<String> actionAnimations() {
		return List.of("attack.swing", "misc.collapse", "misc.reform");
	}

	@Override
	public EntityGroup getGroup() {
		return EntityGroup.UNDEAD;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return isPile() ? null : ReachMobSounds.RESTLESS_BONES_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ReachMobSounds.RESTLESS_BONES_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ReachMobSounds.RESTLESS_BONES_DEATH;
	}

	@Override
	protected SoundEvent stepSound() {
		return ReachMobSounds.RESTLESS_BONES_STEP;
	}
}
