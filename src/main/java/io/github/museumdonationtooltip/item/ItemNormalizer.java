package io.github.museumdonationtooltip.item;

import java.util.Locale;
import java.util.Optional;

/**
 * Normalizes stable SkyBlock IDs, not mutable item-instance metadata.
 */
public final class ItemNormalizer {
	private ItemNormalizer() {
	}

	public static Optional<String> normalize(String rawId) {
		if (rawId == null) {
			return Optional.empty();
		}

		String normalized = rawId.trim();
		if (normalized.regionMatches(true, 0, "skyblock:", 0, "skyblock:".length())) {
			normalized = normalized.substring("skyblock:".length());
		}

		normalized = normalized.toUpperCase(Locale.ROOT).replace(' ', '_');
		if (normalized.isEmpty() || !normalized.matches("[A-Z0-9_.:-]+")) {
			return Optional.empty();
		}
		return Optional.of(normalized);
	}
}

