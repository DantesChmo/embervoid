package com.embervoid.sanctum.leveling;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatSheetTest {
	@Test
	void packetRoundTrip() {
		StatSheet sheet = new StatSheet(12, 95, 140, List.of(
				new StatSheet.Stat("vitality", 14, 99, List.of(new StatSheet.Derived("embervoid.derived.max_health", "24", "25"))),
				new StatSheet.Stat("fortune", 99, 99, List.of())));
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		sheet.write(buf);
		assertEquals(sheet, StatSheet.read(buf));
		assertEquals(0, buf.readableBytes());
	}

	@Test
	void affordability() {
		assertTrue(new StatSheet(1, 26, 26, List.of()).canAfford());
		assertFalse(new StatSheet(1, 26, 25, List.of()).canAfford());
	}
}
