package io.github.museumdonationtooltip.registry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.museumdonationtooltip.item.ItemNormalizer;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Loads the maintainable bundled museum item registry.
 */
public final class DonatableItemRegistry {
	public static final String RESOURCE_PATH = "/museum-donatable-items.json";

	private final Map<String, DonatableItem> itemsById;

	private DonatableItemRegistry(Map<String, DonatableItem> itemsById) {
		this.itemsById = Collections.unmodifiableMap(new HashMap<>(itemsById));
	}

	public static DonatableItemRegistry empty() {
		return new DonatableItemRegistry(Map.of());
	}

	public static DonatableItemRegistry loadBundled() throws IOException {
		InputStream stream = DonatableItemRegistry.class.getResourceAsStream(RESOURCE_PATH);
		if (stream == null) {
			throw new IOException("Missing bundled registry " + RESOURCE_PATH);
		}
		try (stream) {
			return load(stream);
		}
	}

	public static DonatableItemRegistry load(InputStream stream) throws IOException {
		JsonObject root;
		try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
			root = new Gson().fromJson(reader, JsonObject.class);
		} catch (RuntimeException exception) {
			throw new IOException("Malformed museum item registry", exception);
		}

		if (root == null || !root.has("items") || !root.get("items").isJsonArray()) {
			throw new IOException("Museum item registry has no items array");
		}

		Map<String, DonatableItem> result = new HashMap<>();
		for (JsonElement element : root.getAsJsonArray("items")) {
			if (!element.isJsonObject()) {
				continue;
			}
			JsonObject item = element.getAsJsonObject();
			Optional<String> normalizedId = string(item, "id").flatMap(ItemNormalizer::normalize);
			if (normalizedId.isEmpty()) {
				continue;
			}

			String id = normalizedId.get();
			String category = string(item, "category").orElse("UNKNOWN");
			Set<String> museumKeys = normalizedSet(item.getAsJsonArray("museumKeys"));
			museumKeys.add(id);
			DonatableItem entry = new DonatableItem(id, category, museumKeys);
			result.put(id, entry);

			JsonArray aliases = item.getAsJsonArray("aliases");
			if (aliases != null) {
				for (String alias : normalizedSet(aliases)) {
					result.put(alias, entry);
				}
			}
		}
		return new DonatableItemRegistry(result);
	}

	public Optional<DonatableItem> find(String itemId) {
		return ItemNormalizer.normalize(itemId).map(itemsById::get);
	}

	public int lookupIdCount() {
		return itemsById.size();
	}

	public boolean isEmpty() {
		return itemsById.isEmpty();
	}

	private static Set<String> normalizedSet(JsonArray values) {
		Set<String> result = new HashSet<>();
		if (values == null) {
			return result;
		}
		for (JsonElement value : values) {
			if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
				ItemNormalizer.normalize(value.getAsString()).ifPresent(result::add);
			}
		}
		return result;
	}

	private static Optional<String> string(JsonObject object, String name) {
		JsonElement value = object.get(name);
		if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
			return Optional.empty();
		}
		return Optional.of(value.getAsString());
	}
}
