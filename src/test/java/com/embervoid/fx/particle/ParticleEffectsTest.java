package com.embervoid.fx.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The particle effects' command and packet forms; the type is not needed to read them, so it is left null. */
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
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		new TintedParticleEffect(null, 2.25f, 0x123456).write(buf);
		TintedParticleEffect read = TintedParticleEffect.FACTORY.read(null, buf);
		assertEquals(2.25f, read.size());
		assertEquals(0x123456, read.color());
	}

	@Test
	void seekEffectReadsItsTargetFromACommandAndThePacket() throws CommandSyntaxException {
		StringReader reader = new StringReader(" 1.5 -2 64");
		SeekParticleEffect effect = SeekParticleEffect.FACTORY.read(null, reader);
		assertEquals(new Vec3d(1.5, -2, 64), effect.target());
		assertFalse(reader.canRead());

		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		effect.write(buf);
		assertEquals(effect.target(), SeekParticleEffect.FACTORY.read(null, buf).target());
	}
}
