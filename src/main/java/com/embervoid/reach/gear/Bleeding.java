package com.embervoid.reach.gear;

import com.embervoid.Embervoid;
import com.embervoid.fx.particle.EmberParticles;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import org.jetbrains.annotations.Nullable;

/**
 * Bleeding (DESIGN §4.5, a placeholder until the status-effects design): levels I–III, each hit +1 level and a fresh
 * duration, damage every second ignoring armor. Entities in {@code #embervoid:bleed_immune} never bleed.
 */
public final class Bleeding extends StatusEffect {
	public static final TagKey<EntityType<?>> IMMUNE = TagKey.of(RegistryKeys.ENTITY_TYPE, Embervoid.id("bleed_immune"));
	/** Damage type {@code embervoid:bleeding}: bypasses armor and the hurt cooldown, so combat hits do not eat it. */
	public static final RegistryKey<DamageType> DAMAGE_TYPE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Embervoid.id("bleeding"));

	public static final StatusEffect BLEEDING = Registry.register(Registries.STATUS_EFFECT, Embervoid.id("bleeding"), new Bleeding());

	private Bleeding() {
		super(StatusEffectCategory.HARMFUL, 0x8A0B0B);
	}

	public static boolean isImmune(LivingEntity entity) {
		return entity.getType().isIn(IMMUNE);
	}

	/** Current Bleeding level, 0 when not bleeding. */
	public static int level(LivingEntity entity) {
		StatusEffectInstance effect = entity.getStatusEffect(BLEEDING);
		return effect == null ? 0 : effect.getAmplifier() + 1;
	}

	/**
	 * Adds {@code stacks} levels of Bleeding (capped) and refreshes its duration. A player who caused it keeps the kill
	 * credit as with a direct hit. Returns false if the target is immune.
	 */
	public static boolean apply(LivingEntity target, int stacks, @Nullable LivingEntity source) {
		if (stacks <= 0 || isImmune(target) || target.getWorld().isClient()) {
			return false;
		}
		GearConfig.Bleeding config = GearConfig.get().bleeding();
		int level = BleedRules.nextLevel(level(target), stacks, config.maxLevel());
		// Same or higher amplifier: addStatusEffect upgrades or refreshes the running instance.
		target.addStatusEffect(new StatusEffectInstance(BLEEDING, config.durationTicks(), level - 1), source);
		if (source instanceof PlayerEntity player) {
			target.setAttacking(player);
		}
		return true;
	}

	public static DamageSource damageSource(LivingEntity entity) {
		return new DamageSource(entity.getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(DAMAGE_TYPE));
	}

	@Override
	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		return true;
	}

	@Override
	public void applyUpdateEffect(LivingEntity entity, int amplifier) {
		if (entity.getWorld().isClient() || !BleedRules.damagesAt(entity.age) || isImmune(entity)) {
			return;
		}
		entity.damage(damageSource(entity), BleedRules.damagePerTick(amplifier + 1, GearConfig.get().bleeding().damagePerLevel()));
		if (entity.getWorld() instanceof ServerWorld world) {
			world.spawnParticles(EmberParticles.BLOOD_DROP, entity.getX(), entity.getBodyY(0.6), entity.getZ(), 2 + amplifier,
					entity.getWidth() * 0.3, entity.getHeight() * 0.2, entity.getWidth() * 0.3, 0.02);
		}
	}

	static void register() {
	}
}
