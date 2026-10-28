package com.embervoid.progress;

import com.embervoid.hub.HubLayout;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RealmOrderTest {
	@Test
	void nextRealmWalksTheOrderAndStopsAtTheLast() {
		List<Identifier> order = ProgressManager.REALM_ORDER;
		for (int i = 0; i + 1 < order.size(); i++) {
			assertEquals(order.get(i + 1), ProgressManager.nextRealm(order.get(i)));
		}
		assertNull(ProgressManager.nextRealm(order.get(order.size() - 1)));
	}

	@Test
	void unknownRealmHasNoNext() {
		assertNull(ProgressManager.nextRealm(new Identifier("othermod", "realm_1")));
	}

	@Test
	void realmOneComesFirst() {
		assertEquals(ProgressManager.REALM_1, ProgressManager.REALM_ORDER.get(0));
	}

	@Test
	void hubObelisksLeadToTheRealmsInOrder() {
		assertEquals(ProgressManager.REALM_ORDER, HubLayout.OBELISKS.stream().map(HubLayout.Obelisk::realm).toList());
	}
}
