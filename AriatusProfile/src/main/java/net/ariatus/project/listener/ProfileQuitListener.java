package net.ariatus.project.listener;

import net.ariatus.project.profile.ProfileManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class ProfileQuitListener implements Listener {

    private final ProfileManager profileManager;

    public ProfileQuitListener(ProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        profileManager.unloadProfile(event.getPlayer());
    }
}