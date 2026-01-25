package net.conczin.equipment.utils;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Utils {
    public static <T> void setData(Ref<EntityStore> ref, String field, BuilderCodec<T> codec, T data) {
        Inventory inventory = getInventory(ref);
        ItemStack itemInHand = inventory.getActiveHotbarItem();
        if (itemInHand != null) {
            ItemStack newItemInHand = itemInHand.withMetadata(field, codec, data);
            inventory.getHotbar().replaceItemStackInSlot(inventory.getActiveHotbarSlot(), itemInHand, newItemInHand);
        }
    }

    public static <T> T getData(Ref<EntityStore> ref, String field, BuilderCodec<T> codec) {
        Inventory inventory = getInventory(ref);
        ItemStack itemInHand = inventory.getActiveHotbarItem();
        if (itemInHand != null) {
            return itemInHand.getFromMetadataOrNull(field, codec);
        }
        return null;
    }

    public static Inventory getInventory(Ref<EntityStore> ref) {
        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        assert player != null;
        return player.getInventory();
    }
}
