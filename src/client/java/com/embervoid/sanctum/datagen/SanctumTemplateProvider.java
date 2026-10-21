package com.embervoid.sanctum.datagen;

import com.google.common.hash.Hashing;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.Identifier;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Writes the Sanctum's structure templates ({@link SanctumTemplates}) to data/embervoid/structures. */
public class SanctumTemplateProvider implements DataProvider {
	private final DataOutput.PathResolver resolver;

	public SanctumTemplateProvider(FabricDataOutput output) {
		this.resolver = output.getResolver(DataOutput.OutputType.DATA_PACK, "structures");
	}

	@Override
	public CompletableFuture<?> run(DataWriter writer) {
		return CompletableFuture.runAsync(() -> {
			for (Map.Entry<Identifier, TemplateBuilder> piece : SanctumTemplates.all().entrySet()) {
				try {
					ByteArrayOutputStream bytes = new ByteArrayOutputStream();
					NbtIo.writeCompressed(piece.getValue().toNbt(), bytes);
					byte[] data = bytes.toByteArray();
					writer.write(resolver.resolve(piece.getKey(), "nbt"), data, Hashing.sha1().hashBytes(data));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		});
	}

	@Override
	public String getName() {
		return "Sanctum structure templates";
	}
}
