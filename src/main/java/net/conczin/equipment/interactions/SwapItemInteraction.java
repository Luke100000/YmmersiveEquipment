package net.conczin.equipment.interactions;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;

import javax.annotation.Nonnull;

/**
 * Interaction that replaces the currently held item with a new ItemStack.
 * This allows state changes currently not supported or synced.
 */
public class SwapItemInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<SwapItemInteraction> CODEC = BuilderCodec.builder(
                    SwapItemInteraction.class, SwapItemInteraction::new, SimpleInstantInteraction.CODEC
            )
            .documentation("Replaces the held item with a configured item.")
            .appendInherited(
                    new KeyedCodec<>("Item", ItemStack.CODEC),
                    (o, v) -> o.item = v,
                    o -> o.item,
                    (o, p) -> o.item = p.item
            )
            .add()
            .build();

    protected ItemStack item;

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        ItemStack itemInHand = context.getHeldItem();
        ItemContainer container = context.getHeldItemContainer();
        if (container == null) return;

        ItemStack newItem = item != null ? item : ItemStack.EMPTY;

        container.replaceItemStackInSlot(context.getHeldItemSlot(), itemInHand, newItem);
    }
}

