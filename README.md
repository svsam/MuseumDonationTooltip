# MuseumDonationTooltip

MuseumDonationTooltip is a small client-side Fabric mod for Hypixel SkyBlock. When you hover over an item, it adds a line showing whether that item type has already been donated to your Museum.

You will see one of these messages:

- `Museum: Donated`
- `Museum: Not Donated`
- `Museum: Not museum-donatable`
- `Museum: Unknown / API unavailable`

The mod checks the general item type, not the exact copy in your inventory. For example, donating one Aspect of the End means every Aspect of the End you hover over will be shown as donated. Reforges, enchantments, stars, recombobulation, rarity upgrades, and item UUIDs do not change the result.

## What You Need

- Minecraft Java Edition 1.21.11
- Fabric Loader 0.19.3 or newer
- Fabric API for Minecraft 1.21.11
- Java 21
- A Hypixel API key

## Installation

1. Install Fabric Loader and Fabric API for Minecraft 1.21.11.
2. Put `museum-donation-tooltip-1.0.0.jar` in your Minecraft `mods` folder.
3. Start Minecraft once. This creates the mod's config file.
4. Create an application on the [Hypixel Developer Dashboard](https://developer.hypixel.net/).
5. Copy the application's API key.
6. Open:

   ```text
   .minecraft/config/museumdonationtooltip.json
   ```

7. Add the key between the quotes:

   ```json
   "apiKey": "your-api-key-here"
   ```

8. Save the file and run `/museumtooltip reloadconfig` in Minecraft.

Do not put your API key in `MuseumConfig.java`, upload it to GitHub, or share it with anyone.

## Configuration

The full default config looks like this:

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

- `enabled` turns all Museum tooltip lines on or off.
- `showUnknown` controls whether API errors are shown in tooltips.
- `cacheRefreshMinutes` controls how often the mod refreshes your Museum data. Values are limited to between 5 and 1,440 minutes.
- The four color options accept normal Minecraft formatting color names.

The key is stored locally in this config because Hypixel requires it for Museum requests. It is never written to logs or the Museum cache.

## Commands

These commands run on your client and are not sent to Hypixel:

- `/museumtooltip refresh` refreshes your Museum data.
- `/museumtooltip status` shows the current API and cache status.
- `/museumtooltip reloadconfig` reloads the config file, then refreshes.

If the tooltip says `Unknown / API unavailable`, run `/museumtooltip status`. A `403` error normally means that Hypixel rejected the API key. Make sure you copied the API key itself, rather than the application ID or application URL.

## How It Works

The mod reads Hypixel's internal item ID from the data already attached to the item. This is more reliable than using its displayed name, which may contain colors, reforges, stars, or other upgrades.

In the background, the mod:

1. asks Hypixel for your SkyBlock profiles;
2. chooses the currently selected profile when possible;
3. requests that profile's Museum data;
4. turns the donated item IDs into a fast local lookup set;
5. saves the last successful response in a local cache.

Hovering over an item does not make a network request. The tooltip only checks information that has already been loaded into memory, so opening inventories or moving the mouse across items does not repeatedly contact Hypixel.

If the API is temporarily unavailable, the mod keeps using the last successful cache. Missing or unexpected API fields are handled as an unknown state instead of crashing the game.

## Building From Source

On Windows:

```powershell
.\gradlew.bat build
```

On Linux or macOS:

```sh
./gradlew build
```

The finished mod is:

```text
build/libs/museum-donation-tooltip-1.0.0.jar
```

The file ending in `-sources.jar` is source code for development and should not be installed as the mod.

## Donatable Item List

The bundled `museum-donatable-items.json` file is generated from Hypixel's public SkyBlock item resource. It contains the known Museum items, armor-set donation keys, and alternate IDs such as starred Dungeon variants.

To download the latest item data and rebuild the list:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\update-donatable-registry.ps1
```

The current checked-in list contains 1,040 donatable entries and was generated from Hypixel item data last updated on June 9, 2026.

## Project Structure

- `tooltip/` adds the tooltip line through Fabric.
- `item/` reads and normalizes SkyBlock item IDs.
- `api/` handles Hypixel requests and response parsing.
- `cache/` saves the last successful Museum response.
- `service/` manages refreshes and donated-item lookups.
- `registry/` loads the list of Museum-donatable items.
- `config/` loads and validates user settings.
- `model/` contains the tooltip and API status types.

## Safety

This is an informational mod. It does not click menus, move items, automate gameplay, alter combat or movement, or send custom gameplay packets. It only reads information visible to the Minecraft client and requests data from Hypixel's documented public API.

Hypixel considers all modifications use-at-your-own-risk. Check the current rules before using the mod, and only download releases from the project's official source.

## Documentation Used

- [Hypixel Allowed Modifications](https://support.hypixel.net/hc/en-us/articles/6472550754962-Hypixel-Allowed-Modifications)
- [Hypixel SkyBlock Rules](https://support.hypixel.net/hc/en-us/articles/4508088842898-Hypixel-SkyBlock-Rules)
- [Hypixel Public API v2](https://api.hypixel.net/)
- [Hypixel Developer Dashboard](https://developer.hypixel.net/)
- [Official Hypixel SkyBlock Museum Wiki](https://wiki.hypixel.net/Museum)
- [Fabric 1.21.11 documentation](https://docs.fabricmc.net/1.21.11/)
- [Fabric API 0.141.4 Javadocs](https://maven.fabricmc.net/docs/fabric-api-0.141.4+1.21.11/)
- [Yarn 1.21.11 build 6 Javadocs](https://maven.fabricmc.net/docs/yarn-1.21.11+build.6/)
- [Fabric example mod for 1.21.11](https://github.com/FabricMC/fabric-example-mod/tree/1.21.11)
- [MinecraftForge item documentation](https://docs.minecraftforge.net/en/latest/items/)
- [Gson User Guide](https://github.com/google/gson/blob/main/UserGuide.md)
- [Java 21 HttpClient](https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/HttpClient.html)
- [Gradle documentation](https://docs.gradle.org/current/userguide/userguide.html)

## License

MIT
