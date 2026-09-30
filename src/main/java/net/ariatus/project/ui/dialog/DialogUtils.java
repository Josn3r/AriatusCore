package net.ariatus.project.ui.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.ariatus.project.AriatusCore;
import net.ariatus.project.module.AriatusModule;
import net.ariatus.project.module.ModuleStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class DialogUtils {

    private static final MiniMessage MINI_MESSAGE =
            MiniMessage.miniMessage();

    private static final Duration CALLBACK_LIFETIME =
            Duration.ofMinutes(3);

    private static final String TEXT_KEY =
            "value";

    private static final String SELECT_KEY =
            "choice";

    private static final String BOOLEAN_KEY =
            "enabled";

    private final AriatusCore core;

    private final Map<UUID, PendingAction> actions =
            new ConcurrentHashMap<>();

    private final Method showDialogMethod;

    private BukkitTask cleanupTask;

    private volatile boolean running;

    public DialogUtils(
            AriatusCore core
    ) {
        this.core =
                Objects.requireNonNull(
                        core,
                        "core"
                );

        this.showDialogMethod =
                resolveShowDialogMethod();
    }

    public void start() {
        requireMainThread();

        if (running) {
            return;
        }

        running =
                true;

        cleanupTask =
                Bukkit.getScheduler()
                        .runTaskTimer(
                                core,
                                this::cleanupExpired,
                                1200L,
                                1200L
                        );
    }

    public Scope scope(
            AriatusModule owner
    ) {
        return new Scope(
                Objects.requireNonNull(
                        owner,
                        "owner"
                )
        );
    }

    public boolean available() {
        return running
                && showDialogMethod != null;
    }

    public int pendingCallbacks() {
        cleanupExpired();
        return actions.size();
    }

    public int pendingCallbacks(
            AriatusModule owner
    ) {
        Objects.requireNonNull(
                owner,
                "owner"
        );

        cleanupExpired();

        String ownerId =
                owner.id()
                        .toLowerCase();

        return (int) actions.values()
                .stream()
                .filter(action ->
                        action.owner()
                                .id()
                                .equalsIgnoreCase(
                                        ownerId
                                )
                )
                .count();
    }

    public void release(
            AriatusModule owner
    ) {
        Objects.requireNonNull(
                owner,
                "owner"
        );

        requireMainThread();

        String ownerId =
                owner.id()
                        .toLowerCase();

        actions.entrySet()
                .removeIf(entry ->
                        entry.getValue()
                                .owner()
                                .id()
                                .equalsIgnoreCase(
                                        ownerId
                                )
                );
    }

    public void shutdown() {
        requireMainThread();

        running =
                false;

        if (
                cleanupTask != null
                        && !cleanupTask.isCancelled()
        ) {
            cleanupTask.cancel();
        }

        cleanupTask =
                null;

        actions.clear();
    }

    private void notice(
            AriatusModule owner,
            Player player,
            String title,
            String message
    ) {
        requireAvailable(
                owner,
                player
        );

        Dialog dialog =
                Dialog.create(
                        builder ->
                                builder.empty()
                                        .base(
                                                DialogBase.builder(
                                                                parse(
                                                                        title
                                                                )
                                                        )
                                                        .body(
                                                                List.of(
                                                                        DialogBody.plainMessage(
                                                                                parse(
                                                                                        message
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                        .canCloseWithEscape(
                                                                true
                                                        )
                                                        .afterAction(
                                                                DialogBase.DialogAfterAction.CLOSE
                                                        )
                                                        .build()
                                        )
                                        .type(
                                                DialogType.notice()
                                        )
                );

        show(
                player,
                dialog
        );
    }

    private void text(
            AriatusModule owner,
            Player player,
            String title,
            String label,
            String initial,
            int maxLength,
            Consumer<String> onSave
    ) {
        requireAvailable(
                owner,
                player
        );

        Objects.requireNonNull(
                onSave,
                "onSave"
        );

        if (maxLength < 1) {
            throw new IllegalArgumentException(
                    "maxLength debe ser mayor que 0."
            );
        }

        UUID groupId =
                UUID.randomUUID();

        DialogAction saveAction =
                register(
                        owner,
                        player,
                        groupId,
                        response -> {
                            String value =
                                    response.getText(
                                            TEXT_KEY
                                    );

                            if (value == null) {
                                return;
                            }

                            int length =
                                    value.codePointCount(
                                            0,
                                            value.length()
                                    );

                            if (length > maxLength) {
                                return;
                            }

                            onSave.accept(
                                    value
                            );
                        }
                );

        Dialog dialog =
                Dialog.create(
                        builder ->
                                builder.empty()
                                        .base(
                                                DialogBase.builder(
                                                                parse(
                                                                        title
                                                                )
                                                        )
                                                        .canCloseWithEscape(
                                                                true
                                                        )
                                                        .afterAction(
                                                                DialogBase.DialogAfterAction.CLOSE
                                                        )
                                                        .inputs(
                                                                List.of(
                                                                        DialogInput.text(
                                                                                TEXT_KEY,
                                                                                300,
                                                                                parse(
                                                                                        label
                                                                                ),
                                                                                true,
                                                                                Objects.requireNonNullElse(
                                                                                        initial,
                                                                                        ""
                                                                                ),
                                                                                maxLength,
                                                                                null
                                                                        )
                                                                )
                                                        )
                                                        .build()
                                        )
                                        .type(
                                                DialogType.notice(
                                                        button(
                                                                "<green>Guardar</green>",
                                                                saveAction
                                                        )
                                                )
                                        )
                );

        show(
                player,
                dialog
        );
    }

    private void confirm(
            AriatusModule owner,
            Player player,
            String title,
            String message,
            Consumer<Boolean> onResult
    ) {
        requireAvailable(
                owner,
                player
        );

        Objects.requireNonNull(
                onResult,
                "onResult"
        );

        UUID groupId =
                UUID.randomUUID();

        DialogAction accept =
                register(
                        owner,
                        player,
                        groupId,
                        response ->
                                onResult.accept(
                                        true
                                )
                );

        DialogAction reject =
                register(
                        owner,
                        player,
                        groupId,
                        response ->
                                onResult.accept(
                                        false
                                )
                );

        Dialog dialog =
                Dialog.create(
                        builder ->
                                builder.empty()
                                        .base(
                                                DialogBase.builder(
                                                                parse(
                                                                        title
                                                                )
                                                        )
                                                        .body(
                                                                List.of(
                                                                        DialogBody.plainMessage(
                                                                                parse(
                                                                                        message
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                        .canCloseWithEscape(
                                                                true
                                                        )
                                                        .afterAction(
                                                                DialogBase.DialogAfterAction.CLOSE
                                                        )
                                                        .build()
                                        )
                                        .type(
                                                DialogType.confirmation(
                                                        button(
                                                                "<green>Confirmar</green>",
                                                                accept
                                                        ),
                                                        button(
                                                                "<red>Cancelar</red>",
                                                                reject
                                                        )
                                                )
                                        )
                );

        show(
                player,
                dialog
        );
    }

    private void select(
            AriatusModule owner,
            Player player,
            String title,
            String label,
            List<Option> options,
            String initial,
            Consumer<String> onSelect
    ) {
        requireAvailable(
                owner,
                player
        );

        Objects.requireNonNull(
                options,
                "options"
        );

        Objects.requireNonNull(
                onSelect,
                "onSelect"
        );

        if (options.isEmpty()) {
            throw new IllegalArgumentException(
                    "El selector necesita al menos una opción."
            );
        }

        Set<String> validIds =
                new HashSet<>();

        List<SingleOptionDialogInput.OptionEntry> entries =
                new ArrayList<>();

        for (Option option : options) {
            if (!validIds.add(option.id())) {
                throw new IllegalArgumentException(
                        "Opción duplicada: "
                                + option.id()
                );
            }

            entries.add(
                    SingleOptionDialogInput.OptionEntry.create(
                            option.id(),
                            parse(
                                    option.label()
                            ),
                            option.id()
                                    .equals(
                                            initial
                                    )
                    )
            );
        }

        UUID groupId =
                UUID.randomUUID();

        DialogAction saveAction =
                register(
                        owner,
                        player,
                        groupId,
                        response -> {
                            String selected =
                                    response.getText(
                                            SELECT_KEY
                                    );

                            if (
                                    selected == null
                                            || !validIds.contains(
                                            selected
                                    )
                            ) {
                                return;
                            }

                            onSelect.accept(
                                    selected
                            );
                        }
                );

        Dialog dialog =
                Dialog.create(
                        builder ->
                                builder.empty()
                                        .base(
                                                DialogBase.builder(
                                                                parse(
                                                                        title
                                                                )
                                                        )
                                                        .canCloseWithEscape(
                                                                true
                                                        )
                                                        .afterAction(
                                                                DialogBase.DialogAfterAction.CLOSE
                                                        )
                                                        .inputs(
                                                                List.of(
                                                                        DialogInput.singleOption(
                                                                                SELECT_KEY,
                                                                                300,
                                                                                entries,
                                                                                parse(
                                                                                        label
                                                                                ),
                                                                                true
                                                                        )
                                                                )
                                                        )
                                                        .build()
                                        )
                                        .type(
                                                DialogType.notice(
                                                        button(
                                                                "<green>Seleccionar</green>",
                                                                saveAction
                                                        )
                                                )
                                        )
                );

        show(
                player,
                dialog
        );
    }

    private void toggle(
            AriatusModule owner,
            Player player,
            String title,
            String label,
            boolean initial,
            Consumer<Boolean> onSave
    ) {
        requireAvailable(
                owner,
                player
        );

        Objects.requireNonNull(
                onSave,
                "onSave"
        );

        UUID groupId =
                UUID.randomUUID();

        DialogAction saveAction =
                register(
                        owner,
                        player,
                        groupId,
                        response -> {
                            Boolean value =
                                    response.getBoolean(
                                            BOOLEAN_KEY
                                    );

                            if (value != null) {
                                onSave.accept(
                                        value
                                );
                            }
                        }
                );

        Dialog dialog =
                Dialog.create(
                        builder ->
                                builder.empty()
                                        .base(
                                                DialogBase.builder(
                                                                parse(
                                                                        title
                                                                )
                                                        )
                                                        .canCloseWithEscape(
                                                                true
                                                        )
                                                        .afterAction(
                                                                DialogBase.DialogAfterAction.CLOSE
                                                        )
                                                        .inputs(
                                                                List.of(
                                                                        DialogInput.bool(
                                                                                BOOLEAN_KEY,
                                                                                parse(
                                                                                        label
                                                                                ),
                                                                                initial,
                                                                                "true",
                                                                                "false"
                                                                        )
                                                                )
                                                        )
                                                        .build()
                                        )
                                        .type(
                                                DialogType.notice(
                                                        button(
                                                                "<green>Guardar cambios</green>",
                                                                saveAction
                                                        )
                                                )
                                        )
                );

        show(
                player,
                dialog
        );
    }

    private DialogAction register(
            AriatusModule owner,
            Player expected,
            UUID groupId,
            Consumer<DialogResponseView> handler
    ) {
        UUID actionId =
                UUID.randomUUID();

        long expiresAtNanos =
                System.nanoTime()
                        + CALLBACK_LIFETIME.toNanos();

        actions.put(
                actionId,
                new PendingAction(
                        owner,
                        expected.getUniqueId(),
                        groupId,
                        expiresAtNanos,
                        Objects.requireNonNull(
                                handler,
                                "handler"
                        )
                )
        );

        return DialogAction.customClick(
                (response, audience) ->
                        dispatch(
                                actionId,
                                response,
                                audience
                        ),
                ClickCallback.Options.builder()
                        .uses(1)
                        .lifetime(
                                CALLBACK_LIFETIME
                        )
                        .build()
        );
    }

    private void dispatch(
            UUID actionId,
            DialogResponseView response,
            Object audience
    ) {
        if (!running) {
            return;
        }

        if (!(
                audience
                        instanceof Player player
        )) {
            return;
        }

        PendingAction pending =
                actions.get(
                        actionId
                );

        if (pending == null) {
            return;
        }

        if (
                System.nanoTime()
                        >= pending.expiresAtNanos()
        ) {
            removeGroup(
                    pending.groupId()
            );

            return;
        }

        if (
                !pending.expectedPlayer()
                        .equals(
                                player.getUniqueId()
                        )
        ) {
            return;
        }

        removeGroup(
                pending.groupId()
        );

        AriatusModule owner =
                pending.owner();

        if (
                owner.status()
                        != ModuleStatus.ENABLED
        ) {
            return;
        }

        Runnable action =
                () -> {
                    if (
                            !running
                                    || !player.isOnline()
                                    || owner.status()
                                    != ModuleStatus.ENABLED
                    ) {
                        return;
                    }

                    try {
                        pending.handler()
                                .accept(
                                        response
                                );

                    } catch (
                            Exception exception
                    ) {
                        core.loggerService().error(
                                owner,
                                "Error procesando una respuesta de diálogo: "
                                        + rootMessage(
                                        exception
                                )
                        );
                    }
                };

        if (Bukkit.isPrimaryThread()) {
            action.run();
        } else {
            core.taskManager().run(
                    owner,
                    action
            );
        }
    }

    private void removeGroup(
            UUID groupId
    ) {
        actions.entrySet()
                .removeIf(entry ->
                        entry.getValue()
                                .groupId()
                                .equals(
                                        groupId
                                )
                );
    }

    private void cleanupExpired() {
        long now =
                System.nanoTime();

        actions.entrySet()
                .removeIf(entry ->
                        now
                                >= entry.getValue()
                                .expiresAtNanos()
                );
    }

    private void show(
            Player player,
            Dialog dialog
    ) {
        requireMainThread();

        if (showDialogMethod == null) {
            core.loggerService().error(
                    "DialogUtils: Audience#showDialog(DialogLike) no está disponible."
            );

            return;
        }

        try {
            showDialogMethod.invoke(
                    player,
                    dialog
            );

        } catch (
                IllegalAccessException exception
        ) {
            throw new IllegalStateException(
                    "No se pudo acceder a Audience#showDialog(DialogLike).",
                    exception
            );

        } catch (
                InvocationTargetException exception
        ) {
            Throwable cause =
                    exception.getCause();

            if (
                    cause
                            instanceof RuntimeException runtimeException
            ) {
                throw runtimeException;
            }

            throw new IllegalStateException(
                    "Paper produjo un error mostrando el diálogo.",
                    cause
            );
        }
    }

    private Method resolveShowDialogMethod() {
        try {
            ClassLoader loader =
                    DialogUtils.class
                            .getClassLoader();

            Class<?> audienceClass =
                    Class.forName(
                            "net.kyori.adventure.audience.Audience",
                            false,
                            loader
                    );

            Class<?> dialogLikeClass =
                    Class.forName(
                            "net.kyori.adventure.dialog.DialogLike",
                            false,
                            loader
                    );

            return audienceClass.getMethod(
                    "showDialog",
                    dialogLikeClass
            );

        } catch (
                ClassNotFoundException
                        | NoSuchMethodException exception
        ) {
            core.loggerService().error(
                    "DialogUtils: la API showDialog no está disponible: "
                            + rootMessage(
                            exception
                    )
            );

            return null;

        } catch (
                LinkageError error
        ) {
            core.loggerService().error(
                    "DialogUtils: incompatibilidad binaria de Adventure: "
                            + rootMessage(
                            error
                    )
            );

            return null;
        }
    }

    private void requireAvailable(
            AriatusModule owner,
            Player player
    ) {
        requireMainThread();

        if (!running) {
            throw new IllegalStateException(
                    "DialogUtils está deshabilitado."
            );
        }

        Objects.requireNonNull(
                owner,
                "owner"
        );

        Objects.requireNonNull(
                player,
                "player"
        );

        if (!player.isOnline()) {
            throw new IllegalStateException(
                    "El jugador no está conectado."
            );
        }
    }

    private void requireMainThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException(
                    "DialogUtils debe utilizarse desde el hilo principal."
            );
        }
    }

    private static ActionButton button(
            String label,
            DialogAction action
    ) {
        return ActionButton.builder(
                        parse(
                                label
                        )
                )
                .action(
                        action
                )
                .build();
    }

    private static Component parse(
            String text
    ) {
        return MINI_MESSAGE.deserialize(
                Objects.requireNonNullElse(
                        text,
                        ""
                )
        );
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

        public boolean available() {
            return DialogUtils.this.available();
        }

        public void notice(
                Player player,
                String title,
                String message
        ) {
            DialogUtils.this.notice(
                    owner,
                    player,
                    title,
                    message
            );
        }

        public void text(
                Player player,
                String title,
                String label,
                String initial,
                int maxLength,
                Consumer<String> onSave
        ) {
            DialogUtils.this.text(
                    owner,
                    player,
                    title,
                    label,
                    initial,
                    maxLength,
                    onSave
            );
        }

        public void confirm(
                Player player,
                String title,
                String message,
                Consumer<Boolean> onResult
        ) {
            DialogUtils.this.confirm(
                    owner,
                    player,
                    title,
                    message,
                    onResult
            );
        }

        public void select(
                Player player,
                String title,
                String label,
                List<Option> options,
                String initial,
                Consumer<String> onSelect
        ) {
            DialogUtils.this.select(
                    owner,
                    player,
                    title,
                    label,
                    options,
                    initial,
                    onSelect
            );
        }

        public void toggle(
                Player player,
                String title,
                String label,
                boolean initial,
                Consumer<Boolean> onSave
        ) {
            DialogUtils.this.toggle(
                    owner,
                    player,
                    title,
                    label,
                    initial,
                    onSave
            );
        }

        public void release() {
            DialogUtils.this.release(
                    owner
            );
        }

        public int pendingCallbacks() {
            return DialogUtils.this.pendingCallbacks(
                    owner
            );
        }
    }

    private record PendingAction(
            AriatusModule owner,
            UUID expectedPlayer,
            UUID groupId,
            long expiresAtNanos,
            Consumer<DialogResponseView> handler
    ) {
    }

    public record Option(
            String id,
            String label
    ) {

        public Option {
            Objects.requireNonNull(
                    id,
                    "id"
            );

            Objects.requireNonNull(
                    label,
                    "label"
            );

            if (id.isBlank()) {
                throw new IllegalArgumentException(
                        "El ID de una opción no puede estar vacío."
                );
            }
        }
    }
}