package io.github.museumdonationtooltip.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.museumdonationtooltip.model.MuseumDataStatus;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs public Hypixel API reads only. It never sends gameplay packets or actions.
 */
public final class HypixelApiClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(HypixelApiClient.class);
	private static final URI API_ROOT = URI.create("https://api.hypixel.net/v2/");

	private final HttpClient httpClient;

	public HypixelApiClient() {
		httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
	}

	public CompletableFuture<JsonObject> fetchProfiles(UUID playerUuid, String apiKey) {
		return request("skyblock/profiles?uuid=" + encode(playerUuid.toString()), apiKey);
	}

	public CompletableFuture<JsonObject> fetchMuseum(String profileId, String apiKey) {
		return request("skyblock/museum?profile=" + encode(profileId), apiKey);
	}

	private CompletableFuture<JsonObject> request(String pathAndQuery, String apiKey) {
		String endpointName = endpointName(pathAndQuery);
		HttpRequest request = HttpRequest.newBuilder(API_ROOT.resolve(pathAndQuery))
				.timeout(Duration.ofSeconds(20))
				.header("Accept", "application/json")
				.header("API-Key", apiKey)
				.header("User-Agent", "MuseumDonationTooltip/1.0")
				.GET()
				.build();

		LOGGER.info("Requesting Hypixel API endpoint {}", endpointName);
		return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
				.thenApply(response -> {
					try {
						LOGGER.info("Hypixel API endpoint {} responded HTTP {}", endpointName, response.statusCode());
						checkStatus(response);
						var parsed = JsonParser.parseString(response.body());
						if (!parsed.isJsonObject()) {
							throw new HypixelApiException(
									MuseumDataStatus.MALFORMED_RESPONSE,
									"Hypixel returned non-object JSON"
							);
						}
						return parsed.getAsJsonObject();
					} catch (HypixelApiException | RuntimeException exception) {
						throw new CompletionException(exception);
					}
				});
	}

	private static void checkStatus(HttpResponse<String> response) throws HypixelApiException {
		int code = response.statusCode();
		if (code >= 200 && code < 300) {
			return;
		}

		MuseumDataStatus status = switch (code) {
			case 403 -> MuseumDataStatus.FORBIDDEN;
			case 404 -> MuseumDataStatus.PROFILE_NOT_FOUND;
			case 422, 400 -> MuseumDataStatus.INVALID_REQUEST;
			case 429 -> MuseumDataStatus.RATE_LIMITED;
			default -> MuseumDataStatus.NETWORK_ERROR;
		};
		long retryAfter = code == 429 ? rateLimitReset(response) : 0;
		String detail = errorDetail(response.body())
				.map(cause -> "Hypixel API returned HTTP " + code + ": " + cause)
				.orElse("Hypixel API returned HTTP " + code);
		throw new HypixelApiException(status, detail, retryAfter);
	}

	static Optional<String> errorDetail(String body) {
		if (body == null || body.isBlank()) {
			return Optional.empty();
		}
		try {
			var parsed = JsonParser.parseString(body);
			if (!parsed.isJsonObject()) {
				return Optional.empty();
			}
			JsonObject object = parsed.getAsJsonObject();
			if (object.has("cause") && object.get("cause").isJsonPrimitive()) {
				return Optional.of(object.get("cause").getAsString());
			}
			if (object.has("message") && object.get("message").isJsonPrimitive()) {
				return Optional.of(object.get("message").getAsString());
			}
		} catch (RuntimeException ignored) {
			return Optional.empty();
		}
		return Optional.empty();
	}

	private static long rateLimitReset(HttpResponse<?> response) {
		Optional<String> value = response.headers().firstValue("RateLimit-Reset");
		if (value.isEmpty()) {
			value = response.headers().firstValue("Retry-After");
		}
		try {
			return value.map(Long::parseLong).orElse(60L);
		} catch (NumberFormatException ignored) {
			return 60L;
		}
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private static String endpointName(String pathAndQuery) {
		int queryStart = pathAndQuery.indexOf('?');
		return queryStart >= 0 ? pathAndQuery.substring(0, queryStart) : pathAndQuery;
	}
}
