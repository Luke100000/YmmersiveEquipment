package net.conczin.equipment.interactions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;

import javax.annotation.Nonnull;

public class UpdateCameraInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<UpdateCameraInteraction> CODEC = BuilderCodec.builder(
                    UpdateCameraInteraction.class, UpdateCameraInteraction::new, SimpleInstantInteraction.CODEC
            )
            .documentation("Replaces the held item with a configured item.")
            .append(
                    new KeyedCodec<>("CameraType", new EnumCodec<>(CameraType.class)),
                    (o, v) -> o.cameraType = v,
                    o -> o.cameraType
            )
            .add()
            .append(
                    new KeyedCodec<>("Distance", Codec.FLOAT),
                    (o, distance) -> o.distance = distance,
                    o -> o.distance
            )
            .add()
            .build();

    enum CameraType {
        DEFAULT,
        MAP,
        SPYGLASS
    }

    CameraType cameraType = CameraType.DEFAULT;
    float distance = 20.0f;

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) return;
        PlayerRef player = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (player == null) return;

        if (cameraType == CameraType.DEFAULT) {
            player.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, false, null));
        } else {
            ServerCameraSettings s = new ServerCameraSettings();
            s.sendMouseMotion = true;
            s.displayReticle = false;

            if (distance > 0) {
                s.distance = distance;
                s.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffsetRaycast;
            } else {
                s.distance = -raytrace(commandBuffer, ref, -distance);
                s.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffset;
            }

            if (cameraType == CameraType.MAP) {
                s.isFirstPerson = false;
                s.eyeOffset = false;
                s.positionLerpSpeed = 0.2f;
                s.rotationLerpSpeed = 0.25f;
                s.speedModifier = 1.0f;
                s.positionOffset = new Position(0, distance * 0.1, 0);
            } else if (cameraType == CameraType.SPYGLASS) {
                s.isFirstPerson = true;
                s.eyeOffset = true;
                s.positionLerpSpeed = 0.2f;
                s.rotationLerpSpeed = 0.1f;
                s.speedModifier = 0.5f;
            }

            player.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, true, s));
        }
    }

    private float raytrace(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref, float targetDistance) {
        Transform transform = TargetUtil.getLook(ref, commandBuffer);
        Vector3d position = transform.getPosition();
        Vector3d direction = transform.getDirection();
        World world = commandBuffer.getExternalData().getWorld();
        Vector3i hitBlock = TargetUtil.getTargetBlock(
                world,
                (blockId, _) -> {
                    if (blockId == 0) return false;
                    BlockType blocktype = BlockType.getAssetMap().getAsset(blockId);
                    return blocktype != null && blocktype.getMaterial() != BlockMaterial.Empty;
                },
                position.x, position.y, position.z,
                direction.x, direction.y, direction.z,
                targetDistance
        );
        if (hitBlock == null) {
            return targetDistance;
        } else {
            double dx = hitBlock.x + 0.5 - position.x;
            double dy = hitBlock.y + 0.5 - position.y;
            double dz = hitBlock.z + 0.5 - position.z;
            float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            return Math.clamp(distance - 3.0f, 1.0F, targetDistance);
        }
    }
}

