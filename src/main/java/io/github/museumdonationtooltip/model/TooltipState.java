package io.github.museumdonationtooltip.model;

public enum TooltipState {
	DONATED("Museum: Donated"),
	NOT_DONATED("Museum: Not Donated"),
	NOT_DONATABLE("Museum: Not museum-donatable"),
	UNKNOWN("Museum: Unknown / API unavailable");

	private final String text;

	TooltipState(String text) {
		this.text = text;
	}

	public String text() {
		return text;
	}
}

