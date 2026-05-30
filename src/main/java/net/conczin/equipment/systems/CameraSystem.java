package net.conczin.equipment.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import net.conczin.equipment.data.CameraStates;
import net.conczin.equipment.ui.OverlayHud;
import org.joml.Vector3d;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class CameraSystem extends EntityTickingSystem<EntityStore> {
    private final Query<EntityStore> query;

    public CameraSystem() {
        this.query = Player.getComponentType();
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (player != null && playerRef != null) {
            CameraStates.CameraState state = CameraStates.getZoomState(playerRef.getUuid());

            boolean b = hasEquipped(ref, commandBuffer, state);

            if (state.active && !b) {
                state.active = false;
            }

            if (state.active) {
                ServerCameraSettings s = new ServerCameraSettings();
                s.sendMouseMotion = true;
                s.displayReticle = false;

                int zoom = state.zoom >= 0 && state.zoom < state.zoomLevels.size() ? state.zoomLevels.get(state.zoom) : 0;

                if (state.mode == CameraStates.CameraMode.MAP) {
                    s.distance = zoom;
                    s.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffsetRaycast;
                    s.isFirstPerson = false;
                    s.eyeOffset = false;
                    s.positionLerpSpeed = 0.2f;
                    s.rotationLerpSpeed = 0.25f;
                    s.speedModifier = 1.0f;
                    s.positionOffset = new Position(0, zoom * 0.1, 0);
                } else if (state.mode == CameraStates.CameraMode.SPYGLASS) {
                    s.distance = -raytrace(commandBuffer, playerRef.getReference(), zoom);
                    s.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffset;
                    s.isFirstPerson = false;
                    s.eyeOffset = true;
                    s.positionLerpSpeed = 0.2f;
                    s.rotationLerpSpeed = 0.1f;
                    s.speedModifier = 0.5f;
                }

                state.applied = true;
                state.distance = Math.abs(s.distance);

                playerRef.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, true, s));

                if (state.overlay != null) {
                    player.getHudManager().addCustomHud(playerRef, new OverlayHud(playerRef, state.overlay));
                }
            } else if (state.applied) {
                playerRef.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, false, null));
                player.getHudManager().removeCustomHud(playerRef, "YmmersiveEquipment/" + state.overlay);
                state.applied = false;
            }
        }
    }

    private static boolean hasEquipped(Ref<EntityStore> ref, CommandBuffer<EntityStore> commandBuffer, CameraStates.CameraState state) {
        ItemStack mainHand = InventoryComponent.getItemInHand(commandBuffer, ref);
        ItemStack offHand = commandBuffer.getComponent(ref, InventoryComponent.Utility.getComponentType()).getActiveItem();
        boolean inMainHand = mainHand != null && state.lastEquippedItem.equals(mainHand.getItemId());
        boolean inOffHand = offHand != null && state.lastEquippedItem.equals(offHand.getItemId());
        return inMainHand || inOffHand;
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
                targetDistance + 1.0f
        );
        if (hitBlock == null) {
            return targetDistance;
        } else {
            double dx = hitBlock.x + 0.5 - position.x;
            double dy = hitBlock.y + 0.5 - position.y;
            double dz = hitBlock.z + 0.5 - position.z;
            float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            return Math.clamp(distance * 0.9f - 2.0f, 1.0F, targetDistance);
        }
    }
}