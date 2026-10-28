package com.embervoid.entity.mob;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

/**
 * The hit reaction and death animations of the ordinary mobs (PLAN 19.3), named and wired as {@code ReachBoss} does
 * it for the bosses: a {@link #HURT_CONTROLLER} between the walk / idle controller and the move controllers (later
 * controllers win on the bones they animate, so a move's animation beats a flinch), and a {@link #DEATH_CONTROLLER}
 * last, holding {@link #DEATH} on its final frame while the body lies there. Each mob keeps its own flinch cooldown
 * and its {@code deathTicks()} (the animation's length), overriding {@code updatePostDeath} to call
 * {@link #tickDeath}; vanilla would remove the body after 20 ticks.
 */
public final class MobAnimations {
	/** The controller of the hit reaction (the flinch, the stagger). */
	public static final String HURT_CONTROLLER = "hurt";
	/** The controller of the fall on death, held on its last frame. */
	public static final String DEATH_CONTROLLER = "death";
	/** A flinch on a hit outside a windup; never cancels one. */
	public static final String HURT = "misc.hurt";
	/** A heavier reel where the mob can be staggered (the Hollow's broken guard, a heavy hit on the Zombie). */
	public static final String STAGGER = "misc.stagger";
	/** The fall; the body stays for its length. */
	public static final String DEATH = "misc.death";
	/** Ticks between two flinches. */
	public static final int HURT_COOLDOWN = 10;

	/** The hit-reaction controller with the given triggerable animations ({@link #HURT}, {@link #STAGGER}). */
	public static <T extends GeoAnimatable> AnimationController<T> hurtController(T mob, String... animations) {
		AnimationController<T> hurt = new AnimationController<>(mob, HURT_CONTROLLER, 0, state -> PlayState.STOP);
		for (String name : animations) {
			hurt.triggerableAnim(name, RawAnimation.begin().thenPlay(name));
		}
		return hurt;
	}

	/** The death controller: {@link #DEATH} once, held on its last frame. */
	public static <T extends GeoAnimatable> AnimationController<T> deathController(T mob) {
		return new AnimationController<>(mob, DEATH_CONTROLLER, 0, state -> PlayState.STOP)
				.triggerableAnim(DEATH, RawAnimation.begin().thenPlayAndHold(DEATH));
	}

	/** Whether a hit may show a flinch now: a real hit on a living mob, server side, outside the cooldown. */
	public static boolean mayFlinch(LivingEntity mob, boolean hurt, float amount, int lastFlinch) {
		return mob.isAlive() && !mob.getWorld().isClient && mayFlinch(hurt, amount, mob.age, lastFlinch);
	}

	/**
	 * Whether a hit at mob age {@code age} may show a flinch: it hurt, for some damage, and more than
	 * {@link #HURT_COOLDOWN} ticks after the flinch at age {@code lastFlinch}.
	 */
	public static boolean mayFlinch(boolean hurt, float amount, int age, int lastFlinch) {
		return hurt && amount > 0 && age - lastFlinch > HURT_COOLDOWN;
	}

	/**
	 * One tick of a body lying there after death ({@code updatePostDeath}): once {@code deathTime} reaches
	 * {@code deathTicks} the body goes with vanilla's puff (entity status 60). Returns true when it went.
	 */
	public static boolean tickDeath(LivingEntity mob, int deathTime, int deathTicks) {
		if (deathTime < deathTicks || mob.isRemoved() || !(mob.getWorld() instanceof ServerWorld world)) {
			return false;
		}
		world.sendEntityStatus(mob, (byte) 60);
		mob.remove(net.minecraft.entity.Entity.RemovalReason.KILLED);
		return true;
	}

	private MobAnimations() {}
}
