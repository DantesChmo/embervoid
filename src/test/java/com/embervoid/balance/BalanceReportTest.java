package com.embervoid.balance;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Writes the model's tables to {@code build/balance/}: {@code tables.md} (docs/balance-0.4.0.md), {@code overworld.md}
 * (docs/balance-overworld.md) and {@code fight-length.md} (docs/balance-0.5.0.md).
 */
class BalanceReportTest {
	@Test
	void writesTheTables() throws IOException {
		String tables = new BalanceReport(BalanceModel.Playthrough.NORMAL).render();
		assertFalse(tables.isBlank());
		Path file = Path.of("build", "balance", "tables.md");
		Files.createDirectories(file.getParent());
		Files.writeString(file, tables);
		Files.writeString(file.resolveSibling("overworld.md"), new OverworldReport().render());
		Files.writeString(file.resolveSibling("fight-length.md"), new FightLengthReport(BalanceModel.Playthrough.NORMAL).render());
	}
}
