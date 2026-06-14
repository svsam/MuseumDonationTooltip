package io.github.museumdonationtooltip.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.museumdonationtooltip.item.ItemNormalizer;
import io.github.museumdonationtooltip.model.MuseumDataStatus;
import io.github.museumdonationtooltip.model.MuseumSnapshot;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;

/**
 * Persists successful museum reads so tooltip rendering survives restarts and outages.
 */
public final class MuseumCacheManager {
	private static final int SCHEMA_VERSION = 1;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path cacheDirectory;
	private final Logger logger;

	public MuseumCacheManager(Path configDirectory, Logger logger) {
		this.cacheDirectory = configDirectory.resolve("museumdonationtooltip-cache");
		this.logger = logger;
	}

	public Optional<MuseumSnapshot> load(UUID playerUuid) {
		Path path = pathFor(playerUuid);
		if (!Files.exists(path)) {
			return Optional.empty();
		}

		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			CacheFile cache = GSON.fromJson(reader, CacheFile.class);
			if (cache == null
					|| cache.schemaVersion != SCHEMA_VERSION
					|| !compactUuid(playerUuid).equals(compactUuid(cache.playerUuid))) {
				return Optional.empty();
			}
			Set<String> donated = new HashSet<>();
			if (cache.donatedItems != null) {
				for (String item : cache.donatedItems) {
					ItemNormalizer.normalize(item).ifPresent(donated::add);
				}
			}
			Instant fetchedAt = Instant.parse(cache.fetchedAt);
			return Optional.of(new MuseumSnapshot(
					donated,
					cache.profileId,
					fetchedAt,
					MuseumDataStatus.CACHED,
					"Loaded from disk cache"
			));
		} catch (IOException | RuntimeException exception) {
			logger.warn("Could not load museum cache {}", path, exception);
			return Optional.empty();
		}
	}

	public void save(UUID playerUuid, MuseumSnapshot snapshot) {
		if (snapshot.status() != MuseumDataStatus.READY) {
			return;
		}

		Path path = pathFor(playerUuid);
		Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
		CacheFile cache = new CacheFile();
		cache.schemaVersion = SCHEMA_VERSION;
		cache.playerUuid = playerUuid.toString();
		cache.profileId = snapshot.profileId();
		cache.fetchedAt = snapshot.fetchedAt().toString();
		cache.donatedItems = snapshot.donatedKeys();

		try {
			Files.createDirectories(cacheDirectory);
			try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
				GSON.toJson(cache, writer);
			}
			try {
				Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException unsupportedAtomicMove) {
				Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException exception) {
			logger.warn("Could not save museum cache {}", path, exception);
		}
	}

	private Path pathFor(UUID playerUuid) {
		return cacheDirectory.resolve(compactUuid(playerUuid) + ".json");
	}

	private static String compactUuid(UUID uuid) {
		return uuid.toString().replace("-", "").toLowerCase();
	}

	private static String compactUuid(String uuid) {
		return uuid == null ? "" : uuid.replace("-", "").toLowerCase();
	}

	private static final class CacheFile {
		int schemaVersion;
		String playerUuid;
		String profileId;
		String fetchedAt;
		Set<String> donatedItems;
	}
}
