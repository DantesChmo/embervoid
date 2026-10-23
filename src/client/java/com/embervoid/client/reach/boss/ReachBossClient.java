package com.embervoid.client.reach.boss;

import com.embervoid.Embervoid;
import com.embervoid.reach.boss.ReachBoss;
import com.embervoid.reach.boss.ReachBossEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * GeckoLib renderers for the Ashen Reach bosses: models from {@code tools/reach_boss_art.py}, glowmask layer, bones
 * the boss shows only at times ({@link ReachBoss#toggledBones}) hidden or shown every frame, and no vanilla death tilt
 * (every boss has its own death animation, PLAN 19.4).
 */
public final class ReachBossClient {
	public static void register() {
		renderer(ReachBossEntities.CRYPT_KEEPER);
		renderer(ReachBossEntities.ASH_COLOSSUS);
		renderer(ReachBossEntities.GRIEVING_TREE);
		renderer(ReachBossEntities.SMOLDERING_LORD);
		EntityRendererRegistry.register(ReachBossEntities.TREE_LANTERN, TreeLanternRenderer::new);
	}

	private static <T extends ReachBoss> void renderer(EntityType<T> type) {
		String name = Registries.ENTITY_TYPE.getId(type).getPath();
		EntityRendererRegistry.register(type, context -> {
			GeoEntityRenderer<T> renderer = new GeoEntityRenderer<>(context, new Model<>(name)) {
				@Override
				protected float getDeathMaxRotation(T boss) {
					return 0;
				}
			};
			renderer.addRenderLayer(new AutoGlowingGeoLayer<>(renderer));
			return renderer;
		});
	}

	private static class Model<T extends ReachBoss> extends DefaultedEntityGeoModel<T> {
		Model(String name) {
			super(Embervoid.id(name), true);
		}

		@Override
		public void setCustomAnimations(T boss, long instanceId, AnimationState<T> state) {
			super.setCustomAnimations(boss, instanceId, state);
			for (String name : boss.toggledBones()) {
				CoreGeoBone bone = getAnimationProcessor().getBone(name);
				if (bone != null) {
					bone.setHidden(!boss.showsBone(name));
				}
			}
		}
	}

	private ReachBossClient() {}
}
