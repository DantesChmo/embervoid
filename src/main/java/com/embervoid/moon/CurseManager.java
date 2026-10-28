package com.embervoid.moon;

import com.embervoid.Embervoid;
import com.embervoid.boss.ArenaBoss;
import com.embervoid.moon.api.MoonNight;
import com.embervoid.moon.api.MoonNights;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Curse of the Night (DESIGN §3.3, §10.12, PLAN 8.5). A player's death adds a stack when the killer is a hostile mob
 * or the owner of its projectile, anywhere (realms and bosses included). Deaths without an attacking mob (falls,
 * drowning, suffocation, lava, the void) and kills by players add nothing. All stacks reset when a full moon ends.
 *
 * <p>Stacks live in the player's own attachment together with the world's curse epoch (the number of full moons
 * that have ended) at the time; stacks from an older epoch count as zero, so offline players reset too.
 */
public final class CurseManager {
	/** A player's stacks and the curse epoch they were gathered in. */
	record Curse(int stacks, long epoch) {
		static final Codec<Curse> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.fieldOf("stacks").forGetter(Curse::stacks),
				Codec.LONG.fieldOf("epoch").forGetter(Curse::epoch)
		).apply(instance, Curse::new));
	}

	// Persistent and copied on death (the stack is added while the player is dying).
	@SuppressWarnings("UnstableApiUsage")
	private static final AttachmentType<Curse> CURSE = AttachmentRegistry.<Curse>builder()
			.persistent(Curse.CODEC)
			.copyOnDeath()
			.buildAndRegister(Embervoid.id("curse_of_the_night"));

	static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayerEntity player && countsForCurse(source)) {
				int stacks = add(player);
				player.sendMessage(Text.translatable("embervoid.moon.curse.stack", stacks).formatted(Formatting.DARK_PURPLE));
			}
		});
		MoonNights.NIGHT_ENDED.register((server, night, reason) -> {
			if (night == MoonNight.FULL_MOON) {
				MoonNightState state = MoonNightState.get(server);
				state.curseEpoch++;
				state.markDirty();
			}
		});
	}

	/** Whether a death from {@code source} adds a stack: the attacker (or projectile owner) is a hostile mob. */
	static boolean countsForCurse(DamageSource source) {
		return CurseRules.countsForCurse(attackerOf(source));
	}

	private static CurseRules.Attacker attackerOf(DamageSource source) {
		Entity attacker = source.getAttacker();
		if (attacker == null) {
			return CurseRules.Attacker.NONE;
		}
		if (attacker instanceof PlayerEntity) {
			return CurseRules.Attacker.PLAYER;
		}
		return attacker instanceof Monster || attacker instanceof ArenaBoss ? CurseRules.Attacker.HOSTILE : CurseRules.Attacker.OTHER;
	}

	/** The player's current stacks (server side). */
	@SuppressWarnings("UnstableApiUsage")
	public static int stacks(PlayerEntity player) {
		MinecraftServer server = player.getServer();
		Curse curse = player.getAttached(CURSE);
		if (server == null || curse == null) {
			return 0;
		}
		return CurseRules.stacks(curse.stacks(), curse.epoch(), MoonNightState.get(server).curseEpoch());
	}

	/** Adds a stack and returns the new count. */
	@SuppressWarnings("UnstableApiUsage")
	static int add(ServerPlayerEntity player) {
		int stacks = stacks(player) + 1;
		player.setAttached(CURSE, new Curse(stacks, MoonNightState.get(player.server).curseEpoch()));
		return stacks;
	}

	/** Sets the player's stacks (debug). */
	@SuppressWarnings("UnstableApiUsage")
	static void set(ServerPlayerEntity player, int stacks) {
		player.setAttached(CURSE, new Curse(stacks, MoonNightState.get(player.server).curseEpoch()));
	}

	private CurseManager() {}
}
