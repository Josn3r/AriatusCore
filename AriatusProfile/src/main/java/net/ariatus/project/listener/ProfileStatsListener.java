package net.ariatus.project.listener;

import net.ariatus.project.profile.ProfileManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class ProfileStatsListener implements Listener {

    private final ProfileManager profileManager;

    public ProfileStatsListener(ProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        profileManager.fullProfile(event.getPlayer()).ifPresent(profile ->
                profile.stats().addBlockBroken()
        );
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        profileManager.fullProfile(event.getPlayer()).ifPresent(profile ->
                profile.stats().addBlockPlaced()
        );
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        profileManager.fullProfile(player).ifPresent(profile ->
                profile.stats().addDeath()
        );

        Player killer = player.getKiller();

        if (killer != null) {
            profileManager.fullProfile(killer).ifPresent(profile ->
                    profile.stats().addPlayerKill()
            );
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();

        if (killer == null) {
            return;
        }

        if (event.getEntity() instanceof Player) {
            return;
        }

        profileManager.fullProfile(killer).ifPresent(profile ->
                profile.stats().addMobKill()
        );
    }
}