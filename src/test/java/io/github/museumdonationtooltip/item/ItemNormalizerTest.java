package io.github.museumdonationtooltip.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ItemNormalizerTest {
	@Test
	void normalizesStableIds() {
		assertEquals("MITHRIL_PICKAXE", ItemNormalizer.normalize(" skyblock:mithril_pickaxe ").orElseThrow());
		assertEquals("INK_SACK:3", ItemNormalizer.normalize("ink_sack:3").orElseThrow());
	}

	@Test
	void rejectsUnsafeOrBlankIds() {
		assertTrue(ItemNormalizer.normalize("").isEmpty());
		assertTrue(ItemNormalizer.normalize("bad/id").isEmpty());
		assertTrue(ItemNormalizer.normalize(null).isEmpty());
	}
}

