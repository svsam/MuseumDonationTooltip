package io.github.museumdonationtooltip.service;

import io.github.museumdonationtooltip.api.HypixelApiClient;
import io.github.museumdonationtooltip.api.HypixelApiException;
import io.github.museumdonationtooltip.api.MuseumApiParser;
import io.github.museumdonationtooltip.api.ProfileSelector;
import io.github.museumdonationtooltip.cache.MuseumCacheManager;
import io.github.museumdonationtooltip.config.ConfigManager;
import io.github.museumdonationtooltip.config.MuseumConfig;
import io.github.museumdonationtooltip.model.MuseumDataStatus;
import io.github.museumdonationtooltip.model.MuseumSnapshot;
import io.github.museumdonationtooltip.model.TooltipState;
import io.github.museumdonationtooltip.registry.DonatableItem;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;

/**
 * Owns asynchronous refreshes and exposes lock-free local lookups to tooltips.
 */
public final class MuseumDonationService {
	private final ConfigManager configManager;
	private final MuseumCacheManager cacheManager;
	private final HypixelApiClient apiClient;
	private final Logger logger;
	private final AtomicReference<MuseumSnapshot> snapshot = new AtomicReference<>(
			MuseumSnapshot.empty(MuseumDataStatus.LOADING, "Waiting for a player")
	);
	private final AtomicBoolean refreshInFlight = new AtomicBoolean();
	private final AtomicLong nextRefreshAtMillis = new AtomicLong();

	private volatile UUID activePlayer;

	public MuseumDonationService(
			ConfigManager configManager,
			MuseumCacheManager cacheManager,
			HypixelApiClient apiClient,
			Logger logger
	) {
		this.configManager = configManager;
		this.cacheManager = cacheManager;
		this.apiClient = apiClient;
		this.logger = logger;
	}

	public MuseumSnapshot snapshot() {
		return snapshot.get();
	}

	public void tick(UUID playerUuid) {
		if (!playerUuid.equals(activePlayer)) {
			activePlayer = playerUuid;
			snapshot.set(cacheManager.load(playerUuid).orElseGet(
					() -> MuseumSnapshot.empty(MuseumDataStatus.LOADING, "No cached museum data")
			));
			nextRefreshAtMillis.set(0);
		}

		if (System.currentTimeMillis() >= nextRefreshAtMillis.get()) {
			refresh(playerUuid, false);
		}
	}

	public void disconnect() {
		activePlayer = null;
	}

	public boolean forceRefresh(UUID playerUuid) {
		nextRefreshAtMillis.set(0);
		return refresh(playerUuid, true);
	}

	public TooltipState lookup(DonatableItem item) {
		MuseumSnapshot current = snapshot.get();
		if (!current.hasUsableData()) {
			return TooltipState.UNKNOWN;
		}
		for (String museumKey : item.museumKeys()) {
			if (current.donatedKeys().contains(museumKey)) {
				return TooltipState.DONATED;
			}
		}
		return TooltipState.NOT_DONATED;
	}

	private boolean refresh(UUID playerUuid, boolean force) {
		if (!force && System.currentTimeMillis() < nextRefreshAtMillis.get()) {
			return false;
		}
		if (!refreshInFlight.compareAndSet(false, true)) {
			return false;
		}

		MuseumConfig config = configManager.get();
		String apiKey = config.apiKey;
		if (apiKey.isBlank()) {
			handleFailure(
					MuseumDataStatus.MISSING_API_KEY,
					"Set apiKey in config/museumdonationtooltip.json",
					Duration.ofMinutes(config.cacheRefreshMinutes)
			);
			refreshInFlight.set(false);
			return false;
		}

		if (!snapshot.get().hasUsableData()) {
			snapshot.set(MuseumSnapshot.empty(MuseumDataStatus.LOADING, "Requesting selected SkyBlock profile"));
		}

		apiClient.fetchProfiles(playerUuid, apiKey)
				.thenApply(response -> {
					try {
						return ProfileSelector.selectProfileId(response, playerUuid);
					} catch (HypixelApiException exception) {
						throw new CompletionException(exception);
					}
				})
				.thenCompose(profileId -> apiClient.fetchMuseum(profileId, apiKey)
						.thenApply(response -> {
							try {
								Set<String> donated = MuseumApiParser.parseDonatedKeys(response, playerUuid);
								return new RefreshResult(profileId, donated);
							} catch (HypixelApiException exception) {
								throw new CompletionException(exception);
							}
						}))
				.whenComplete((result, failure) -> {
					try {
						// Ignore a late response after disconnecting or switching accounts.
						if (!playerUuid.equals(activePlayer)) {
							nextRefreshAtMillis.set(0);
							return;
						}
						if (failure == null) {
							MuseumSnapshot updated = new MuseumSnapshot(
									result.donatedKeys(),
									result.profileId(),
									Instant.now(),
									MuseumDataStatus.READY,
									"Loaded " + result.donatedKeys().size() + " donated museum keys"
							);
							snapshot.set(updated);
							cacheManager.save(playerUuid, updated);
							scheduleAfter(Duration.ofMinutes(configManager.get().cacheRefreshMinutes));
						} else {
							handleThrowable(failure);
						}
					} finally {
						refreshInFlight.set(false);
					}
				});
		return true;
	}

	private void handleThrowable(Throwable failure) {
		Throwable cause = unwrap(failure);
		if (cause instanceof HypixelApiException apiFailure) {
			Duration retry = apiFailure.retryAfterSeconds() > 0
					? Duration.ofSeconds(apiFailure.retryAfterSeconds())
					: Duration.ofMinutes(configManager.get().cacheRefreshMinutes);
			handleFailure(apiFailure.status(), apiFailure.getMessage(), retry);
			return;
		}
		handleFailure(
				MuseumDataStatus.NETWORK_ERROR,
				cause.getClass().getSimpleName() + ": " + safeMessage(cause),
				Duration.ofMinutes(configManager.get().cacheRefreshMinutes)
		);
	}

	private void handleFailure(MuseumDataStatus status, String detail, Duration retryAfter) {
		MuseumSnapshot previous = snapshot.get();
		if (previous.hasUsableData()) {
			snapshot.set(new MuseumSnapshot(
					previous.donatedKeys(),
					previous.profileId(),
					previous.fetchedAt(),
					MuseumDataStatus.CACHED,
					status.displayText() + ": " + detail
			));
		} else {
			snapshot.set(MuseumSnapshot.empty(status, detail));
		}
		scheduleAfter(retryAfter);
		logger.warn("Museum refresh unavailable: {} ({})", status, detail);
	}

	private void scheduleAfter(Duration duration) {
		nextRefreshAtMillis.set(System.currentTimeMillis() + Math.max(1_000L, duration.toMillis()));
	}

	private static Throwable unwrap(Throwable throwable) {
		Throwable current = throwable;
		while ((current instanceof CompletionException || current instanceof java.util.concurrent.ExecutionException)
				&& current.getCause() != null) {
			current = current.getCause();
		}
		return current;
	}

	private static String safeMessage(Throwable throwable) {
		return throwable.getMessage() == null ? "no detail" : throwable.getMessage();
	}

	private record RefreshResult(String profileId, Set<String> donatedKeys) {
	}
}
