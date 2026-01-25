package net.conczin.equipment;

import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import net.conczin.equipment.interactions.FillContainerInteraction;
import net.conczin.equipment.interactions.UpdateCameraInteraction;
import net.conczin.equipment.interactions.RememberPositionInteraction;
import net.conczin.equipment.interactions.TeleportInteraction;
import net.conczin.equipment.systems.CameraSystem;

import javax.annotation.Nonnull;


public class YmmersiveEquipment extends JavaPlugin {
    private static YmmersiveEquipment instance;

    public YmmersiveEquipment(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        this.getCodecRegistry(Interaction.CODEC).register("Ymmersive_Equipment_Update_Camera", UpdateCameraInteraction.class, UpdateCameraInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("Ymmersive_Equipment_Refill_Container", FillContainerInteraction.class, FillContainerInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("Ymmersive_Equipment_Remember_Position", RememberPositionInteraction.class, RememberPositionInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("Ymmersive_Equipment_Teleport", TeleportInteraction.class, TeleportInteraction.CODEC);

        this.getEntityStoreRegistry().registerSystem(new CameraSystem());
    }

    public static YmmersiveEquipment getInstance() {
        return instance;
    }
}