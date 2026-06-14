package io.github.museumdonationtooltip.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProfileSelectorTest {
	private static final UUID PLAYER = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

	@Test
	void prefersSelectedProfile() throws Exception {
		var response = JsonParser.parseString("""
				{
				  "success": true,
				  "profiles": [
				    {"profile_id": "old", "selected": false},
				    {"profile_id": "selected-profile", "selected": true}
				  ]
				}
				""").getAsJsonObject();

		assertEquals("selected-profile", ProfileSelector.selectProfileId(response, PLAYER));
	}

	@Test
	void fallsBackToLatestMemberSave() throws Exception {
		var response = JsonParser.parseString("""
				{
				  "success": true,
				  "profiles": [
				    {
				      "profile_id": "older",
				      "members": {"aaaaaaaabbbbccccddddeeeeeeeeeeee": {"last_save": 10}}
				    },
				    {
				      "profile_id": "newer",
				      "members": {"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee": {"last_save": 20}}
				    }
				  ]
				}
				""").getAsJsonObject();

		assertEquals("newer", ProfileSelector.selectProfileId(response, PLAYER));
	}
}

