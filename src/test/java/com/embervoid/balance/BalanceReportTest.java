package com.embervoid.balance;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

/** Writes the model's tables to {@code build/balance/tables.md}, the source of the tables in docs/balance-0.4.0.md. */
class BalanceReportTest {
	@Test
	void writesTheTables() throws IOException {
		String tables = new BalanceReport(BalanceModel.Playthrough.NORMAL).render();
		assertFalse(tables.isBlank());
		Path file = Path.of("build", "balance", "tables.md");
		Files.createDirectories(file.getParent());
		Files.writeString(file, tables);
	}
}
