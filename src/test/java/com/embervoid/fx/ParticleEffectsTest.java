package com.embervoid.fx;

import com.embervoid.fx.particle.SeekParticleEffect;
import com.embervoid.fx.particle.TintedParticleEffect;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParticleEffectsTest {
	private static TintedParticleEffect tintedFromCommand(String arguments) throws CommandSyntaxException {
		StringReader reader = new StringReader(arguments);
		TintedParticleEffect effect = TintedParticleEffect.FACTORY.read(null, reader);
		assertFalse(reader.canRead(), "left unread: '" + reader.getRemaining() + "'");
		return effect;
	}

	@Test
	void tintedEffectReadsTheDocumentedHexColourFromACommand() throws CommandSyntaxException {
		TintedParticleEffect effect = tintedFromCommand(" 3 0xFF7A2A");
		assertEquals(3, effect.size());
		assertEquals(0xFF7A2A, effect.color());
	}

	@Test
	void tintedEffectReadsHexInEitherCase() throws CommandSyntaxException {
		assertEquals(0xABCDEF, tintedFromCommand(" 1 0XabcDEF").color());
	}

	@Test
	void tintedEffectStillReadsADecimalColour() throws CommandSyntaxException {
		// 0x00FF00 is 65280.
		TintedParticleEffect effect = tintedFromCommand(" 1.5 65280");
		assertEquals(1.5f, effect.size());
		assertEquals(0x00FF00, effect.color());
	}

	@Test
	void tintedEffectRefusesAColourThatIsNoNumber() {
		assertThrows(CommandSyntaxException.class, () -> TintedParticleEffect.FACTORY.read(null, new StringReader(" 3 orange")));
		assertThrows(CommandSyntaxException.class, () -> TintedParticleEffect.FACTORY.read(null, new StringReader(" 3 0xGG0000")));
		assertThrows(CommandSyntaxException.class, () -> TintedParticleEffect.FACTORY.read(null, new StringReader(" 3")));
	}

	@Test
	void tintedEffectSurvivesThePacket() {
		TintedParticleEffect effect = new TintedParticleEffect(null, 3.5f, CombatFx.EMBER);
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		effect.write(buf);
		assertEquals(effect, TintedParticleEffect.FACTORY.read(null, buf));
		assertEquals(0, buf.readableBytes());
	}

	@Test
	void tintedEffectSurvivesTheCodec() {
		Codec<TintedParticleEffect> codec = TintedParticleEffect.codec(null);
		TintedParticleEffect effect = new TintedParticleEffect(null, 0.25f, CombatFx.BLOOD);
		NbtElement nbt = codec.encodeStart(NbtOps.INSTANCE, effect).getOrThrow(false, s -> {});
		assertEquals(effect, codec.parse(NbtOps.INSTANCE, nbt).getOrThrow(false, s -> {}));
	}

	@Test
	void tintedEffectNeedsBothArguments() {
		assertThrows(CommandSyntaxException.class, () -> TintedParticleEffect.FACTORY.read(null, new StringReader(" 3")));
	}

	@Test
	void seekEffectSurvivesThePacketAndTheCodec() {
		SeekParticleEffect effect = new SeekParticleEffect(null, new Vec3d(-12.25, 70.5, 1e6));
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		effect.write(buf);
		assertEquals(effect, SeekParticleEffect.FACTORY.read(null, buf));
		assertEquals(0, buf.readableBytes());
		Codec<SeekParticleEffect> codec = SeekParticleEffect.codec(null);
		NbtElement nbt = codec.encodeStart(NbtOps.INSTANCE, effect).getOrThrow(false, s -> {});
		assertEquals(effect, codec.parse(NbtOps.INSTANCE, nbt).getOrThrow(false, s -> {}));
	}

	@Test
	void seekEffectReadsItsTargetFromACommand() throws CommandSyntaxException {
		SeekParticleEffect effect = SeekParticleEffect.FACTORY.read(null, new StringReader(" 1.5 -2 3"));
		assertEquals(new Vec3d(1.5, -2, 3), effect.target());
	}
}
