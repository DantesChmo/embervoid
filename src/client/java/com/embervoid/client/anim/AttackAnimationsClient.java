package com.embervoid.client.anim;

import com.embervoid.Embervoid;
import com.embervoid.anim.AttackAnimations;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.api.layered.modifier.MirrorModifier;
import dev.kosmx.playerAnim.api.layered.modifier.SpeedModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Client side of the player attack animations ({@link AttackAnimations}, PLAN 17.1): every player gets a layer of ours
 * in their playerAnimator stack, and a packet puts a clip on it, stretched to the ticks the server sent (the
 * weapon's attack cooldown) and mirrored for a weapon in the left arm. It fades in from the pose before (vanilla or
 * the previous swing) so swings in a row blend.
 */
public final class AttackAnimationsClient {
	private static final Identifier LAYER = Embervoid.id("attack");
	/** Layer priority in the player's animation stack: a layer with a higher one plays over ours. */
	private static final int PRIORITY = 900;
	private static final int FADE_IN_TICKS = 2;
	private static final int FADE_OUT_TICKS = 4;
	/** Clips already reported missing, so a broken resource pack logs once per clip. */
	private static final Set<Identifier> MISSING = new HashSet<>();
	private static final List<ClipListener> LISTENERS = new ArrayList<>();

	/** Told of every clip a player starts, before it is put on the layer (the Scourge's lash follows its swings). */
	public interface ClipListener {
		void started(AbstractClientPlayerEntity player, Identifier clip, int ticks);
	}

	public static void listen(ClipListener listener) {
		LISTENERS.add(listener);
	}

	public static void register() {
		PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, PRIORITY, player -> new ModifierLayer<>());
		ClientPlayNetworking.registerGlobalReceiver(AttackAnimations.PACKET, (client, handler, buf, responseSender) -> {
			int entityId = buf.readVarInt();
			int ticks = buf.readVarInt();
			Identifier clip = ticks > 0 ? buf.readIdentifier() : null;
			boolean mirrored = ticks > 0 && buf.readBoolean();
			client.execute(() -> play(client, entityId, clip, ticks, mirrored));
		});
	}

	private static void play(MinecraftClient client, int entityId, @Nullable Identifier clip, int ticks, boolean mirrored) {
		if (client.world == null || !(client.world.getEntityById(entityId) instanceof AbstractClientPlayerEntity player)) {
			return;
		}
		if (clip != null) {
			for (ClipListener listener : LISTENERS) {
				listener.started(player, clip, ticks);
			}
		}
		ModifierLayer<IAnimation> layer = layer(player);
		if (layer == null) {
			return;
		}
		while (layer.size() > 0) {
			layer.removeModifier(0);
		}
		if (clip == null) {
			layer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(FADE_OUT_TICKS, Ease.INOUTSINE), null);
			return;
		}
		KeyframeAnimation animation = PlayerAnimationRegistry.getAnimation(clip);
		if (animation == null) {
			if (MISSING.add(clip)) {
				Embervoid.LOGGER.warn("Missing player animation {}", clip);
			}
			return;
		}
		layer.addModifierLast(new SpeedModifier((float) animation.getLength() / ticks));
		layer.addModifierLast(new MirrorModifier(mirrored));
		layer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(FADE_IN_TICKS, Ease.OUTSINE),
				new KeyframeAnimationPlayer(animation), true);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private static ModifierLayer<IAnimation> layer(AbstractClientPlayerEntity player) {
		return PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER) instanceof ModifierLayer<?> layer
				? (ModifierLayer<IAnimation>) layer : null;
	}

	private AttackAnimationsClient() {}
}
