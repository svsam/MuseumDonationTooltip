package io.github.museumdonationtooltip.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DonatableItemRegistryTest {
	@Test
	void resolvesAliasesAndArmorSetDonationKeys() throws Exception {
		String json = """
				{
				  "items": [
				    {
				      "id": "FARM_ARMOR_CHESTPLATE",
				      "category": "FARMING",
				      "museumKeys": ["FARM_ARMOR"],
				      "aliases": ["STARRED_FARM_ARMOR_CHESTPLATE"]
				    }
				  ]
				}
				""";

		DonatableItemRegistry registry = DonatableItemRegistry.load(
				new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))
		);
		DonatableItem item = registry.find("starred_farm_armor_chestplate").orElseThrow();

		assertEquals("FARM_ARMOR_CHESTPLATE", item.itemId());
		assertTrue(item.museumKeys().contains("FARM_ARMOR"));
		assertTrue(item.museumKeys().contains("FARM_ARMOR_CHESTPLATE"));
	}
}
