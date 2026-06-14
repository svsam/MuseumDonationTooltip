package io.github.museumdonationtooltip.config;

import java.util.Locale;
import java.util.Set;

/**
 * User-editable settings stored in config/museumdonationtooltip.json.
 */
public final class MuseumConfig {
	private static final int MIN_REFRESH_MINUTES = 5;
	private static final int MAX_REFRESH_MINUTES = 24 * 60;
	private static final Set<String> VALID_COLORS = Set.of(
			"black", "dark_blue", "dark_green", "dark_aqua", "dark_red",
			"dark_purple", "gold", "gray", "dark_gray", "blue", "green",
			"aqua", "red", "light_purple", "yellow", "white"
	);

	public String apiKey = "";
	public boolean enabled = true;
	public boolean showUnknown = true;
	public int cacheRefreshMinutes = 15;
	public String donatedColor = "green";
	public String notDonatedColor = "red";
	public String notDonatableColor = "dark_gray";
	public String unknownColor = "yellow";

	public void validate() {
		apiKey = apiKey == null ? "" : apiKey.trim();
		cacheRefreshMinutes = Math.max(MIN_REFRESH_MINUTES, Math.min(MAX_REFRESH_MINUTES, cacheRefreshMinutes));
		donatedColor = validColor(donatedColor, "green");
		notDonatedColor = validColor(notDonatedColor, "red");
		notDonatableColor = validColor(notDonatableColor, "dark_gray");
		unknownColor = validColor(unknownColor, "yellow");
	}

	private static String validColor(String value, String fallback) {
		if (value == null) {
			return fallback;
		}
		String normalized = value.trim().toLowerCase(Locale.ROOT);
		return VALID_COLORS.contains(normalized) ? normalized : fallback;
	}
}

