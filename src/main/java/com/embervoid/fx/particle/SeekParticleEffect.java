package com.embervoid.fx.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

/** A particle that flies to a point in the world: the soul wisp the Scythe draws out of a kill into the player. */
public record SeekParticleEffect(ParticleType<SeekParticleEffect> type, Vec3d target) implements ParticleEffect {
	public static Codec<SeekParticleEffect> codec(ParticleType<SeekParticleEffect> type) {
		return RecordCodecBuilder.create(instance -> instance.group(
				Codec.DOUBLE.fieldOf("x").forGetter(e -> e.target().x),
				Codec.DOUBLE.fieldOf("y").forGetter(e -> e.target().y),
				Codec.DOUBLE.fieldOf("z").forGetter(e -> e.target().z)
		).apply(instance, (x, y, z) -> new SeekParticleEffect(type, new Vec3d(x, y, z))));
	}

	public static final Factory<SeekParticleEffect> FACTORY = new Factory<>() {
		@Override
		public SeekParticleEffect read(ParticleType<SeekParticleEffect> type, StringReader reader) throws CommandSyntaxException {
			reader.expect(' ');
			double x = reader.readDouble();
			reader.expect(' ');
			double y = reader.readDouble();
			reader.expect(' ');
			double z = reader.readDouble();
			return new SeekParticleEffect(type, new Vec3d(x, y, z));
		}

		@Override
		public SeekParticleEffect read(ParticleType<SeekParticleEffect> type, PacketByteBuf buf) {
			return new SeekParticleEffect(type, new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble()));
		}
	};

	@Override
	public ParticleType<?> getType() {
		return type;
	}

	@Override
	public void write(PacketByteBuf buf) {
		buf.writeDouble(target.x);
		buf.writeDouble(target.y);
		buf.writeDouble(target.z);
	}

	@Override
	public String asString() {
		return String.format(Locale.ROOT, "%s %.2f %.2f %.2f", Registries.PARTICLE_TYPE.getId(type), target.x, target.y, target.z);
	}
}
