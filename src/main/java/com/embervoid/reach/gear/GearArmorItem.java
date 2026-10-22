package com.embervoid.reach.gear;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Realm armor: a vanilla armor piece plus optional extra attribute modifiers and tooltip lines
 * {@code embervoid.gear.<key>.piece} (what this piece does) and {@code embervoid.gear.<key>.set} (the full-set bonus).
 */
public class GearArmorItem extends ArmorItem {
	private final String key;
	private final boolean pieceEffect;
	private final boolean setBonus;
	private final Multimap<EntityAttribute, EntityAttributeModifier> extra;

	public GearArmorItem(GearArmorMaterial material, Type type, Settings settings, String key, boolean pieceEffect,
			boolean setBonus, Extra... extra) {
		super(material, type, settings);
		this.key = key;
		this.pieceEffect = pieceEffect;
		this.setBonus = setBonus;
		ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
		for (Extra e : extra) {
			UUID id = UUID.nameUUIDFromBytes(("embervoid:gear/" + type.getName() + "/" + e.name).getBytes(StandardCharsets.UTF_8));
			builder.put(e.attribute, new EntityAttributeModifier(id, "embervoid:" + e.name, e.amount, e.operation));
		}
		this.extra = builder.build();
	}

	/** An extra attribute modifier this piece gives while worn. */
	public record Extra(String name, EntityAttribute attribute, double amount, EntityAttributeModifier.Operation operation) {
	}

	@Override
	public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
		Multimap<EntityAttribute, EntityAttributeModifier> base = super.getAttributeModifiers(slot);
		if (slot != getSlotType() || extra.isEmpty()) {
			return base;
		}
		return ImmutableMultimap.<EntityAttribute, EntityAttributeModifier>builder().putAll(base).putAll(extra).build();
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		if (pieceEffect) {
			tooltip.add(Text.translatable("embervoid.gear." + key + ".piece").formatted(Formatting.GRAY));
		}
		if (setBonus) {
			tooltip.add(Text.translatable("embervoid.gear.set_bonus",
					Text.translatable("embervoid.gear." + key + ".set")).formatted(Formatting.GOLD));
		}
	}
}
