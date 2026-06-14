package io.github.museumdonationtooltip.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.museumdonationtooltip.model.MuseumDataStatus;
import java.util.Optional;
import java.util.UUID;

/**
 * Selects the API profile marked selected, with last-save fallback for older responses.
 */
public final class ProfileSelector {
	private ProfileSelector() {
	}

	public static String selectProfileId(JsonObject response, UUID playerUuid) throws HypixelApiException {
		requireSuccess(response);
		JsonElement profilesElement = response.get("profiles");
		if (profilesElement == null || profilesElement.isJsonNull()) {
			throw new HypixelApiException(MuseumDataStatus.PRIVATE_DATA, "SkyBlock profile data is private or missing");
		}
		if (!profilesElement.isJsonArray()) {
			throw new HypixelApiException(MuseumDataStatus.MALFORMED_RESPONSE, "profiles is not an array");
		}

		JsonArray profiles = profilesElement.getAsJsonArray();
		if (profiles.isEmpty()) {
			throw new HypixelApiException(MuseumDataStatus.PROFILE_NOT_FOUND, "No SkyBlock profiles were returned");
		}

		JsonObject best = null;
		long bestLastSave = Long.MIN_VALUE;
		for (JsonElement element : profiles) {
			if (!element.isJsonObject()) {
				continue;
			}
			JsonObject profile = element.getAsJsonObject();
			if (booleanValue(profile, "selected").orElse(false) && profileId(profile).isPresent()) {
				return profileId(profile).orElseThrow();
			}

			long lastSave = memberLastSave(profile, playerUuid).orElse(Long.MIN_VALUE);
			if (profileId(profile).isPresent() && (best == null || lastSave > bestLastSave)) {
				best = profile;
				bestLastSave = lastSave;
			}
		}

		if (best == null) {
			throw new HypixelApiException(MuseumDataStatus.MALFORMED_RESPONSE, "No profile contained an ID");
		}
		return profileId(best).orElseThrow();
	}

	private static void requireSuccess(JsonObject response) throws HypixelApiException {
		if (response == null) {
			throw new HypixelApiException(MuseumDataStatus.MALFORMED_RESPONSE, "Empty profiles response");
		}
		if (response.has("success") && !booleanValue(response, "success").orElse(false)) {
			throw new HypixelApiException(MuseumDataStatus.INVALID_REQUEST, "Hypixel reported an unsuccessful request");
		}
	}

	private static Optional<String> profileId(JsonObject profile) {
		for (String name : new String[] {"profile_id", "profileId", "id"}) {
			JsonElement value = profile.get(name);
			if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
				return Optional.of(value.getAsString());
			}
		}
		return Optional.empty();
	}

	private static Optional<Long> memberLastSave(JsonObject profile, UUID playerUuid) {
		JsonObject members = object(profile, "members").orElse(null);
		if (members == null) {
			return Optional.empty();
		}
		String wanted = compactUuid(playerUuid.toString());
		for (String key : members.keySet()) {
			if (!compactUuid(key).equals(wanted) || !members.get(key).isJsonObject()) {
				continue;
			}
			JsonElement value = members.getAsJsonObject(key).get("last_save");
			if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
				return Optional.of(value.getAsLong());
			}
		}
		return Optional.empty();
	}

	private static Optional<Boolean> booleanValue(JsonObject object, String name) {
		JsonElement value = object.get(name);
		if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
			return Optional.empty();
		}
		return Optional.of(value.getAsBoolean());
	}

	private static Optional<JsonObject> object(JsonObject object, String name) {
		JsonElement value = object.get(name);
		return value != null && value.isJsonObject() ? Optional.of(value.getAsJsonObject()) : Optional.empty();
	}

	static String compactUuid(String value) {
		return value.replace("-", "").toLowerCase();
	}
}

