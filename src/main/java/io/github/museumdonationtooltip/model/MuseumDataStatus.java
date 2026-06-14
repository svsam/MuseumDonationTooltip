package io.github.museumdonationtooltip.model;

/**
 * Describes the most recent museum data source or failure.
 */
public enum MuseumDataStatus {
	READY("Museum data is current"),
	CACHED("Using cached museum data"),
	LOADING("Museum data is loading"),
	MISSING_API_KEY("API key is not configured"),
	FORBIDDEN("Hypixel rejected the API key"),
	INVALID_REQUEST("Hypixel rejected the profile request"),
	RATE_LIMITED("Hypixel API rate limit reached"),
	PRIVATE_DATA("Museum or profile API data is unavailable"),
	PROFILE_NOT_FOUND("No SkyBlock profile was found"),
	NETWORK_ERROR("Hypixel API network request failed"),
	MALFORMED_RESPONSE("Hypixel API response could not be parsed");

	private final String displayText;

	MuseumDataStatus(String displayText) {
		this.displayText = displayText;
	}

	public String displayText() {
		return displayText;
	}
}

