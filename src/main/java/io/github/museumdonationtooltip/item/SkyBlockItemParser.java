package io.github.museumdonationtooltip.item;

import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

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
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData == null) {
			return Optional.empty();
		}

		CompoundTag root = customData.copyTag();
		CompoundTag attributes = root.getCompound(EXTRA_ATTRIBUTES).orElse(null);
		if (attributes == null) {
			attributes = root;
		}

		return attributes.getString(ITEM_ID).flatMap(ItemNormalizer::normalize);
	}
}
