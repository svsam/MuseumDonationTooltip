package io.github.museumdonationtooltip.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HypixelApiClientTest {
	@Test
	void extractsHypixelErrorCauseWithoutLeakingRequestData() {
		assertEquals(
				"Invalid API key",
				HypixelApiClient.errorDetail("""
						{"success": false, "cause": "Invalid API key"}
						""").orElseThrow()
		);
		assertTrue(HypixelApiClient.errorDetail("not-json").isEmpty());
	}
}
