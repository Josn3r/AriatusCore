package net.ariatus.project.integration;

import net.ariatus.project.AriatusCore;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ItemsAdderService {

    private final AriatusCore core;

    private final Set<String> warnedIds =
            ConcurrentHashMap.newKeySet();

    private volatile Accessor accessor;

    public ItemsAdderService(
            AriatusCore core
    ) {
        this.core =
                Objects.requireNonNull(
                        core,
                        "core"
                );
    }

    public boolean available() {
        Plugin plugin =
                Bukkit.getPluginManager()
                        .getPlugin(
                                "ItemsAdder"
                        );

        return plugin != null
                && plugin.isEnabled();
    }

    public Optional<ItemStack> resolve(
            String namespacedId
    ) {
        Objects.requireNonNull(
                namespacedId,
                "namespacedId"
        );

        if (namespacedId.isBlank()) {
            return Optional.empty();
        }

        Plugin plugin =
                Bukkit.getPluginManager()
                        .getPlugin(
                                "ItemsAdder"
                        );

        if (
                plugin == null
                        || !plugin.isEnabled()
        ) {
            return Optional.empty();
        }

        try {
            Accessor current =
                    accessor;

            if (
                    current == null
                            || current.plugin()
                            != plugin
            ) {
                current =
                        createAccessor(
                                plugin
                        );

                accessor =
                        current;
            }

            Object customStack =
                    current.getInstance()
                            .invoke(
                                    null,
                                    namespacedId
                            );

            if (customStack == null) {
                warnOnce(
                        namespacedId,
                        "no existe en el registro de ItemsAdder."
                );

                return Optional.empty();
            }

            Object result =
                    current.getItemStack()
                            .invoke(
                                    customStack
                            );

            if (
                    result
                            instanceof ItemStack itemStack
            ) {
                return Optional.of(
                        itemStack.clone()
                );
            }

            warnOnce(
                    namespacedId,
                    "getItemStack() no devolvió un ItemStack."
            );

            return Optional.empty();

        } catch (
                ReflectiveOperationException
                        | LinkageError exception
        ) {
            warnOnce(
                    namespacedId,
                    rootMessage(
                            exception
                    )
            );

            accessor =
                    null;

            return Optional.empty();
        }
    }

    public void invalidate() {
        accessor = null;
        warnedIds.clear();
    }

    private Accessor createAccessor(
            Plugin plugin
    ) throws
            ClassNotFoundException,
            NoSuchMethodException {

        ClassLoader loader =
                plugin.getClass()
                        .getClassLoader();

        Class<?> customStackClass =
                Class.forName(
                        "dev.lone.itemsadder.api.CustomStack",
                        true,
                        loader
                );

        Method getInstance =
                customStackClass.getMethod(
                        "getInstance",
                        String.class
                );

        Method getItemStack =
                customStackClass.getMethod(
                        "getItemStack"
                );

        return new Accessor(
                plugin,
                getInstance,
                getItemStack
        );
    }

    private void warnOnce(
            String id,
            String message
    ) {
        if (
                warnedIds.add(
                        id
                )
        ) {
            core.loggerService()
                    .warn(
                            "ItemsAdder: no se pudo resolver '"
                                    + id
                                    + "': "
                                    + message
                    );
        }
    }

    private static String rootMessage(
            Throwable throwable
    ) {
        Throwable current =
                throwable;

        if (
                current
                        instanceof InvocationTargetException invocationTargetException
                        && invocationTargetException
                        .getCause()
                        != null
        ) {
            current =
                    invocationTargetException
                            .getCause();
        }

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

    private record Accessor(
            Plugin plugin,
            Method getInstance,
            Method getItemStack
    ) {
    }
}