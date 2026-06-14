package io.github.museumdonationtooltip;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import io.github.museumdonationtooltip.api.HypixelApiClient;
import io.github.museumdonationtooltip.cache.MuseumCacheManager;
import io.github.museumdonationtooltip.config.ConfigManager;
import io.github.museumdonationtooltip.model.MuseumSnapshot;
import io.github.museumdonationtooltip.registry.DonatableItemRegistry;
import io.github.museumdonationtooltip.service.MuseumDonationService;
import io.github.museumdonationtooltip.tooltip.MuseumTooltipHandler;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-only entrypoint for MuseumDonationTooltip.
 *
 * <p>Safety and compliance: this mod is informational only. It never clicks menus,
 * moves items, changes movement or combat, automates gameplay, or sends gameplay
 * actions/custom packets to Hypixel. It only reads data already visible to the client
 * and makes documented public API requests. Hypixel modifications are use-at-your-own-risk;
 * users should install releases only from this project's official release source.</p>
 *
 * <p>Documentation Used: Hypixel Allowed Modifications, SkyBlock Rules, Public API
 * (profiles, museum, items), the official SkyBlock Museum wiki, Fabric 1.21.11
 * documentation/Javadocs, MinecraftForge tooltip documentation for cross-loader
 * terminology comparison, Gson, Java HttpClient, and Gradle documentation.</p>
 */
public final class MuseumDonationTooltipClient implements ClientModInitializer {
	public static final String MOD_ID = "museumdonationtooltip";
	private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter
			.ofPattern("uuuu-MM-dd HH:mm:ss z")
			.withZone(ZoneId.systemDefault());

	private ConfigManager configManager;
	private MuseumDonationService donationService;
	private int tickCounter;

	@Override
	public void onInitializeClient() {
		// Mod initialization: load local state before registering any client events.
		configManager = new ConfigManager(FabricLoader.getInstance().getConfigDir(), LOGGER);
		configManager.load();

		DonatableItemRegistry registry;
		try {
			registry = DonatableItemRegistry.loadBundled();
			LOGGER.info("Loaded {} museum item lookup IDs", registry.lookupIdCount());
		} catch (IOException exception) {
			// A missing registry must not crash Minecraft; tooltips become Unknown instead.
			LOGGER.error("Could not load the museum-donatable item registry", exception);
			registry = DonatableItemRegistry.empty();
		}

		MuseumCacheManager cacheManager = new MuseumCacheManager(
				FabricLoader.getInstance().getConfigDir(),
				LOGGER
		);
		donationService = new MuseumDonationService(
				configManager,
				cacheManager,
				new HypixelApiClient(),
				LOGGER
		);

		new MuseumTooltipHandler(configManager, registry, donationService).register();
		registerTickRefresh();
		registerClientCommands();
		LOGGER.info("MuseumDonationTooltip initialized as a client-only informational mod");
	}

	private void registerTickRefresh() {
		// Timed refresh/profile detection runs outside tooltip rendering.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (++tickCounter < 20) {
				return;
			}
			tickCounter = 0;
			if (client.player == null) {
				donationService.disconnect();
				return;
			}
			donationService.tick(client.player.getUuid());
		});
	}

	private void registerClientCommands() {
		// Client commands are handled locally and are never sent to Hypixel.
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(literal("museumtooltip")
						.then(literal("refresh").executes(context -> {
							UUID playerUuid = currentPlayerUuid();
							if (playerUuid == null) {
								context.getSource().sendError(Text.literal("Join a world before refreshing."));
								return 0;
							}
							boolean started = donationService.forceRefresh(playerUuid);
							context.getSource().sendFeedback(Text.literal(
									started ? "Museum refresh started." : "Museum refresh is already running or needs an API key."
							));
							return started ? 1 : 0;
						}))
						.then(literal("reloadconfig").executes(context -> {
							configManager.load();
							UUID playerUuid = currentPlayerUuid();
							if (playerUuid != null) {
								donationService.forceRefresh(playerUuid);
							}
							context.getSource().sendFeedback(Text.literal("MuseumDonationTooltip config reloaded."));
							return 1;
						}))
						.then(literal("status").executes(context -> {
							MuseumSnapshot snapshot = donationService.snapshot();
							String fetched = snapshot.fetchedAt().equals(java.time.Instant.EPOCH)
									? "never"
									: TIME_FORMAT.format(snapshot.fetchedAt());
							context.getSource().sendFeedback(Text.literal(
									snapshot.status().displayText()
											+ " | profile=" + emptyAsUnknown(snapshot.profileId())
											+ " | donatedKeys=" + snapshot.donatedKeys().size()
											+ " | fetched=" + fetched
											+ (snapshot.detail().isBlank() ? "" : " | " + snapshot.detail())
							));
							return 1;
						}))
				));
	}

	private static UUID currentPlayerUuid() {
		MinecraftClient client = MinecraftClient.getInstance();
		return client.player == null ? null : client.player.getUuid();
	}

	private static String emptyAsUnknown(String value) {
		return value == null || value.isBlank() ? "unknown" : value;
	}
}
