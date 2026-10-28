package com.embervoid.boss;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A lasting danger zone: a {@link TelegraphShape} that hurts the players standing in it every {@code interval} ticks
 * for {@code duration} ticks (fire pool, fire trail, root line). Drawn while it lasts.
 */
public class AreaHazard implements ArenaHazard {
	private static final int DRAW_INTERVAL = 4;

	private final TelegraphShape shape;
	private final HazardClock clock;
	private final int interval;
	private final DamageSource source;
	private final float damage;
	private final ParticleEffect particle;
	@Nullable
	private final Consumer<PlayerEntity> onHit;

	/**
	 * @param source damage source of every hit; give it the boss as attacker so its level scales the damage
	 * @param onHit  extra effect on each player hit (set on fire, a status effect), or null
	 */
	public AreaHazard(TelegraphShape shape, int duration, int interval, DamageSource source, float damage, ParticleEffect particle,
			@Nullable Consumer<PlayerEntity> onHit) {
		this.shape = shape;
		this.clock = new HazardClock(duration);
		this.interval = Math.max(1, interval);
		this.source = source;
		this.damage = damage;
		this.particle = particle;
		this.onHit = onHit;
	}

	public TelegraphShape shape() {
		return shape;
	}

	@Override
	public boolean tick(BossFight fight) {
		ServerWorld world = fight.world();
		if (clock.every(DRAW_INTERVAL)) {
			shape.draw(world, particle, 1);
		}
		if (clock.every(interval)) {
			for (PlayerEntity player : Telegraph.playersIn(world, shape, fight.centerPos())) {
				if (player.damage(source, damage) && onHit != null) {
					onHit.accept(player);
				}
			}
		}
		return clock.advance();
	}
}
