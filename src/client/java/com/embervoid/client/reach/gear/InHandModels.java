package com.embervoid.client.reach.gear;

import com.embervoid.Embervoid;
import com.embervoid.reach.gear.GearWeapons;
import com.embervoid.sanctum.gear.SanctumWeapons;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The weapons' big 3D models in a hand (docs/art-direction.md): {@code item/<weapon>_in_hand}, element
 * models by {@code tools/weapon_model_art.py}, swapped in by {@code GearItemRendererMixin} for the first- and
 * third-person hands the way vanilla swaps the trident's; the inventory, the ground and item frames keep the icon.
 */
public final class InHandModels {
	private static final Map<Item, Identifier> MODELS = new IdentityHashMap<>();

	static {
		register(GearWeapons.ASHEN_SCYTHE);
		register(GearWeapons.ROYAL_ASH_RAPIER);
		register(GearWeapons.WARDENS_SCOURGE);
		register(SanctumWeapons.SANCTUM_LONGSWORD);
		register(SanctumWeapons.ASHBEARERS_MACE);
	}

	private static void register(Item item) {
		MODELS.put(item, Embervoid.id("item/" + net.minecraft.registry.Registries.ITEM.getId(item).getPath() + "_in_hand"));
	}

	/** Loads the in-hand models with the item models. */
	public static void load() {
		ModelLoadingPlugin.register(plugin -> plugin.addModels(MODELS.values().toArray(Identifier[]::new)));
	}

	/** Whether {@code mode} is a hand (first or third person), where the in-hand model is drawn. */
	public static boolean isHand(ModelTransformationMode mode) {
		return mode != ModelTransformationMode.GUI && mode != ModelTransformationMode.GROUND && mode != ModelTransformationMode.FIXED
				&& mode != ModelTransformationMode.NONE && mode != ModelTransformationMode.HEAD;
	}

	/** The in-hand model of {@code stack} in {@code mode}, or null to keep the model vanilla picked. */
	@Nullable
	public static BakedModel modelFor(ItemStack stack, ModelTransformationMode mode) {
		Identifier id = MODELS.get(stack.getItem());
		if (id == null || !isHand(mode)) {
			return null;
		}
		return MinecraftClient.getInstance().getBakedModelManager().getModel(id);
	}

	private InHandModels() {}
}
