package net.ariatus.project.listener;

import com.destroystokyo.paper.ParticleBuilder;
import net.ariatus.project.profile.ProfileManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;

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

    @EventHandler
    public void onPlayerMove (PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.isOnGround() && player.isSprinting()) {
            spawnParticle(player.getLocation(), Particle.END_ROD, 2, 0.3, 0.3, 0.3, 0.01);
        }
    }

    //

    public void spawnParticle(Location location, Particle particle, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        if (location == null || location.getWorld() == null) return;
        new ParticleBuilder(particle).location(location).count(count).offset(offsetX, offsetY, offsetZ).extra(extra).spawn();
    }
}