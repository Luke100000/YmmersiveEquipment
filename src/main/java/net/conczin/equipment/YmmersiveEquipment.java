package net.conczin.equipment;

import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import net.conczin.equipment.interactions.FillContainerInteraction;
import net.conczin.equipment.interactions.SwapItemInteraction;
import net.conczin.equipment.interactions.UpdateCameraInteraction;
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
        this.getCodecRegistry(Interaction.CODEC).register("Ymmersive_Equipment_Swap_Item", SwapItemInteraction.class, SwapItemInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("Ymmersive_Equipment_Update_Camera", UpdateCameraInteraction.class, UpdateCameraInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("Ymmersive_Equipment_Refill_Container", FillContainerInteraction.class, FillContainerInteraction.CODEC);

        this.getEntityStoreRegistry().registerSystem(new CameraSystem());
    }

    public static YmmersiveEquipment getInstance() {
        return instance;
    }
}