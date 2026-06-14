package io.github.museumdonationtooltip.api;

import io.github.museumdonationtooltip.model.MuseumDataStatus;

public final class HypixelApiException extends Exception {
	private final MuseumDataStatus status;
	private final long retryAfterSeconds;

	public HypixelApiException(MuseumDataStatus status, String message) {
		this(status, message, 0);
	}

	public HypixelApiException(MuseumDataStatus status, String message, long retryAfterSeconds) {
		super(message);
		this.status = status;
		this.retryAfterSeconds = Math.max(0, retryAfterSeconds);
	}

	public MuseumDataStatus status() {
		return status;
	}

	public long retryAfterSeconds() {
		return retryAfterSeconds;
	}
}

