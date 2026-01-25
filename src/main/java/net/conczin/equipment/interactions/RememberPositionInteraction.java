package net.conczin.equipment.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.conczin.equipment.utils.Utils;

import javax.annotation.Nonnull;

/**
 * Remembers the player's current position and switches the item to the "charged" state.
 */
public class RememberPositionInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<RememberPositionInteraction> CODEC = BuilderCodec.builder(
                    RememberPositionInteraction.class, RememberPositionInteraction::new, SimpleInstantInteraction.CODEC
            )
            .documentation("Remembers the player's current position and changes item state.")
            .build();

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();

        if (commandBuffer == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        // Get a player's current position
        TransformComponent transform = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        // Store the position in the item's metadata
        Vector3d position = transform.getPosition().clone();
        Utils.setData(ref, "YmmersiveEquipmentPosition", Vector3d.CODEC, position);

        // Play avatar powers enable sound
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null) {
            int soundEventIndex = SoundEvent.getAssetMap().getIndex("SFX_Avatar_Powers_Enable_Local");
            SoundUtil.playSoundEvent2dToPlayer(playerRef, soundEventIndex, SoundCategory.SFX);
        }

        // Change item state to "charged"
        Inventory inventory = Utils.getInventory(ref);
        ItemStack itemInHand = inventory.getActiveHotbarItem();
        if (itemInHand != null) {
            ItemStack newItemInHand = itemInHand.withState("Charged");
            inventory.getHotbar().replaceItemStackInSlot(inventory.getActiveHotbarSlot(), itemInHand, newItemInHand);
        }
    }
}
