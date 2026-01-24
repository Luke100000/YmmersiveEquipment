package net.conczin.equipment.ui;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public class EmptyHud extends CustomUIHud {
    public EmptyHud(@Nonnull PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        builder.append("YmmersiveEquipment/Empty.ui");
    }
}
