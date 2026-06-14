package io.github.museumdonationtooltip.model;

import java.time.Instant;
import java.util.Set;

/**
 * Immutable state read by the tooltip callback.
 */
public record MuseumSnapshot(
		Set<String> donatedKeys,
		String profileId,
		Instant fetchedAt,
		MuseumDataStatus status,
		String detail
) {
	public MuseumSnapshot {
		donatedKeys = Set.copyOf(donatedKeys);
		profileId = profileId == null ? "" : profileId;
		fetchedAt = fetchedAt == null ? Instant.EPOCH : fetchedAt;
		detail = detail == null ? "" : detail;
	}

	public static MuseumSnapshot empty(MuseumDataStatus status, String detail) {
		return new MuseumSnapshot(Set.of(), "", Instant.EPOCH, status, detail);
	}

	public boolean hasUsableData() {
		return status == MuseumDataStatus.READY || status == MuseumDataStatus.CACHED;
	}
}

