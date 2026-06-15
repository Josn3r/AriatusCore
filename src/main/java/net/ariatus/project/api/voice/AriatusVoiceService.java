package net.ariatus.project.api.voice;

import java.util.Set;
import java.util.UUID;

public interface AriatusVoiceService {

    boolean isConnected(UUID playerUuid);

    boolean isSpeaking(UUID playerUuid);

    Set<UUID> connectedPlayers();

    int connectedCount();

    default boolean isDisconnected(UUID playerUuid) {
        return !isConnected(playerUuid);
    }

    void playSound(UUID playerUuid, String soundId, double volume);

    default void playSound(UUID playerUuid, String soundId) {
        playSound(playerUuid, soundId, 1.0);
    }

    void stopSound(UUID playerUuid);
    
}