package net.ariatus.project.ui.item;

import net.ariatus.project.integration.ItemsAdderService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ItemUtils {

    private static final MiniMessage MINI_MESSAGE =
            MiniMessage.miniMessage();

    private final ItemsAdderService itemsAdder;

    public ItemUtils(
            ItemsAdderService itemsAdder
    ) {
        this.itemsAdder =
                Objects.requireNonNull(
                        itemsAdder,
                        "itemsAdder"
                );
    }

    public ItemBuilder create(
            Material material
    ) {
        return new ItemBuilder(
                new ItemStack(
                        validMaterial(
                                material
                        )
                )
        );
    }

    public ItemBuilder create(
            String material
    ) {
        Material resolved =
                Material.matchMaterial(
                        Objects.requireNonNullElse(
                                material,
                                ""
                        )
                );

        return create(
                resolved == null
                        ? Material.BARRIER
                        : resolved
        );
    }

    public ItemBuilder custom(
            String namespacedId,
            Material fallback
    ) {
        ItemStack base =
                itemsAdder.resolve(
                                namespacedId
                        )
                        .orElseGet(
                                () ->
                                        new ItemStack(
                                                validMaterial(
                                                        fallback
                                                )
                                        )
                        );

        return new ItemBuilder(
                base
        );
    }

    public Component text(
            String miniMessage
    ) {
        return MINI_MESSAGE.deserialize(
                Objects.requireNonNullElse(
                        miniMessage,
                        ""
                )
        );
    }

    private static Material validMaterial(
            Material material
    ) {
        if (
                material == null
                        || material.isAir()
                        || !material.isItem()
        ) {
            return Material.BARRIER;
        }

        return material;
    }

    public static final class ItemBuilder {

        private final ItemStack item;

        private ItemBuilder(
                ItemStack item
        ) {
            this.item =
                    Objects.requireNonNull(
                                    item,
                                    "item"
                            )
                            .clone();
        }

        public ItemBuilder name(
                String name
        ) {
            return name(
                    MINI_MESSAGE.deserialize(
                            Objects.requireNonNullElse(
                                    name,
                                    ""
                            )
                    )
            );
        }

        public ItemBuilder name(
                Component name
        ) {
            ItemMeta meta =
                    item.getItemMeta();

            if (meta != null) {
                meta.displayName(
                        Objects.requireNonNull(
                                name,
                                "name"
                        )
                );

                item.setItemMeta(
                        meta
                );
            }

            return this;
        }

        public ItemBuilder lore(
                String... lines
        ) {
            return lore(
                    Arrays.asList(
                            lines
                    )
            );
        }

        public ItemBuilder lore(
                List<String> lines
        ) {
            List<Component> components =
                    lines == null
                            ? List.of()
                            : lines.stream()
                            .map(
                                    line ->
                                            MINI_MESSAGE.deserialize(
                                                    Objects.requireNonNullElse(
                                                            line,
                                                            ""
                                                    )
                                            )
                            )
                            .toList();

            return loreComponents(
                    components
            );
        }

        public ItemBuilder loreComponents(
                List<Component> lines
        ) {
            ItemMeta meta =
                    item.getItemMeta();

            if (meta != null) {
                meta.lore(
                        lines == null
                                ? List.of()
                                : List.copyOf(
                                lines
                        )
                );

                item.setItemMeta(
                        meta
                );
            }

            return this;
        }

        public ItemBuilder amount(
                int amount
        ) {
            item.setAmount(
                    Math.max(
                            1,
                            Math.min(
                                    amount,
                                    item.getMaxStackSize()
                            )
                    )
            );

            return this;
        }

        public ItemBuilder glow(
                boolean glow
        ) {
            ItemMeta meta =
                    item.getItemMeta();

            if (meta != null) {
                meta.setEnchantmentGlintOverride(
                        glow
                );

                item.setItemMeta(
                        meta
                );
            }

            return this;
        }

        public ItemBuilder unbreakable(
                boolean unbreakable
        ) {
            ItemMeta meta =
                    item.getItemMeta();

            if (meta != null) {
                meta.setUnbreakable(
                        unbreakable
                );

                item.setItemMeta(
                        meta
                );
            }

            return this;
        }

        public ItemBuilder hide(
                ItemFlag... flags
        ) {
            ItemMeta meta =
                    item.getItemMeta();

            if (
                    meta != null
                            && flags != null
                            && flags.length > 0
            ) {
                meta.addItemFlags(
                        flags
                );

                item.setItemMeta(
                        meta
                );
            }

            return this;
        }

        public ItemBuilder hideAttributes() {
            return hide(
                    ItemFlag.HIDE_ATTRIBUTES,
                    ItemFlag.HIDE_ENCHANTS,
                    ItemFlag.HIDE_UNBREAKABLE,
                    ItemFlag.HIDE_DESTROYS,
                    ItemFlag.HIDE_PLACED_ON,
                    ItemFlag.HIDE_DYE,
                    ItemFlag.HIDE_ARMOR_TRIM,
                    ItemFlag.HIDE_STORED_ENCHANTS
            );
        }

        public ItemBuilder head(
                UUID uuid
        ) {
            Objects.requireNonNull(
                    uuid,
                    "uuid"
            );

            ItemMeta meta =
                    item.getItemMeta();

            if (
                    meta
                            instanceof SkullMeta skullMeta
            ) {
                skullMeta.setPlayerProfile(
                        Bukkit.createProfile(
                                uuid
                        )
                );

                item.setItemMeta(
                        skullMeta
                );
            }

            return this;
        }

        public ItemStack build() {
            return item.clone();
        }
    }
}