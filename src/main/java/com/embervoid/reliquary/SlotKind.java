package com.embervoid.reliquary;

import com.embervoid.Embervoid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/**
 * One kind of Reliquary slot (DESIGN §4.5): which items fit it (an item tag) and how many slots of it a player has
 * (config {@code reliquary.slots.<name>}, default {@code defaultCount}). A later realm adds a kind with one more
 * constant in {@link SlotKinds}; its items go into the kind's tag.
 *
 * @param name         id within the Reliquary, saved with the player (e.g. {@code artifact})
 * @param tagId        id of the item tag of the items that fit
 * @param defaultCount slots of this kind when the config does not say otherwise
 */
public record SlotKind(String name, Identifier tagId, int defaultCount) {
	public static SlotKind of(String name, String tag, int defaultCount) {
		return new SlotKind(name, Embervoid.id(tag), defaultCount);
	}

	/** Items that fit. Made on use (tag keys are interned), so the kinds load without the registries. */
	public TagKey<Item> tag() {
		return TagKey.of(RegistryKeys.ITEM, tagId);
	}

	/** Slots of this kind a player has now (config). */
	public int count() {
		return ReliquaryConfig.get().slots(this);
	}

	public boolean accepts(ItemStack stack) {
		return !stack.isEmpty() && stack.isIn(tag());
	}

	/** Sprite shown in an empty slot, in the block atlas ({@code textures/item/empty_<name>_slot.png}). */
	public Identifier emptyIcon() {
		return Embervoid.id("item/empty_" + name + "_slot");
	}

	/** {@code embervoid.reliquary.slot.<name>}: the kind's name on the screen. */
	public String translationKey() {
		return "embervoid.reliquary.slot." + name;
	}
}
