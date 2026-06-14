package io.github.museumdonationtooltip.tooltip;

import io.github.museumdonationtooltip.config.ConfigManager;
import io.github.museumdonationtooltip.config.MuseumConfig;
import io.github.museumdonationtooltip.item.SkyBlockItemParser;
import io.github.museumdonationtooltip.model.TooltipState;
import io.github.museumdonationtooltip.registry.DonatableItemRegistry;
import io.github.museumdonationtooltip.service.MuseumDonationService;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Adds one informational line after vanilla/Hypixel tooltip text.
 */
public final class MuseumTooltipHandler {
	private final ConfigManager configManager;
	private final DonatableItemRegistry registry;
	private final MuseumDonationService donationService;
	private final SkyBlockItemParser itemParser = new SkyBlockItemParser();

	public MuseumTooltipHandler(
			ConfigManager configManager,
			DonatableItemRegistry registry,
			MuseumDonationService donationService
	) {
		this.configManager = configManager;
		this.registry = registry;
		this.donationService = donationService;
	}

	public void register() {
		// This callback performs no network or disk I/O; it only reads immutable cached state.
		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			MuseumConfig config = configManager.get();
			if (!config.enabled) {
				return;
			}

			itemParser.extractItemId(stack).ifPresent(itemId -> {
				TooltipState state;
				if (registry.isEmpty()) {
					state = TooltipState.UNKNOWN;
				} else {
					state = registry.find(itemId)
							.map(donationService::lookup)
							.orElse(TooltipState.NOT_DONATABLE);
				}
				if (state == TooltipState.UNKNOWN && !config.showUnknown) {
					return;
				}
				lines.add(Text.literal(state.text()).formatted(colorFor(state, config)));
			});
		});
	}

	private static Formatting colorFor(TooltipState state, MuseumConfig config) {
		String name = switch (state) {
			case DONATED -> config.donatedColor;
			case NOT_DONATED -> config.notDonatedColor;
			case NOT_DONATABLE -> config.notDonatableColor;
			case UNKNOWN -> config.unknownColor;
		};
		Formatting formatting = Formatting.byName(name);
		return formatting == null ? Formatting.WHITE : formatting;
	}
}

