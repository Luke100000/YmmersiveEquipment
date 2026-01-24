package net.conczin.equipment.interactions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.conczin.equipment.data.CameraStates;
import net.conczin.equipment.utils.ListCodec;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Manages zoom state on an item.
 */
public class UpdateCameraInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<UpdateCameraInteraction> CODEC = BuilderCodec.builder(
                    UpdateCameraInteraction.class, UpdateCameraInteraction::new, SimpleInstantInteraction.CODEC
            )
            .documentation("Manages zoom state for items like spyglasses.")
            .append(
                    new KeyedCodec<>("Action", new EnumCodec<>(ZoomAction.class)),
                    (o, v) -> o.action = v,
                    o -> o.action
            )
            .add()
            .append(
                    new KeyedCodec<>("Overlay", Codec.STRING),
                    (o, v) -> o.overlay = v,
                    o -> o.overlay
            )
            .add()
            .append(
                    new KeyedCodec<>("Sound", Codec.STRING),
                    (o, v) -> o.sound = v,
                    o -> o.sound
            )
            .add()
            .append(
                    new KeyedCodec<>("ZoomLevels", new ListCodec<>(Codec.INTEGER)),
                    (o, v) -> o.zoomLevels = v,
                    o -> o.zoomLevels
            )
            .add()
            .append(
                    new KeyedCodec<>("CameraMode", new EnumCodec<>(CameraStates.CameraMode.class)),
                    (o, v) -> o.cameraMode = v,
                    o -> o.cameraMode
            )
            .add()
            .build();

    public enum ZoomAction {
        Toggle,
        Enable,
        Disable,
        ZoomIn,
        ZoomOut
    }

    protected ZoomAction action = ZoomAction.Toggle;
    protected String overlay = null;
    protected String sound = "Spyglass";
    protected CameraStates.CameraMode cameraMode = CameraStates.CameraMode.MAP;
    protected List<Integer> zoomLevels = List.of(0);

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) return;

        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;

        ItemStack heldItem = context.getHeldItem();
        if (heldItem == null) return;

        CameraStates.CameraState state = CameraStates.getZoomState(playerRef.getUuid());
        state.zoomLevels = zoomLevels;
        state.overlay = overlay;
        state.mode = cameraMode;
        state.lastEquippedItem = heldItem.getItemId();

        switch (action) {
            case Toggle -> {
                state.active = !state.active;
                playSound(playerRef, state.active ? "Open" : "Close");
            }
            case Enable -> {
                state.active = true;
                playSound(playerRef, "Open");
            }
            case Disable -> {
                state.active = false;
                playSound(playerRef, "Close");
            }
            case ZoomIn -> {
                if (state.active) {
                    if (state.zoom + 1 < state.zoomLevels.size()) {
                        state.zoom = state.zoom + 1;
                        playSound(playerRef, "ZoomIn");
                    }
                } else {
                    state.active = true;
                    state.zoom = 0;
                    playSound(playerRef, "Open");
                }
            }
            case ZoomOut -> {
                if (state.zoom == 0) {
                    if (state.active) {
                        state.active = false;
                        playSound(playerRef, "Close");
                    }
                } else {
                    state.zoom = state.zoom - 1;
                    playSound(playerRef, "ZoomOut");
                }
            }
        }
    }

    private void playSound(PlayerRef playerRef, String soundType) {
        int soundEventIndex = SoundEvent.getAssetMap().getIndex("SFX_YmmersiveEquipment_" + sound + "_" + soundType);
        SoundUtil.playSoundEvent2dToPlayer(playerRef, soundEventIndex, SoundCategory.SFX);
    }
}
