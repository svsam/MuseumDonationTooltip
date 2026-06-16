# MuseumDonationTooltip - Made by svsam

MuseumDonationTooltip is a small client-side Fabric mod for Hypixel SkyBlock. When you hover over an item, it adds a line showing whether that item type has already been donated to your Museum.

You will see one of these messages:

- `Museum: Donated`
- `Museum: Not Donated`
- `Museum: Unknown / API unavailable`

The mod checks the general item type, not the exact copy in your inventory. For example, donating one Aspect of the End means every Aspect of the End you hover over will be shown as donated. Reforges, enchantments, stars, recombobulation, rarity upgrades, and item UUIDs do not change the result.

Items that cannot be donated to the Museum do not receive a Museum tooltip line.

## Installation

1. Install Fabric Loader and Fabric API for Minecraft 1.21.11.
2. Put the newest `museum-donation-tooltip-*.jar` in your Minecraft `mods` folder.
3. Start Minecraft once. This creates the mod's config file.
4. Place the API key in the config file.
5. Open:

   ```text
   .minecraft/config/museumdonationtooltip.json
   ```

6. Add the key between the quotes:

   ```json
   "apiKey": "your-api-key-here"
   ```

7. Save the file and run `/museumtooltip reloadconfig` in Minecraft.

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
  "unknownColor": "yellow"
}
```

The key is stored locally in this config because Hypixel requires it for Museum requests. It is never written to logs or the Museum cache.

## Commands

These commands run on your client and are not sent to Hypixel:

- `/museumtooltip refresh` refreshes your Museum data.
- `/museumtooltip status` shows the current API and cache status.
- `/museumtooltip reloadconfig` reloads the config file, then refreshes.

If the tooltip says `Unknown / API unavailable`, run `/museumtooltip status`. A `403` error normally means that Hypixel rejected the API key. Make sure you copied the API key itself, rather than the application ID or application URL. (Pls accept my application hypixel I'll update this when needed)

## How It Works

1. asks Hypixel for your SkyBlock profiles;
2. chooses the currently selected profile when possible;
3. requests that profile's Museum data;
4. turns the donated item IDs into a fast local lookup set;
5. saves the last successful response in a local cache.

If the API is temporarily unavailable, the mod keeps using the last successful cache. Missing or unexpected API fields are handled as an unknown state instead of crashing the game.

## Building From Source

On Windows:

```powershell
.\gradlew.bat build
```

The finished mod is:

```text
build/libs/museum-donation-tooltip-1.0.2.jar
```

The file ending in `-sources.jar` is source code for development and should not be installed as the mod.

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
