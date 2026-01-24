package net.conczin.equipment.ui;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import net.conczin.equipment.data.CameraStates;

import javax.annotation.Nonnull;

public class OverlayHud extends CustomUIHud {
    private final String overlay;

    public OverlayHud(@Nonnull PlayerRef playerRef, String overlay) {
        super(playerRef);

        this.overlay = overlay;
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        builder.append("YmmersiveEquipment/" + overlay + ".ui");

        CameraStates.CameraState state = CameraStates.getZoomState(getPlayerRef().getUuid());

        builder.set("#ZoomLabel.Text", (state.zoom + 1) + " x");
        builder.set("#DistanceLabel.Text", Math.ceil(state.distance) + " m");
    }
}
