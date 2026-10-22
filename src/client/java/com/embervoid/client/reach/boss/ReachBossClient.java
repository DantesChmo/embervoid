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
 * GeckoLib renderers for the Ashen Reach bosses: greybox models at final size from {@code tools/boss_placeholders.py},
 * glowmask layer, bones the boss shows only at times ({@link ReachBoss#toggledBones}) hidden or shown every frame.
 */
public final class ReachBossClient {
	public static void register() {
		renderer(ReachBossEntities.CRYPT_KEEPER);
	}

	private static <T extends ReachBoss> void renderer(EntityType<T> type) {
		String name = Registries.ENTITY_TYPE.getId(type).getPath();
		EntityRendererRegistry.register(type, context -> {
			GeoEntityRenderer<T> renderer = new GeoEntityRenderer<>(context, new Model<>(name));
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
