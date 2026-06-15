package io.github.museumdonationtooltip.tooltip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.museumdonationtooltip.model.TooltipState;
import io.github.museumdonationtooltip.registry.DonatableItemRegistry;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MuseumTooltipHandlerTest {
	@Test
	void omitsNonDonatableItemsAndResolvesKnownItems() throws Exception {
		String json = """
				{
				  "items": [
				    {
				      "id": "ASPECT_OF_THE_END",
				      "category": "COMBAT",
				      "museumKeys": ["ASPECT_OF_THE_END"],
				      "aliases": []
				    }
				  ]
				}
				""";
		DonatableItemRegistry registry = DonatableItemRegistry.load(
				new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))
		);

		assertTrue(MuseumTooltipHandler.resolveState(
				"NON_DONATABLE_ITEM",
				registry,
				item -> TooltipState.DONATED
		).isEmpty());
		assertEquals(
				TooltipState.DONATED,
				MuseumTooltipHandler.resolveState(
						"ASPECT_OF_THE_END",
						registry,
						item -> TooltipState.DONATED
				).orElseThrow()
		);
	}
}
