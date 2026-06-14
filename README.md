# MuseumDonationTooltip

MuseumDonationTooltip is a client-only Fabric mod for Minecraft 1.21.11. It adds one line to Hypixel SkyBlock item tooltips:

- `Museum: Donated`
- `Museum: Not Donated`
- `Museum: Not museum-donatable`
- `Museum: Unknown / API unavailable`

The result is based on the SkyBlock item type, not the physical item instance. Enchantments, reforges, stars, recombobulation, rarity changes, durability, UUIDs, and similar mutable metadata do not affect the lookup.

## Architecture

Tooltip rendering is deliberately kept fast and local:

1. `SkyBlockItemParser` reads `ExtraAttributes.id` from the hovered stack's `minecraft:custom_data` component.
2. `ItemNormalizer` produces a stable uppercase SkyBlock key.
3. `DonatableItemRegistry` resolves the key against the bundled Hypixel item snapshot, including starred aliases and armor-set donation keys.
4. `MuseumDonationService` checks an immutable in-memory `HashSet` of donated keys.
5. `MuseumTooltipHandler` appends the configured line through Fabric's `ItemTooltipCallback`.

Network calls never happen in the tooltip callback. In the background, the service:

- requests `/v2/skyblock/profiles?uuid=...`;
- selects the profile marked `selected`, with a latest-save fallback;
- requests `/v2/skyblock/museum?profile=...`;
- parses documented and historically observed response nesting defensively;
- caches successful data under `config/museumdonationtooltip-cache/`;
- keeps using the last good cache during temporary API failures.

The parser treats missing, null, renamed, wrongly typed, or unexpectedly nested fields as unavailable data rather than crashing Minecraft.

## Requirements

- Minecraft Java Edition 1.21.11
- Fabric Loader 0.19.3 or newer
- Fabric API for Minecraft 1.21.11
- Java 21
- A Hypixel Developer Dashboard API key

## Build

On Windows:

```powershell
.\gradlew.bat build
```

On Linux or macOS:

```sh
./gradlew build
```

The remapped mod JAR is written to `build/libs/`.

## Install And Configure

1. Install Fabric Loader and Fabric API for Minecraft 1.21.11.
2. Put the built JAR in the Minecraft `mods` directory.
3. Start Minecraft once so the config file is created.
4. Create an application and API key in the [Hypixel Developer Dashboard](https://developer.hypixel.net/).
5. Edit `config/museumdonationtooltip.json` and set `apiKey`.
6. Run `/museumtooltip reloadconfig` in game.

Default config:

```json
{
  "apiKey": "",
  "enabled": true,
  "showUnknown": true,
  "cacheRefreshMinutes": 15,
  "donatedColor": "green",
  "notDonatedColor": "red",
  "notDonatableColor": "dark_gray",
  "unknownColor": "yellow"
}
```

The refresh interval is clamped to 5-1,440 minutes. API keys are stored locally in plain text because Hypixel requires the `API-Key` request header. The mod never logs the key or writes it to the museum cache.

Available local commands:

- `/museumtooltip refresh` requests a background refresh.
- `/museumtooltip status` shows cache/API state and the selected profile ID.
- `/museumtooltip reloadconfig` reloads settings and refreshes.

These commands are registered as Fabric client commands and are not sent to Hypixel.

## API And Failure Handling

| Condition | Behavior |
| --- | --- |
| `403` | Marks the key forbidden; uses a previous valid cache if available. |
| `400` or `422` | Marks the request invalid; does not crash. |
| `404` or no profiles | Reports that no profile was found. |
| `429` | Honors `RateLimit-Reset` or `Retry-After` before retrying. |
| Private/null profile or museum data | Reports unavailable/private data. |
| Network failure | Retains the last successful cache when possible. |
| Malformed JSON or fields | Reports a malformed response and fails safely. |

The official API documentation's example museum shape and UUID-keyed/nested member variants are covered by unit tests. An authenticated live museum request is not included in automated tests because the project must not contain or depend on a developer's private API key.

## Donatable Item Data

`src/main/resources/museum-donatable-items.json` is generated from Hypixel's public `/v2/resources/skyblock/items` endpoint. The checked-in snapshot contains 1,040 items from the resource last updated on June 9, 2026.

To update it:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\update-donatable-registry.ps1
```

The generator includes every item with `museum_data`, its museum category, `mapped_item_ids` aliases, and armor-set donation keys. Updating the data does not require changes to tooltip or API logic.

## Source Layout

- `MuseumDonationTooltipClient`: initialization, client events, local commands, and compliance notes.
- `tooltip/`: Fabric tooltip event handling and color formatting.
- `item/`: SkyBlock NBT identity extraction and normalization.
- `api/`: HTTP requests, selected-profile logic, and defensive museum parsing.
- `cache/`: per-player atomic JSON cache loading and saving.
- `service/`: refresh scheduling, status transitions, and donated-key lookups.
- `registry/`: bundled museum-donatable item and alias registry.
- `config/`: validated user settings.
- `model/`: tooltip and API/cache state models.

## Safety And Compliance

This mod is informational only. It does not:

- automate gameplay or clicks;
- move or modify inventory items;
- alter movement or combat;
- send custom or gameplay packets;
- change Minecraft's communication with Hypixel;
- read information unavailable to the normal client.

It only reads client-visible item data and calls documented public Hypixel API endpoints. Hypixel states that modifications are used at the player's own risk. Download this mod only from its official source/release page and review current Hypixel rules before use.

## Documentation Used

- [Hypixel Allowed Modifications](https://support.hypixel.net/hc/en-us/articles/6472550754962-Hypixel-Allowed-Modifications)
- [Hypixel SkyBlock Rules](https://support.hypixel.net/hc/en-us/articles/4508088842898-Hypixel-SkyBlock-Rules)
- [Hypixel Public API v2](https://api.hypixel.net/), especially profiles, museum, item resources, authentication, response notes, and rate-limit headers
- [Hypixel Developer Dashboard](https://developer.hypixel.net/)
- [Official Hypixel SkyBlock Museum Wiki](https://wiki.hypixel.net/Museum)
- [Fabric 1.21.11 documentation](https://docs.fabricmc.net/1.21.11/)
- [Fabric API 0.141.4 Javadocs](https://maven.fabricmc.net/docs/fabric-api-0.141.4+1.21.11/)
- [Yarn 1.21.11 build 6 Javadocs](https://maven.fabricmc.net/docs/yarn-1.21.11+build.6/)
- [Fabric example mod, 1.21.11 branch](https://github.com/FabricMC/fabric-example-mod/tree/1.21.11)
- [MinecraftForge item documentation](https://docs.minecraftforge.net/en/latest/items/), used only to compare cross-loader tooltip terminology; the implementation is Fabric
- [Gson User Guide](https://github.com/google/gson/blob/main/UserGuide.md)
- [Java 21 HttpClient](https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/HttpClient.html)
- [Gradle documentation](https://docs.gradle.org/current/userguide/userguide.html)

## License

MIT
