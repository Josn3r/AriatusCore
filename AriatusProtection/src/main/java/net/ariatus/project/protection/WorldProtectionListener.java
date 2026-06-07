package net.ariatus.project.protection;

import net.ariatus.project.message.MessageService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class WorldProtectionListener implements Listener {

    private final ProtectionManager protectionManager;
    private final Map<UUID, Long> messageCooldown = new HashMap<>();

    public WorldProtectionListener(ProtectionManager protectionManager) {
        this.protectionManager = protectionManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (bypass(player)) {
            return;
        }

        Optional<ProtectionSettings> optionalSettings =
                protectionManager.settings(player.getWorld().getName());

        if (optionalSettings.isEmpty()) {
            return;
        }

        ProtectionSettings settings = optionalSettings.get();

        if (settings.blockBreaking()) {
            return;
        }

        event.setCancelled(true);
        send(player, protectionManager.message("break-blocked", "&cNo puedes romper bloques en esta zona."));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (bypass(player)) {
            return;
        }

        Optional<ProtectionSettings> optionalSettings =
                protectionManager.settings(player.getWorld().getName());

        if (optionalSettings.isEmpty()) {
            return;
        }

        ProtectionSettings settings = optionalSettings.get();

        if (settings.blockPlacing()) {
            return;
        }

        event.setCancelled(true);
        send(player, protectionManager.message("place-blocked", "&cNo puedes colocar bloques en esta zona."));
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();

        if (bypass(player)) {
            return;
        }

        Block block = event.getClickedBlock();

        if (block == null) {
            return;
        }

        Optional<ProtectionSettings> optionalSettings =
                protectionManager.settings(block.getWorld().getName());

        if (optionalSettings.isEmpty()) {
            return;
        }

        ProtectionSettings settings = optionalSettings.get();
        Material material = block.getType();

        if (!settings.interactionBlocked(material)) {
            return;
        }

        event.setCancelled(true);
        send(player, protectionManager.message("interact-blocked", "&cEste bloque es decorativo."));
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (bypass(player)) {
            return;
        }

        Location location = event.getInventory().getLocation();

        if (location == null || location.getWorld() == null) {
            return;
        }

        Optional<ProtectionSettings> optionalSettings =
                protectionManager.settings(location.getWorld().getName());

        if (optionalSettings.isEmpty()) {
            return;
        }

        Material material = location.getBlock().getType();
        ProtectionSettings settings = optionalSettings.get();

        if (!settings.inventoryBlocked(material)) {
            return;
        }

        event.setCancelled(true);
        send(player, protectionManager.message("inventory-blocked", "&cNo puedes abrir este bloque."));
    }

    private boolean bypass(CommandSender sender) {
        return sender.hasPermission(protectionManager.bypassPermission());
    }

    private void send(Player player, String message) {
        long now = System.currentTimeMillis();
        long last = messageCooldown.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < 1200) {
            return;
        }

        messageCooldown.put(player.getUniqueId(), now);
        MessageService.send(player, message);
    }
}