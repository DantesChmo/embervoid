package com.embervoid.sanctum;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The Ashen Maiden, the leveling NPC of the Sanctum core (DESIGN §2). She stands in place, watches whoever is near,
 * cannot be hurt (only {@code /kill} and the void remove her) and never despawns. Talking to her opens the
 * level-up screen.
 */
public class AshenMaidenEntity extends PathAwareEntity implements GeoEntity {
	/** How close a player has to stand to her for a level-up request to be accepted. */
	public static final double REACH = 8.0;

	private static final RawAnimation INTERACT = RawAnimation.begin().thenPlay("misc.interact");

	private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);

	public AshenMaidenEntity(EntityType<? extends AshenMaidenEntity> type, World world) {
		super(type, world);
		setPersistent();
		setInvulnerable(true);
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 20)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 8, 1.0f));
		goalSelector.add(2, new LookAroundGoal(this));
	}

	@Override
	protected ActionResult interactMob(PlayerEntity player, Hand hand) {
		if (hand != Hand.MAIN_HAND) {
			return ActionResult.PASS;
		}
		if (player instanceof ServerPlayerEntity serverPlayer) {
			triggerAnim("interact", "interact");
			playSound(SanctumSounds.MAIDEN_GREET, 1.0f, 1.0f);
			MaidenTalk.talk(serverPlayer, this);
		}
		return ActionResult.success(getWorld().isClient);
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		return !source.isOf(DamageTypes.OUT_OF_WORLD) && !source.isOf(DamageTypes.GENERIC_KILL);
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	@Override
	public boolean cannotDespawn() {
		return true;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void pushAway(Entity entity) {
	}

	@Override
	public boolean canBeLeashedBy(PlayerEntity player) {
		return false;
	}

	@Override
	public boolean isFireImmune() {
		return true;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(DefaultAnimations.genericIdleController(this));
		controllers.add(new AnimationController<>(this, "interact", 0, state -> PlayState.STOP)
				.triggerableAnim("interact", INTERACT));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animations;
	}
}
