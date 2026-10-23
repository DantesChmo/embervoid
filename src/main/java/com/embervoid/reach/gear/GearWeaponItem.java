package com.embervoid.reach.gear;

import com.embervoid.item.ability.SpecialAbilities;
import com.embervoid.item.ability.SpecialAbility;
import com.embervoid.stats.WeaponScaling;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A unique realm weapon (DESIGN §4.5 "Weapons"): fixed damage and speed, its own STR/DEX scaling, an optional reach
 * other than vanilla's 3 blocks ({@link GearReach}), Bleeding and fire on hit, and a right-click special attack
 * (DESIGN §8.1). Not a sword: no vanilla sweep; repaired with a Smoldering Ingot.
 */
public class GearWeaponItem extends Item {
	/**
	 * @param damage        attack damage (the tooltip number)
	 * @param speed         attacks per second
	 * @param scaling       STR/DEX share of the stat damage bonus
	 * @param kindlingShare share of the hit that scales with KIN instead (the weapon's ember part)
	 * @param reach         attack reach in blocks; 0 = vanilla
	 * @param bleed         Bleeding levels added per hit
	 * @param igniteSeconds seconds the target burns after a hit
	 */
	public record Spec(float damage, float speed, WeaponScaling.Scaling scaling, float kindlingShare, double reach,
			int bleed, int igniteSeconds) {
	}

	private final Spec spec;
	@Nullable
	private final SpecialAbility ability;
	private final Multimap<EntityAttribute, EntityAttributeModifier> modifiers;

	public GearWeaponItem(Settings settings, Spec spec, @Nullable SpecialAbility ability) {
		super(settings);
		this.spec = spec;
		this.ability = ability;
		this.modifiers = ImmutableMultimap.of(
				EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(ATTACK_DAMAGE_MODIFIER_ID, "Weapon modifier",
						spec.damage() - 1, EntityAttributeModifier.Operation.ADDITION),
				EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(ATTACK_SPEED_MODIFIER_ID, "Weapon modifier",
						spec.speed() - 4, EntityAttributeModifier.Operation.ADDITION));
	}

	public Spec spec() {
		return spec;
	}

	@Nullable
	public SpecialAbility ability() {
		return ability;
	}

	@Override
	public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
		return slot == EquipmentSlot.MAINHAND ? modifiers : super.getAttributeModifiers(slot);
	}

	@Override
	public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (!attacker.getWorld().isClient()) {
			if (spec.bleed() > 0) {
				Bleeding.apply(target, spec.bleed(), attacker);
			}
			if (spec.igniteSeconds() > 0) {
				target.setOnFireFor(spec.igniteSeconds());
			}
			onHit(stack, target, attacker);
		}
		wear(stack, attacker);
		return true;
	}

	/** Extra effects of a landed melee hit, server side. */
	protected void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
	}

	/** The durability a landed melee hit costs: one. */
	protected void wear(ItemStack stack, LivingEntity attacker) {
		stack.damage(1, attacker, e -> e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
	}

	@Override
	public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
		if (state.getHardness(world, pos) != 0) {
			stack.damage(2, miner, e -> e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
		}
		return true;
	}

	@Override
	public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
		return !miner.getAbilities().creativeMode;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if (ability == null) {
			return super.use(world, user, hand);
		}
		return SpecialAbilities.use(ability, world, user, hand);
	}

	@Override
	public int getEnchantability() {
		return 15;
	}

	@Override
	public boolean canRepair(ItemStack stack, ItemStack ingredient) {
		return ingredient.isOf(GearItems.SMOLDERING_INGOT);
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		tooltip.add(Text.translatable(getTranslationKey() + ".passive").formatted(Formatting.GRAY));
		if (ability != null) {
			SpecialAbilities.appendTooltip(ability, tooltip);
		}
	}
}
