package net.ariatus.project.ui.menu;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.module.AriatusModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class MenuUtils implements Listener {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final AriatusCore core;
    private final Set<Menu> menus = new HashSet<>();

    private BukkitTask refreshTask;
    private boolean running;

    public MenuUtils(AriatusCore core) {
        this.core = Objects.requireNonNull(core, "core");
    }

    public void start() {
        requireMainThread();

        if (running) {
            return;
        }

        running = true;

        refreshTask = Bukkit.getScheduler().runTaskTimer(
                core,
                this::refreshOpenMenus,
                20L,
                20L
        );
    }

    public Scope scope(AriatusModule owner) {
        return new Scope(
                Objects.requireNonNull(owner, "owner")
        );
    }

    public int trackedMenus() {
        return menus.size();
    }

    public int trackedMenus(AriatusModule owner) {
        Objects.requireNonNull(owner, "owner");

        String ownerId = owner.id().toLowerCase();

        return (int) menus.stream()
                .filter(menu -> menu.ownerId().equals(ownerId))
                .count();
    }

    public void release(AriatusModule owner) {
        Objects.requireNonNull(owner, "owner");
        requireMainThread();

        String ownerId = owner.id().toLowerCase();

        List<Menu> ownedMenus = menus.stream()
                .filter(menu -> menu.ownerId().equals(ownerId))
                .toList();

        for (Menu menu : ownedMenus) {
            menu.releaseInternal();
        }
    }

    public void shutdown() {
        requireMainThread();

        if (!running) {
            return;
        }

        running = false;

        if (
                refreshTask != null
                        && !refreshTask.isCancelled()
        ) {
            refreshTask.cancel();
        }

        refreshTask = null;

        for (Menu menu : List.copyOf(menus)) {
            menu.releaseInternal();
        }

        menus.clear();

        HandlerList.unregisterAll(this);
    }

    public static int[] contentSlots(int rows) {
        if (rows < 3 || rows > 6) {
            throw new IllegalArgumentException(
                    "contentSlots requiere entre 3 y 6 filas."
            );
        }

        List<Integer> slots = new ArrayList<>();

        for (
                int row = 1;
                row < rows - 1;
                row++
        ) {
            for (
                    int column = 1;
                    column <= 7;
                    column++
            ) {
                slots.add(
                        row * 9 + column
                );
            }
        }

        return slots.stream()
                .mapToInt(Integer::intValue)
                .toArray();
    }

    public static <T> Page paginate(
            List<T> source,
            int requestedPage,
            int[] slots,
            BiConsumer<Integer, T> renderer
    ) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(slots, "slots");
        Objects.requireNonNull(renderer, "renderer");

        if (slots.length == 0) {
            throw new IllegalArgumentException(
                    "La paginación necesita al menos un slot."
            );
        }

        int totalPages =
                Math.max(
                        1,
                        (
                                source.size()
                                        + slots.length
                                        - 1
                        )
                                / slots.length
                );

        int page =
                Math.max(
                        0,
                        Math.min(
                                requestedPage,
                                totalPages - 1
                        )
                );

        int offset =
                page * slots.length;

        for (
                int i = 0;
                i < slots.length;
                i++
        ) {
            int index =
                    offset + i;

            if (index >= source.size()) {
                break;
            }

            renderer.accept(
                    slots[i],
                    source.get(index)
            );
        }

        return new Page(
                page,
                totalPages
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(
            InventoryClickEvent event
    ) {
        if (!(
                event.getView()
                        .getTopInventory()
                        .getHolder()
                        instanceof Menu menu
        )) {
            return;
        }

        event.setCancelled(true);

        if (
                !running
                        || menu.released
        ) {
            return;
        }

        if (!(
                event.getWhoClicked()
                        instanceof Player player
        )) {
            return;
        }

        if (
                menu.viewer == null
                        || !menu.viewer.equals(
                        player.getUniqueId()
                )
        ) {
            return;
        }

        int slot =
                event.getRawSlot();

        if (
                slot < 0
                        || slot
                        >= menu.inventory.getSize()
        ) {
            return;
        }

        Consumer<MenuClick> action =
                menu.slotActions.get(
                        slot
                );

        if (action == null) {
            ItemStack clicked =
                    event.getCurrentItem();

            if (
                    clicked != null
                            && !clicked.getType().isAir()
            ) {
                ItemMeta meta =
                        clicked.getItemMeta();

                if (
                        meta != null
                                && meta.displayName() != null
                ) {
                    action =
                            menu.displayNameActions.get(
                                    meta.displayName()
                            );
                }
            }
        }

        if (action == null) {
            return;
        }

        Consumer<MenuClick> finalAction =
                action;

        ClickType clickType =
                event.getClick();

        core.taskManager().runLater(
                menu.owner,
                () -> {
                    if (
                            !running
                                    || menu.released
                                    || !player.isOnline()
                    ) {
                        return;
                    }

                    if (
                            player.getOpenInventory()
                                    .getTopInventory()
                                    != menu.inventory
                    ) {
                        return;
                    }

                    try {
                        finalAction.accept(
                                new MenuClick(
                                        player,
                                        menu,
                                        slot,
                                        clickType
                                )
                        );

                    } catch (
                            Exception exception
                    ) {
                        core.loggerService().error(
                                menu.owner,
                                "Error ejecutando una acción de menú: "
                                        + rootMessage(
                                        exception
                                )
                        );
                    }
                },
                1L
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(
            InventoryDragEvent event
    ) {
        if (
                event.getView()
                        .getTopInventory()
                        .getHolder()
                        instanceof Menu
        ) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(
            InventoryCloseEvent event
    ) {
        if (!(
                event.getInventory()
                        .getHolder()
                        instanceof Menu menu
        )) {
            return;
        }

        if (menu.released) {
            return;
        }

        if (!(
                event.getPlayer()
                        instanceof Player player
        )) {
            return;
        }

        if (
                menu.viewer == null
                        || !menu.viewer.equals(
                        player.getUniqueId()
                )
        ) {
            return;
        }

        Consumer<Player> closeAction =
                menu.closeAction;

        AriatusModule owner =
                menu.owner;

        menu.disposeInternal();

        if (closeAction == null) {
            return;
        }

        core.taskManager().runLater(
                owner,
                () -> {
                    if (!player.isOnline()) {
                        return;
                    }

                    try {
                        closeAction.accept(
                                player
                        );

                    } catch (
                            Exception exception
                    ) {
                        core.loggerService().error(
                                owner,
                                "Error ejecutando el cierre de un menú: "
                                        + rootMessage(
                                        exception
                                )
                        );
                    }
                },
                1L
        );
    }

    private Menu create(
            AriatusModule owner,
            int rows,
            Component title
    ) {
        requireMainThread();

        if (!running) {
            throw new IllegalStateException(
                    "MenuUtils no está iniciado."
            );
        }

        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException(
                    "Un menú debe tener entre 1 y 6 filas."
            );
        }

        Menu menu =
                new Menu(
                        owner,
                        rows,
                        Objects.requireNonNull(
                                title,
                                "title"
                        )
                );

        menus.add(
                menu
        );

        return menu;
    }

    private void refreshOpenMenus() {
        if (!running) {
            return;
        }

        long now =
                System.nanoTime();

        for (
                Player player :
                Bukkit.getOnlinePlayers()
        ) {
            Inventory inventory =
                    player.getOpenInventory()
                            .getTopInventory();

            if (!(
                    inventory.getHolder()
                            instanceof Menu menu
            )) {
                continue;
            }

            if (
                    menu.released
                            || menu.viewer == null
                            || !menu.viewer.equals(
                            player.getUniqueId()
                    )
            ) {
                continue;
            }

            if (
                    menu.refreshAction == null
                            || now
                            < menu.nextRefreshNanos
            ) {
                continue;
            }

            menu.nextRefreshNanos =
                    now
                            + menu.refreshEveryNanos;

            try {
                menu.refreshAction.accept(
                        menu
                );

            } catch (
                    Exception exception
            ) {
                core.loggerService().error(
                        menu.owner,
                        "Error actualizando un menú: "
                                + rootMessage(
                                exception
                        )
                );
            }
        }
    }

    private void requireMainThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException(
                    "MenuUtils debe utilizarse desde el hilo principal."
            );
        }
    }

    private static String rootMessage(
            Throwable throwable
    ) {
        Throwable current =
                throwable;

        while (
                current.getCause()
                        != null
        ) {
            current =
                    current.getCause();
        }

        String message =
                current.getMessage();

        return message == null
                ? current.getClass()
                .getSimpleName()
                : message;
    }

    public final class Scope {

        private final AriatusModule owner;

        private Scope(
                AriatusModule owner
        ) {
            this.owner =
                    owner;
        }

        public Menu create(
                int rows,
                String title
        ) {
            return create(
                    rows,
                    MINI_MESSAGE.deserialize(
                            Objects.requireNonNullElse(
                                    title,
                                    ""
                            )
                    )
            );
        }

        public Menu create(
                int rows,
                Component title
        ) {
            return MenuUtils.this.create(
                    owner,
                    rows,
                    title
            );
        }

        public void release() {
            MenuUtils.this.release(
                    owner
            );
        }

        public int trackedMenus() {
            return MenuUtils.this.trackedMenus(
                    owner
            );
        }
    }

    public final class Menu
            implements InventoryHolder {

        private final AriatusModule owner;
        private final Inventory inventory;

        private final Map<Integer, Consumer<MenuClick>>
                slotActions =
                new HashMap<>();

        private final Map<Component, Consumer<MenuClick>>
                displayNameActions =
                new HashMap<>();

        private UUID viewer;

        private Consumer<Player>
                closeAction;

        private Consumer<Menu>
                refreshAction;

        private long refreshEveryNanos =
                1_000_000_000L;

        private long nextRefreshNanos;

        private boolean released;

        private Menu(
                AriatusModule owner,
                int rows,
                Component title
        ) {
            this.owner =
                    Objects.requireNonNull(
                            owner,
                            "owner"
                    );

            this.inventory =
                    Bukkit.createInventory(
                            this,
                            rows * 9,
                            title
                    );
        }

        public Menu item(
                int slot,
                ItemStack item
        ) {
            requireUsable();
            validateSlot(
                    slot
            );

            inventory.setItem(
                    slot,
                    item == null
                            ? null
                            : item.clone()
            );

            return this;
        }

        public Menu button(
                int slot,
                ItemStack item,
                Consumer<MenuClick> action
        ) {
            item(
                    slot,
                    item
            );

            if (action == null) {
                slotActions.remove(
                        slot
                );
            } else {
                slotActions.put(
                        slot,
                        action
                );
            }

            return this;
        }

        public Menu action(
                int slot,
                Consumer<MenuClick> action
        ) {
            requireUsable();

            validateSlot(
                    slot
            );

            if (action == null) {
                slotActions.remove(
                        slot
                );
            } else {
                slotActions.put(
                        slot,
                        action
                );
            }

            return this;
        }

        public Menu onDisplayName(
                String displayName,
                Consumer<MenuClick> action
        ) {
            return onDisplayName(
                    MINI_MESSAGE.deserialize(
                            Objects.requireNonNullElse(
                                    displayName,
                                    ""
                            )
                    ),
                    action
            );
        }

        public Menu onDisplayName(
                Component displayName,
                Consumer<MenuClick> action
        ) {
            requireUsable();

            Objects.requireNonNull(
                    displayName,
                    "displayName"
            );

            if (action == null) {
                displayNameActions.remove(
                        displayName
                );
            } else {
                displayNameActions.put(
                        displayName,
                        action
                );
            }

            return this;
        }

        public Menu permissionItem(
                int slot,
                Player viewer,
                String permission,
                ItemStack allowedItem,
                ItemStack deniedItem,
                Consumer<MenuClick> allowedAction,
                Consumer<MenuClick> deniedAction
        ) {
            Objects.requireNonNull(
                    viewer,
                    "viewer"
            );

            boolean allowed =
                    permission == null
                            || permission.isBlank()
                            || viewer.hasPermission(
                            permission
                    );

            return button(
                    slot,
                    allowed
                            ? allowedItem
                            : deniedItem,
                    allowed
                            ? allowedAction
                            : deniedAction
            );
        }

        public Menu fill(
                int[] slots,
                ItemStack item
        ) {
            requireUsable();

            Objects.requireNonNull(
                    slots,
                    "slots"
            );

            for (int slot : slots) {
                item(
                        slot,
                        item
                );
            }

            return this;
        }

        public Menu border(
                ItemStack item
        ) {
            requireUsable();

            Objects.requireNonNull(
                    item,
                    "item"
            );

            int rows =
                    inventory.getSize()
                            / 9;

            for (
                    int slot = 0;
                    slot
                            < inventory.getSize();
                    slot++
            ) {
                int row =
                        slot / 9;

                int column =
                        slot % 9;

                if (
                        row == 0
                                || row
                                == rows - 1
                                || column == 0
                                || column == 8
                ) {
                    item(
                            slot,
                            item
                    );
                }
            }

            return this;
        }

        public Menu clear(
                int slot
        ) {
            requireUsable();

            validateSlot(
                    slot
            );

            inventory.setItem(
                    slot,
                    null
            );

            slotActions.remove(
                    slot
            );

            return this;
        }

        public Menu clear() {
            requireUsable();

            inventory.clear();
            slotActions.clear();
            displayNameActions.clear();

            return this;
        }

        public Menu onClose(
                Consumer<Player> action
        ) {
            requireUsable();

            closeAction =
                    action;

            return this;
        }

        public Menu autoRefresh(
                Consumer<Menu> action
        ) {
            return autoRefresh(
                    1L,
                    action
            );
        }

        public Menu autoRefresh(
                long seconds,
                Consumer<Menu> action
        ) {
            requireUsable();

            if (seconds < 1L) {
                throw new IllegalArgumentException(
                        "El intervalo mínimo de refresh es 1 segundo."
                );
            }

            refreshAction =
                    Objects.requireNonNull(
                            action,
                            "action"
                    );

            refreshEveryNanos =
                    seconds
                            * 1_000_000_000L;

            nextRefreshNanos =
                    System.nanoTime()
                            + refreshEveryNanos;

            return this;
        }

        public Menu stopAutoRefresh() {
            requireUsable();

            refreshAction =
                    null;

            return this;
        }

        public void refreshNow() {
            requireUsable();

            if (refreshAction != null) {
                refreshAction.accept(
                        this
                );
            }
        }

        public void open(
                Player player
        ) {
            requireUsable();
            requireMainThread();

            Objects.requireNonNull(
                    player,
                    "player"
            );

            if (
                    viewer != null
                            && !viewer.equals(
                            player.getUniqueId()
                    )
            ) {
                throw new IllegalStateException(
                        "Crea una instancia de Menu independiente para cada jugador."
                );
            }

            viewer =
                    player.getUniqueId();

            nextRefreshNanos =
                    System.nanoTime()
                            + refreshEveryNanos;

            player.openInventory(
                    inventory
            );
        }

        public void switchTo(
                Player player,
                Menu other
        ) {
            requireUsable();

            Objects.requireNonNull(
                    other,
                    "other"
            );

            other.open(
                    player
            );
        }

        public void close(
                Player player
        ) {
            requireMainThread();

            if (
                    player.getOpenInventory()
                            .getTopInventory()
                            == inventory
            ) {
                player.closeInventory();
            }
        }

        public void dispose() {
            requireMainThread();
            disposeInternal();
        }

        public int size() {
            return inventory.getSize();
        }

        public UUID viewer() {
            return viewer;
        }

        public String ownerId() {
            return owner.id()
                    .toLowerCase();
        }

        public boolean released() {
            return released;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        private void validateSlot(
                int slot
        ) {
            if (
                    slot < 0
                            || slot
                            >= inventory.getSize()
            ) {
                throw new IllegalArgumentException(
                        "Slot fuera del inventario: "
                                + slot
                );
            }
        }

        private void requireUsable() {
            if (released) {
                throw new IllegalStateException(
                        "Este menú ya fue liberado."
                );
            }
        }

        private void disposeInternal() {
            if (released) {
                return;
            }

            released =
                    true;

            closeAction =
                    null;

            refreshAction =
                    null;

            slotActions.clear();
            displayNameActions.clear();

            menus.remove(
                    this
            );
        }

        private void releaseInternal() {
            if (released) {
                return;
            }

            released =
                    true;

            closeAction =
                    null;

            refreshAction =
                    null;

            slotActions.clear();
            displayNameActions.clear();

            menus.remove(
                    this
            );

            if (viewer == null) {
                return;
            }

            Player player =
                    Bukkit.getPlayer(
                            viewer
                    );

            if (
                    player != null
                            && player.isOnline()
                            && player.getOpenInventory()
                            .getTopInventory()
                            == inventory
            ) {
                player.closeInventory();
            }
        }
    }

    public record MenuClick(
            Player player,
            Menu menu,
            int slot,
            ClickType type
    ) {
    }

    public record Page(
            int index,
            int total
    ) {

        public boolean hasPrevious() {
            return index > 0;
        }

        public boolean hasNext() {
            return index + 1 < total;
        }

        public int previous() {
            return Math.max(
                    0,
                    index - 1
            );
        }

        public int next() {
            return Math.min(
                    total - 1,
                    index + 1
            );
        }

        public String label() {
            return (
                    index + 1
            )
                    + "/"
                    + total;
        }
    }
}