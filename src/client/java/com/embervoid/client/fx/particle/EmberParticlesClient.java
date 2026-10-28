package com.embervoid.client.fx.particle;

import com.embervoid.fx.particle.EmberParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

public final class EmberParticlesClient {
	public static void register() {
		ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();
		registry.register(EmberParticles.EMBER, sprites -> new DriftParticle.Factory(DriftParticle.Kind.EMBER, sprites));
		registry.register(EmberParticles.ASH_FLAKE, sprites -> new DriftParticle.Factory(DriftParticle.Kind.ASH_FLAKE, sprites));
		registry.register(EmberParticles.SOOT, sprites -> new DriftParticle.Factory(DriftParticle.Kind.SOOT, sprites));
		registry.register(EmberParticles.CINDER_SPARK, sprites -> new DriftParticle.Factory(DriftParticle.Kind.CINDER_SPARK, sprites));
		registry.register(EmberParticles.BLOOD_DROP, sprites -> new DriftParticle.Factory(DriftParticle.Kind.BLOOD_DROP, sprites));
		registry.register(EmberParticles.AMBER_MOTE, sprites -> new DriftParticle.Factory(DriftParticle.Kind.AMBER_MOTE, sprites));
		registry.register(EmberParticles.COLD_FLAME, sprites -> new DriftParticle.Factory(DriftParticle.Kind.COLD_FLAME, sprites));
		registry.register(EmberParticles.WHIP_LINE, sprites -> new DriftParticle.Factory(DriftParticle.Kind.WHIP_LINE, sprites));
		registry.register(EmberParticles.SOUL_WISP, SoulWispParticle.Factory::new);
		registry.register(EmberParticles.SHOCKWAVE, sprites -> new DecalParticle.Factory(true, sprites));
		registry.register(EmberParticles.SCORCH, sprites -> new DecalParticle.Factory(false, sprites));
		registry.register(EmberParticles.BURST, BurstParticle.Factory::new);
	}

	private EmberParticlesClient() {}
}
