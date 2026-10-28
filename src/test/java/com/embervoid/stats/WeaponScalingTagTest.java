package com.embervoid.stats;

import com.embervoid.stats.WeaponScaling.Scaling;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The command tag a projectile keeps its weapon's scaling in: {@code embervoid.scaling:<str>:<dex>}. */
class WeaponScalingTagTest {
	@Test
	void theDocumentedFormReads() {
		assertEquals(new Scaling(0.5f, 0.5f), WeaponScaling.parseTag("embervoid.scaling:0.5:0.5"));
		assertEquals(new Scaling(0, 1), WeaponScaling.parseTag("embervoid.scaling:0:1"));
	}

	@Test
	void everyScalingSurvivesItsTag() {
		for (Scaling scaling : new Scaling[] {Scaling.STRENGTH, Scaling.DEXTERITY, Scaling.HYBRID, new Scaling(0.25f, 1.75f)}) {
			assertEquals(scaling, WeaponScaling.parseTag(WeaponScaling.toTag(scaling)));
		}
	}

	@Test
	void theTagIsWrittenInTheDocumentedForm() {
		String tag = WeaponScaling.toTag(new Scaling(1, 0));
		assertEquals("embervoid.scaling:", tag.substring(0, "embervoid.scaling:".length()));
		assertEquals(3, tag.split(":").length);
	}

	@Test
	void otherTagsAreNotScaling() {
		assertNull(WeaponScaling.parseTag("embervoid.placed"));
		assertNull(WeaponScaling.parseTag("scaling:1:0"));
		assertNull(WeaponScaling.parseTag(""));
	}

	@Test
	void aBrokenScalingTagGivesNoBonus() {
		assertEquals(Scaling.NONE, WeaponScaling.parseTag("embervoid.scaling:"));
		assertEquals(Scaling.NONE, WeaponScaling.parseTag("embervoid.scaling:1"));
		assertEquals(Scaling.NONE, WeaponScaling.parseTag("embervoid.scaling:strong:0"));
	}
}
