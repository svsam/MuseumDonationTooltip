package io.github.museumdonationtooltip.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.museumdonationtooltip.item.ItemNormalizer;
import io.github.museumdonationtooltip.model.MuseumDataStatus;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Defensively supports both documented and historically observed museum response nesting.
 */
public final class MuseumApiParser {
	private MuseumApiParser() {
	}

	public static Set<String> parseDonatedKeys(JsonObject response, UUID playerUuid) throws HypixelApiException {
		if (response == null) {
			throw malformed("Empty museum response");
		}
		if (response.has("success") && !booleanValue(response.get("success")).orElse(false)) {
			throw new HypixelApiException(MuseumDataStatus.INVALID_REQUEST, "Hypixel reported an unsuccessful request");
		}

		JsonElement profileElement = response.get("profile");
		if (profileElement == null || profileElement.isJsonNull()) {
			throw new HypixelApiException(MuseumDataStatus.PRIVATE_DATA, "Museum data is private or missing");
		}
		if (!profileElement.isJsonObject()) {
			throw malformed("profile is not an object");
		}

		JsonObject member = selectMember(profileElement.getAsJsonObject(), playerUuid);
		Set<String> result = new HashSet<>();
		boolean recognized = collectItems(member.get("items"), result);
		recognized |= collectItems(member.get("special"), result);
		recognized |= collectItems(member.get("special_items"), result);

		if (!recognized && !member.has("value") && !member.has("appraisal")) {
			throw malformed("Museum member data has no recognized donation fields");
		}
		return Set.copyOf(result);
	}

	private static JsonObject selectMember(JsonObject profile, UUID playerUuid) throws HypixelApiException {
		if (hasDonationShape(profile)) {
			return profile;
		}

		JsonObject members = profile.has("members") && profile.get("members").isJsonObject()
				? profile.getAsJsonObject("members")
				: profile;
		String wanted = ProfileSelector.compactUuid(playerUuid.toString());
		JsonObject onlyCandidate = null;
		int candidates = 0;

		for (String key : members.keySet()) {
			JsonElement value = members.get(key);
			if (!value.isJsonObject()) {
				continue;
			}
			JsonObject candidate = value.getAsJsonObject();
			if (!hasDonationShape(candidate)) {
				continue;
			}
			candidates++;
			onlyCandidate = candidate;
			if (ProfileSelector.compactUuid(key).equals(wanted)) {
				return candidate;
			}
		}

		if (candidates == 1) {
			return onlyCandidate;
		}
		throw malformed("Could not locate the current member in museum data");
	}

	private static boolean hasDonationShape(JsonObject object) {
		return object.has("items")
				|| object.has("special")
				|| object.has("special_items")
				|| object.has("value")
				|| object.has("appraisal");
	}

	private static boolean collectItems(JsonElement element, Set<String> result) {
		if (element == null || element.isJsonNull()) {
			return false;
		}
		if (element.isJsonArray()) {
			for (JsonElement value : element.getAsJsonArray()) {
				collectValue(value, result);
			}
			return true;
		}
		if (element.isJsonObject()) {
			for (String key : element.getAsJsonObject().keySet()) {
				ItemNormalizer.normalize(key).ifPresent(result::add);
				collectValue(element.getAsJsonObject().get(key), result);
			}
			return true;
		}
		return false;
	}

	private static void collectValue(JsonElement value, Set<String> result) {
		if (value == null || value.isJsonNull()) {
			return;
		}
		if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
			ItemNormalizer.normalize(value.getAsString()).ifPresent(result::add);
			return;
		}
		if (!value.isJsonObject()) {
			return;
		}
		JsonObject object = value.getAsJsonObject();
		for (String field : new String[] {"id", "item_id", "itemId"}) {
			JsonElement id = object.get(field);
			if (id != null && id.isJsonPrimitive() && id.getAsJsonPrimitive().isString()) {
				ItemNormalizer.normalize(id.getAsString()).ifPresent(result::add);
			}
		}
	}

	private static Optional<Boolean> booleanValue(JsonElement value) {
		if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
			return Optional.empty();
		}
		return Optional.of(value.getAsBoolean());
	}

	private static HypixelApiException malformed(String message) {
		return new HypixelApiException(MuseumDataStatus.MALFORMED_RESPONSE, message);
	}
}
