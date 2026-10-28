package com.embervoid.fx.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;

import java.util.Locale;

/**
 * A particle with a size (blocks) and a colour (0xRRGGBB): the shockwave ring, the ground scorch and the burst flash.
 * {@code /particle embervoid:shockwave 3 0xFF7A2A} on the command line.
 */
public record TintedParticleEffect(ParticleType<TintedParticleEffect> type, float size, int color) implements ParticleEffect {
	public static Codec<TintedParticleEffect> codec(ParticleType<TintedParticleEffect> type) {
		return RecordCodecBuilder.create(instance -> instance.group(
				Codec.FLOAT.fieldOf("size").forGetter(TintedParticleEffect::size),
				Codec.INT.fieldOf("color").forGetter(TintedParticleEffect::color)
		).apply(instance, (size, color) -> new TintedParticleEffect(type, size, color)));
	}

	public static final Factory<TintedParticleEffect> FACTORY = new Factory<>() {
		@Override
		public TintedParticleEffect read(ParticleType<TintedParticleEffect> type, StringReader reader) throws CommandSyntaxException {
			reader.expect(' ');
			float size = reader.readFloat();
			reader.expect(' ');
			int color = readColor(reader);
			return new TintedParticleEffect(type, size, color);
		}

		@Override
		public TintedParticleEffect read(ParticleType<TintedParticleEffect> type, PacketByteBuf buf) {
			return new TintedParticleEffect(type, buf.readFloat(), buf.readInt());
		}
	};

	/** A colour as {@code 0xRRGGBB} (any case) or as a decimal int. */
	static int readColor(StringReader reader) throws CommandSyntaxException {
		int start = reader.getCursor();
		String token = reader.readUnquotedString();
		if (token.isEmpty()) {
			throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedInt().createWithContext(reader);
		}
		try {
			if (token.length() > 2 && token.charAt(0) == '0' && (token.charAt(1) == 'x' || token.charAt(1) == 'X')) {
				return Integer.parseUnsignedInt(token.substring(2), 16);
			}
			return Integer.parseInt(token);
		} catch (NumberFormatException e) {
			reader.setCursor(start);
			throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidInt().createWithContext(reader, token);
		}
	}

	@Override
	public ParticleType<?> getType() {
		return type;
	}

	@Override
	public void write(PacketByteBuf buf) {
		buf.writeFloat(size);
		buf.writeInt(color);
	}

	@Override
	public String asString() {
		return String.format(Locale.ROOT, "%s %.2f %d", Registries.PARTICLE_TYPE.getId(type), size, color);
	}
}
