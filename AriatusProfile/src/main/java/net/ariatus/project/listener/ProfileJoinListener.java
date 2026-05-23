package net.ariatus.project.listener;

import net.ariatus.project.profile.ProfileManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ProfileJoinListener implements Listener {

    private final ProfileManager profileManager;

    public ProfileJoinListener(ProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        profileManager.loadProfile(event.getPlayer());
    }
}