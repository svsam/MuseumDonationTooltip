package io.github.museumdonationtooltip.registry;

import java.util.Set;

/**
 * One hoverable item ID and all museum keys that can prove its donation.
 */
public record DonatableItem(String itemId, String category, Set<String> museumKeys) {
	public DonatableItem {
		museumKeys = Set.copyOf(museumKeys);
	}
}

