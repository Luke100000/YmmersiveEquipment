package net.conczin.equipment.data;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public final class CameraStates {
    private static final Map<UUID, CameraState> STATES = new ConcurrentHashMap<>();

    public static CameraState getZoomState(UUID player) {
        return STATES.computeIfAbsent(player, _ -> new CameraState());
    }

    public enum CameraMode {
        MAP,
        SPYGLASS,
    }

    public static class CameraState {
        public boolean active = false;
        public boolean applied = false;
        public int zoom = 0;
        public List<Integer> zoomLevels = List.of(0);
        public String overlay = null;
        public CameraMode mode = CameraMode.MAP;
        public String lastEquippedItem = "";
        public float distance;
    }
}