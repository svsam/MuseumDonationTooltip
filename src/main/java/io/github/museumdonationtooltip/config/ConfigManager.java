package io.github.museumdonationtooltip.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;

/**
 * Loads, validates, and atomically saves the mod config.
 */
public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path configPath;
	private final Logger logger;
	private final AtomicReference<MuseumConfig> current = new AtomicReference<>(new MuseumConfig());

	public ConfigManager(Path configDirectory, Logger logger) {
		this.configPath = configDirectory.resolve("museumdonationtooltip.json");
		this.logger = logger;
	}

	public MuseumConfig get() {
		return current.get();
	}

	public synchronized MuseumConfig load() {
		MuseumConfig config = new MuseumConfig();
		if (Files.exists(configPath)) {
			try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
				MuseumConfig loaded = GSON.fromJson(reader, MuseumConfig.class);
				if (loaded != null) {
					config = loaded;
				}
			} catch (IOException | RuntimeException exception) {
				logger.warn("Could not read {}; using defaults", configPath, exception);
			}
		}

		config.validate();
		current.set(config);
		save(config);
		return config;
	}

	private void save(MuseumConfig config) {
		Path temporary = configPath.resolveSibling(configPath.getFileName() + ".tmp");
		try {
			Files.createDirectories(configPath.getParent());
			try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}
			try {
				Files.move(temporary, configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException unsupportedAtomicMove) {
				Files.move(temporary, configPath, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException exception) {
			logger.warn("Could not save {}", configPath, exception);
		}
	}
}

