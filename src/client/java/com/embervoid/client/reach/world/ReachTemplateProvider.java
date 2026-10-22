package com.embervoid.client.reach.world;

import com.embervoid.sanctum.datagen.TemplateBuilder;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Writes the Ashen Reach world templates (outposts, minor structures, crypt, Citadel) to data/embervoid/structures. */
public class ReachTemplateProvider implements DataProvider {
	private final DataOutput.PathResolver resolver;

	public ReachTemplateProvider(FabricDataOutput output) {
		this.resolver = output.getResolver(DataOutput.OutputType.DATA_PACK, "structures");
	}

	/** Every template this provider writes, by id. */
	public static Map<Identifier, TemplateBuilder> all() {
		Map<Identifier, TemplateBuilder> templates = new LinkedHashMap<>();
		templates.putAll(OutpostTemplates.all());
		return templates;
	}

	@Override
	public CompletableFuture<?> run(DataWriter writer) {
		return CompletableFuture.runAsync(() -> {
			for (Map.Entry<Identifier, TemplateBuilder> template : all().entrySet()) {
				try {
					ByteArrayOutputStream bytes = new ByteArrayOutputStream();
					NbtIo.writeCompressed(template.getValue().toNbt(), bytes);
					byte[] data = bytes.toByteArray();
					writer.write(resolver.resolve(template.getKey(), "nbt"), data, Hashing.sha1().hashBytes(data));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		});
	}

	@Override
	public String getName() {
		return "Ashen Reach world templates";
	}
}
