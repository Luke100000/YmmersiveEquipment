package net.conczin.equipment.utils;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Utils {
    public static <T> void setData(Ref<EntityStore> ref, CommandBuffer<EntityStore> commandBuffer, String field, Codec<T> codec, T data) {
        ItemStack itemInHand = InventoryComponent.getItemInHand(ref.getStore(), ref);
        if (itemInHand != null) {
            ItemStack newItemInHand = itemInHand.withMetadata(field, codec, data);
            InventoryComponent.Hotbar hotbar = commandBuffer.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
            if (hotbar != null) {
                hotbar.getInventory().replaceItemStackInSlot(hotbar.getActiveSlot(), itemInHand, newItemInHand);
            }
        }
    }

    public static <T> T getData(Ref<EntityStore> ref, String field, Codec<T> codec) {
        ItemStack itemInHand = InventoryComponent.getItemInHand(ref.getStore(), ref);
        if (itemInHand != null) {
            return itemInHand.getFromMetadataOrNull(field, codec);
        }
        return null;
    }
}
