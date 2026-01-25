package net.conczin.equipment.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.conczin.equipment.utils.Utils;

import javax.annotation.Nonnull;

/**
 * Teleports the player to the remembered position.
 */
public class TeleportInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<TeleportInteraction> CODEC = BuilderCodec.builder(
                    TeleportInteraction.class, TeleportInteraction::new, SimpleInstantInteraction.CODEC
            )
            .documentation("Teleports the player to the remembered position and changes item state back to default.")
            .build();

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        // Get the remembered position from item metadata
        Vector3d rememberedPosition = Utils.getData(ref, "YmmersiveEquipmentPosition", Vector3d.CODEC);
        if (rememberedPosition == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        // Get the current rotation to preserve it
        TransformComponent transform = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        // Create and add a teleport component to perform the teleportation
        Vector3f currentRotation = transform.getRotation().clone();
        Teleport teleport = Teleport.createForPlayer(rememberedPosition, currentRotation);
        commandBuffer.addComponent(ref, Teleport.getComponentType(), teleport);

        // Play teleport sound
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null) {
            int soundEventIndex = SoundEvent.getAssetMap().getIndex("SFX_Portal_Neutral_Teleport_Local");
            SoundUtil.playSoundEvent2dToPlayer(playerRef, soundEventIndex, SoundCategory.SFX);
        }

        // Change item state back to default (remove state)
        Inventory inventory = Utils.getInventory(ref);
        ItemStack itemInHand = inventory.getActiveHotbarItem();
        if (itemInHand != null) {
            ItemStack newItemInHand = withDefaultState(itemInHand);
            inventory.getHotbar().replaceItemStackInSlot(inventory.getActiveHotbarSlot(), itemInHand, newItemInHand);
        }
    }

    public ItemStack withDefaultState(ItemStack itemStack) {
        Item item = itemStack.getItem();
        String defaultState = item.getId().substring(1, item.getId().lastIndexOf("_State_"));
        //noinspection deprecation
        return new ItemStack(defaultState, itemStack.getQuantity(), itemStack.getDurability(), itemStack.getMaxDurability(), itemStack.getMetadata());
    }
}
