package net.ariatus.project.storage;

import net.ariatus.project.AriatusStorage;
import org.bukkit.Material;

import java.math.BigDecimal;

public class StorageConfig {

    private final AriatusStorage module;

    public StorageConfig(AriatusStorage module) {
        this.module = module;
    }

    public int maxChests() {
        return module.configInt("config.yml", "storage.max-chests", 9);
    }

    public int defaultUnlocked() {
        return module.configInt("config.yml", "storage.default-unlocked", 1);
    }

    public int purchasableUntil() {
        return module.configInt("config.yml", "storage.purchasable-until", 6);
    }

    public int vipStart() {
        return module.configInt("config.yml", "storage.vip-start", 7);
    }

    public String menuTitle() {
        return module.configString("config.yml", "storage.menu.title", "&6Baúles");
    }

    public int menuRows() {
        return module.configInt("config.yml", "storage.menu.rows", 3);
    }

    public String chestTitle(int number) {
        return module.configString("config.yml", "storage.chest.title", "&8Baúl #%number%")
                .replace("%number%", String.valueOf(number));
    }

    public int chestRows(int number) {
        return module.configInt("config.yml", "storage.chests." + number + ".rows", 6);
    }

    public String chestName(int number) {
        return module.configString("config.yml", "storage.chests." + number + ".name", "&aBaúl " + number);
    }

    public BigDecimal price(int number) {
        String value = module.configString("config.yml", "storage.chests." + number + ".price", "0");

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }

    public boolean vip(int number) {
        return module.configBoolean("config.yml", "storage.chests." + number + ".vip", false);
    }

    public String permission(int number) {
        return module.configString("config.yml", "storage.chests." + number + ".permission", "");
    }

    public Material material(int number) {
        String raw = module.configString("config.yml", "storage.chests." + number + ".material", "CHEST");

        try {
            return Material.valueOf(raw.toUpperCase());
        } catch (Exception exception) {
            return Material.CHEST;
        }
    }

    public String message(String key, String fallback) {
        return module.configString("config.yml", "storage.messages." + key, fallback);
    }
}