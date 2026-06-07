package net.ariatus.project.storage;

import net.ariatus.project.AriatusStorage;
import net.ariatus.project.api.economy.Currency;
import net.ariatus.project.api.economy.EconomyService;
import net.ariatus.project.api.storage.StorageChestView;
import net.ariatus.project.api.storage.StorageService;
import net.ariatus.project.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class StorageManager implements StorageService {

    private final AriatusStorage module;
    private final StorageRepository repository;
    private final StorageConfig config;

    public StorageManager(AriatusStorage module, StorageRepository repository, StorageConfig config) {
        this.module = module;
        this.repository = repository;
        this.config = config;
    }

    @Override
    public void openMenu(Player player) {
        UUID uuid = player.getUniqueId();

        List<CompletableFuture<ChestMenuState>> futures = new ArrayList<>();

        for (int number = 1; number <= config.maxChests(); number++) {
            int chestNumber = number;

            CompletableFuture<ChestMenuState> future = repository.unlocked(uuid, chestNumber)
                    .thenApply(unlocked -> new ChestMenuState(chestNumber, unlocked));

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> Bukkit.getScheduler().runTask(module.core(), () -> {
                    if (!player.isOnline()) {
                        return;
                    }

                    Inventory inventory = Bukkit.createInventory(
                            new StorageMenuHolder(),
                            config.menuRows() * 9,
                            MessageService.parse(config.menuTitle())
                    );

                    for (CompletableFuture<ChestMenuState> future : futures) {
                        ChestMenuState state = future.join();

                        ItemStack item = menuItem(player, state.chestNumber(), state.unlocked());
                        inventory.setItem(slotFor(state.chestNumber()), item);
                    }

                    player.openInventory(inventory);
                }));
    }

    @Override
    public void openChest(Player player, int chestNumber) {
        if (!validChest(chestNumber)) {
            MessageService.send(player, config.message("invalid-chest", "&cEse baúl no existe."));
            return;
        }

        repository.unlocked(player.getUniqueId(), chestNumber).thenAccept(unlocked -> {
            if (!unlocked && !canAccessVipChest(player, chestNumber)) {
                module.tasks().runAsync(module, () -> {
                    MessageService.send(player, config.message("vip-required", "&cEste baúl requiere rango VIP."));
                });
                return;
            }

            int rows = config.chestRows(chestNumber);
            int size = rows * 9;

            repository.loadContent(player.getUniqueId(), chestNumber).thenAccept(content -> {
                ItemStack[] items = InventorySerializer.deserialize(content, size);

                module.tasks().run(module, () -> {
                    Inventory inventory = Bukkit.createInventory(
                            new StorageChestHolder(player.getUniqueId(), chestNumber),
                            size,
                            MessageService.parse(config.chestTitle(chestNumber))
                    );

                    inventory.setContents(items);
                    player.openInventory(inventory);
                });
            });
        });
    }

    @Override
    public CompletableFuture<Boolean> unlockChest(UUID uuid, int chestNumber) {
        if (!validChest(chestNumber)) {
            return CompletableFuture.completedFuture(false);
        }

        return repository.unlock(uuid, chestNumber).thenApply(ignored -> true);
    }

    public void purchaseChest(Player player, int chestNumber) {
        if (!validChest(chestNumber)) {
            MessageService.send(player, config.message("invalid-chest", "&cEse baúl no existe."));
            return;
        }

        if (chestNumber <= config.defaultUnlocked()) {
            MessageService.send(player, config.message("already-unlocked", "&eYa tienes este baúl desbloqueado."));
            return;
        }

        if (chestNumber >= config.vipStart()) {
            if (canAccessVipChest(player, chestNumber)) {
                openChest(player, chestNumber);
                return;
            }

            MessageService.send(player, config.message("vip-required", "&cEste baúl requiere rango VIP."));
            return;
        }

        if (chestNumber > config.purchasableUntil()) {
            MessageService.send(player, config.message("vip-required", "&cEste baúl requiere rango VIP."));
            return;
        }

        repository.unlocked(player.getUniqueId(), chestNumber).thenAccept(unlocked -> {
            if (unlocked) {
                module.tasks().runAsync(module, () -> {
                    MessageService.send(player, config.message("already-unlocked", "&eYa tienes este baúl desbloqueado."));
                });
                return;
            }

            repository.unlocked(player.getUniqueId(), chestNumber - 1).thenAccept(previousUnlocked -> {
                if (!previousUnlocked) {
                    module.tasks().runAsync(module, () -> {
                        MessageService.send(player, config.message("previous-required", "&cDebes desbloquear primero el baúl anterior."));
                    });
                    return;
                }

                EconomyService economy = economyService();

                if (economy == null) {
                    module.tasks().runAsync(module, () -> {
                        MessageService.send(player, "&cEconomía no disponible.");
                    });
                    return;
                }

                Currency currency = currency();
                BigDecimal price = config.price(chestNumber);

                economy.withdraw(player.getUniqueId(), currency, price, "storage_unlock_chest_" + chestNumber).thenAccept(success -> {
                            if (!success) {
                                module.tasks().runAsync(module, () -> {
                                    MessageService.send(player, config.message("not-enough-money", "&cNo tienes suficiente dinero."));
                                });
                                return;
                            }

                            repository.unlock(player.getUniqueId(), chestNumber).thenRun(() -> {
                                    module.tasks().runAsync(module, () -> {
                                        MessageService.send(
                                                player,
                                                config.message("unlocked", "&aHas desbloqueado el &eBaúl #%number%&a.")
                                                        .replace("%number%", String.valueOf(chestNumber))
                                        );
                                        openMenu(player);
                                    });
                            });
                });
            });
        });
    }

    public void saveChest(UUID uuid, int chestNumber, Inventory inventory) {
        String content = InventorySerializer.serialize(inventory);
        repository.saveContent(uuid, chestNumber, content);
    }

    @Override
    public CompletableFuture<Boolean> unlocked(UUID uuid, int chestNumber) {
        return repository.unlocked(uuid, chestNumber);
    }

    @Override
    public Optional<StorageChestView> chest(UUID uuid, int chestNumber) {
        if (!validChest(chestNumber)) {
            return Optional.empty();
        }

        boolean vip = config.vip(chestNumber);
        int rows = config.chestRows(chestNumber);

        return Optional.of(new PlayerStorageChest(
                uuid,
                chestNumber,
                chestNumber <= config.defaultUnlocked(),
                vip,
                rows
        ));
    }

    public boolean validChest(int number) {
        return number >= 1 && number <= config.maxChests();
    }

    public boolean canAccessVipChest(Player player, int chestNumber) {
        if (!config.vip(chestNumber)) {
            return false;
        }

        String permission = config.permission(chestNumber);

        return permission != null && !permission.isBlank() && player.hasPermission(permission);
    }

    private ItemStack menuItem(Player player, int chestNumber, boolean unlocked) {
        boolean vip = config.vip(chestNumber);
        boolean vipAccess = canAccessVipChest(player, chestNumber);

        Material material = config.material(chestNumber);

        if (!unlocked && vip && !vipAccess) {
            material = Material.ENDER_CHEST;
        } else if (!unlocked) {
            material = Material.BARRIER; // fallback below
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.displayName(MessageService.parse(config.chestName(chestNumber)));

        List<String> lore = new ArrayList<>();

        if (unlocked || vipAccess) {
            lore.add("&aDesbloqueado");
            lore.add("&7Click para abrir.");
        } else if (vip) {
            lore.add("&6Baúl VIP");
            lore.add("&cRequiere permiso:");
            lore.add("&7" + config.permission(chestNumber));
        } else {
            lore.add("&cBloqueado");
            lore.add("&7Precio: &e" + priceText(chestNumber));
            lore.add("&7Click para comprar.");
        }

        meta.lore(lore.stream().map(MessageService::parse).toList());
        item.setItemMeta(meta);

        return item;
    }

    private int slotFor(int chestNumber) {
        return 9 + chestNumber - 1;
    }

    private String priceText(int chestNumber) {
        EconomyService economy = economyService();

        if (economy == null) {
            return config.price(chestNumber).toPlainString();
        }

        return economy.format(currency(), config.price(chestNumber));
    }

    private EconomyService economyService() {
        try {
            return module.services().require(EconomyService.class);
        } catch (Exception exception) {
            return null;
        }
    }

    private Currency currency() {
        String raw = module.configString("config.yml", "storage.economy.currency", "COINS");

        try {
            return Currency.from(raw);
        } catch (Exception exception) {
            return Currency.COINS;
        }
    }

    private record ChestMenuState(int chestNumber, boolean unlocked) {
    }
}