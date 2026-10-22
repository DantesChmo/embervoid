package com.embervoid.reach.mob;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.FlyGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

/**
 * A Realm 1 mob that flies (Ashen Carrion, Crypt Moth): no gravity, flight move control and bird navigation, no
 * fall damage, the {@code move.fly} animation. Wanders through the air and chases with its melee move.
 */
public abstract class FlyingReachMob extends ReachMob {
	protected FlyingReachMob(EntityType<? extends FlyingReachMob> type, World world) {
		super(type, world);
		moveControl = new FlightMoveControl(this, 20, true);
		setNoGravity(true);
	}

	@Override
	protected EntityNavigation createNavigation(World world) {
		BirdNavigation navigation = new BirdNavigation(this, world);
		navigation.setCanPathThroughDoors(false);
		navigation.setCanSwim(true);
		navigation.setCanEnterOpenDoors(true);
		return navigation;
	}

	@Override
	protected void initGoals() {
		goalSelector.add(6, new FlyGoal(this, 1.0));
		goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 12));
		goalSelector.add(8, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this));
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
		initAttackGoals();
	}

	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
		return false;
	}

	@Override
	protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {}

	@Override
	protected PlayState movementAnimation(AnimationState<ReachMob> state) {
		return state.setAndContinue(DefaultAnimations.FLY);
	}
}
