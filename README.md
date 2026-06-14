# MuseumDonationTooltip

Create a Minecraft Fabric 1.21.11 client-side mod for Hypixel SkyBlock called “MuseumDonationTooltip”.

The mod should check whether a Hypixel SkyBlock item type has already been donated to the player’s Museum, then add a line to the item’s tooltip when the player hovers over that item.

Important: this must be a purely client-side informational mod. It must not automate gameplay, click menus, move items, send custom packets, alter player movement, alter combat, alter inventory behaviour, or change how the Minecraft client communicates with Hypixel. It should only read client-visible item data and request public Hypixel API data.

Core feature:

When the player hovers over a SkyBlock item, append a tooltip line at the end of the item description saying one of the following:

“Museum: Donated”
“Museum: Not Donated”
“Museum: Not museum-donatable”
“Museum: Unknown / API unavailable”

The check should be based on the item type in general, not the specific physical item. For example, if the player has donated one Mithril Pickaxe to the Museum, then any other Mithril Pickaxe the player later obtains should show “Museum: Donated” when hovered over.

Functional requirements:

1. Use Minecraft fabric 1.21.11.

2. Use Fabric's tooltip event system to modify item tooltip text when the player hovers over an item.

3. Read the hovered item’s SkyBlock item identity from its NBT data where possible. Prefer a stable internal item identifier rather than the display name, because item names can include colours, reforges, stars, recombobulation, enchantments, skins, or other formatting.

4. Create a normalisation system that converts the hovered item into a comparable museum item key. The system should ignore properties that do not change the general item type, such as:

   * enchantments
   * reforges
   * hot potato books
   * stars
   * recombobulation
   * item rarity changes
   * durability or usage state
   * individual UUID-like metadata

5. Use the Hypixel Public API endpoint for SkyBlock museum data:

   * Use the museum endpoint by profile ID.
   * Use the player’s selected SkyBlock profile if possible.
   * Respect API rate limits.
   * Cache API responses locally so the mod does not request the API every time a tooltip is rendered.
   * Provide clear handling for 403, 422, 429, failed network requests, private API data, missing profile data, and malformed responses.

6. Build and maintain a donated-item set:

   * Fetch the museum data from the Hypixel API.
   * Parse all donated item entries for the selected profile/member.
   * Store the donated item types in a HashSet or equivalent fast lookup structure.
   * Check the normalised hovered item key against this set.
   * The result should be item-type based, not instance based.

7. Build and maintain a museum-donatable-item set:

   * Include a way to know which SkyBlock items can be donated to the Museum.
   * Prefer a maintainable data file such as a JSON file bundled with the mod.
   * The JSON should list known museum-donatable item IDs and optionally their category, such as Weapons, Armor Sets, Rarities, or Special Items.
   * Design the code so this data file can be updated without rewriting the main logic.
   * If an item is not in the donatable list, show “Museum: Not museum-donatable” rather than “Not Donated”.

8. Add a config system:

   * API key input, if required by the current Hypixel API system.
   * Toggle for enabling/disabling tooltip lines.
   * Toggle for showing unknown/API errors.
   * Cache refresh interval.
   * Optional colour formatting for donated, not donated, not donatable, and unknown states.

9. Add safety and compliance comments:

   * Explain in comments that the mod is informational only.
   * Explain that it does not automate actions.
   * Explain that it does not send gameplay actions to Hypixel.
   * Explain that Hypixel mods are use-at-your-own-risk.
   * Explain that users should only download the mod from its official release source.

10. Add code comments throughout the program explaining the logic:

* Mod initialisation.
* Event registration.
* Tooltip event handling.
* SkyBlock item ID extraction from NBT.
* Item normalisation.
* API request logic.
* API response parsing.
* Cache loading/saving.
* Donated-item lookup.
* Error handling.
* Config handling.

11. Include a “Documentation Used” section in the project comments. Use these documentation sources:

* Hypixel Allowed Modifications documentation.
* Hypixel SkyBlock Rules.
* Hypixel Public API documentation, especially the SkyBlock profile and museum endpoints.
* MinecraftForge documentation.
* Fabric 1.21.11 JavaDocs, especially client events and tooltip-related events.
* Official Hypixel SkyBlock Wiki page for the Museum.
* Any additional documentation used for JSON parsing, HTTP requests, Gradle, or Minecraft NBT handling.

Important API-handling requirement:

Do not assume the exact Hypixel API JSON structure blindly. Before implementing the parser, inspect the current response format from the Hypixel Public API documentation and, where possible, test with a real example response.

The parser should be defensive and flexible:

* Check whether each expected field exists before reading it.
* Handle missing, null, renamed, or nested fields safely.
* Avoid crashing if the API response structure changes.
* Log or display a clear “Unknown / API unavailable” state when the museum data cannot be parsed.
* Keep the API parsing logic separate from the tooltip logic, so the parser can be updated later without rewriting the rest of the mod.
* Include comments explaining which fields are expected from the API and what fallback behaviour is used if those fields are missing.

Implementation expectations:

The final project should be organised cleanly into separate classes/modules, for example:

* Main mod class
* Tooltip event handler
* SkyBlock item parser
* Item normaliser
* Museum API client
* Museum cache manager
* Museum donation service
* Donatable item registry
* Config manager
* Error/status model

The program should avoid doing network calls directly inside the tooltip event. Tooltip rendering happens frequently, so the tooltip event should only perform fast local lookups against cached data. API fetching should happen on startup, profile change, manual refresh, or timed refresh.

The mod should fail safely. If the API is unavailable, the tooltip should not crash the game. It should show “Museum: Unknown / API unavailable” or hide the museum line depending on config.

Before giving the final answer, explain the architecture first, then provide the commented source files, then provide the README.md, then list all documentation used.
