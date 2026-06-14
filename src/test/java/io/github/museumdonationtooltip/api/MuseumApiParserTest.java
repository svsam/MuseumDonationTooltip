package io.github.museumdonationtooltip.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import io.github.museumdonationtooltip.model.MuseumDataStatus;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MuseumApiParserTest {
	private static final UUID PLAYER = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

	@Test
	void parsesDocumentedDirectShape() throws Exception {
		var response = JsonParser.parseString("""
				{
				  "success": true,
				  "profile": {
				    "value": 100,
				    "items": {
				      "MITHRIL_PICKAXE": {"donated_time": 1},
				      "FARM_ARMOR": {}
				    },
				    "special": ["SPECIAL_ITEM"]
				  }
				}
				""").getAsJsonObject();

		Set<String> keys = MuseumApiParser.parseDonatedKeys(response, PLAYER);
		assertEquals(Set.of("MITHRIL_PICKAXE", "FARM_ARMOR", "SPECIAL_ITEM"), keys);
	}

	@Test
	void parsesUuidMemberMapAndNestedMembers() throws Exception {
		var directMemberMap = JsonParser.parseString("""
				{
				  "success": true,
				  "profile": {
				    "aaaaaaaabbbbccccddddeeeeeeeeeeee": {
				      "items": {"ASPECT_OF_THE_END": {}},
				      "special": []
				    }
				  }
				}
				""").getAsJsonObject();
		assertTrue(MuseumApiParser.parseDonatedKeys(directMemberMap, PLAYER).contains("ASPECT_OF_THE_END"));

		var nestedMembers = JsonParser.parseString("""
				{
				  "success": true,
				  "profile": {
				    "members": {
				      "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee": {
				        "items": {},
				        "special_items": [{"item_id": "HEGEMONY_ARTIFACT"}]
				      }
				    }
				  }
				}
				""").getAsJsonObject();
		assertTrue(MuseumApiParser.parseDonatedKeys(nestedMembers, PLAYER).contains("HEGEMONY_ARTIFACT"));
	}

	@Test
	void treatsNullProfileAsPrivateData() {
		var response = JsonParser.parseString("""
				{"success": true, "profile": null}
				""").getAsJsonObject();

		HypixelApiException exception = assertThrows(
				HypixelApiException.class,
				() -> MuseumApiParser.parseDonatedKeys(response, PLAYER)
		);
		assertEquals(MuseumDataStatus.PRIVATE_DATA, exception.status());
	}
}

