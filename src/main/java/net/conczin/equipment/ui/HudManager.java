package net.conczin.equipment.ui;

import com.buuz135.mhud.MultipleHUD;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class HudManager {
    public static void setHud(Player player, PlayerRef playerRef, String identifier, CustomUIHud hud) {
        PluginBase plugin = PluginManager.get().getPlugin(PluginIdentifier.fromString("Buuz135:MultipleHUD"));
        if (plugin == null) {
            player.getHudManager().setCustomHud(playerRef, hud);
        } else {
            MultipleHUD.getInstance().setCustomHud(player, playerRef, identifier, hud);
        }
    }
}
