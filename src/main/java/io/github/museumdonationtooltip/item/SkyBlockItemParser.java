package io.github.museumdonationtooltip.item;

import java.util.Optional;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/**
 * Extracts Hypixel's stable ExtraAttributes.id from client-visible custom NBT.
 */
public final class SkyBlockItemParser {
	private static final String EXTRA_ATTRIBUTES = "ExtraAttributes";
	private static final String ITEM_ID = "id";

	public Optional<String> extractItemId(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return Optional.empty();
		}

		// Modern Minecraft stores legacy custom NBT in the minecraft:custom_data component.
		NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (customData == null) {
			return Optional.empty();
		}

		NbtCompound root = customData.copyNbt();
		NbtCompound attributes = root.getCompound(EXTRA_ATTRIBUTES).orElse(null);
		if (attributes == null) {
			attributes = root;
		}

		return attributes.getString(ITEM_ID).flatMap(ItemNormalizer::normalize);
	}
}

