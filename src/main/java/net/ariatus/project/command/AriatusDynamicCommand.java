package net.ariatus.project.command;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.module.AriatusModule;
import net.ariatus.project.profiler.ProfilerCategory;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class AriatusDynamicCommand extends Command {

    private final AriatusCore core;
    private final AriatusModule module;
    private final AriatusCommandExecutor executor;

    public AriatusDynamicCommand(
            AriatusCore core,
            AriatusModule module,
            AriatusCommandExecutor executor
    ) {
        super(
                executor.name()
                        .toLowerCase(
                                Locale.ROOT
                        ),
                executor.description(),
                executor.usage(),
                executor.aliases()
        );

        this.core =
                Objects.requireNonNull(
                        core,
                        "core"
                );

        this.module =
                Objects.requireNonNull(
                        module,
                        "module"
                );

        this.executor =
                Objects.requireNonNull(
                        executor,
                        "executor"
                );

        String permission =
                executor.permission();

        setPermission(
                permission == null
                        || permission.isBlank()
                        ? null
                        : permission
        );
    }

    @Override
    public boolean execute(
            CommandSender sender,
            String commandLabel,
            String[] args
    ) {
        if (!testPermission(sender)) {
            return true;
        }

        long start =
                System.nanoTime();

        try {
            return executor.execute(
                    sender,
                    args
            );

        } catch (Exception exception) {
            core.profiler()
                    .error(
                            module.id(),
                            ProfilerCategory.COMMAND
                    );

            module.logger()
                    .error(
                            "Error ejecutando /"
                                    + getName()
                                    + ".",
                            exception
                    );

            return true;

        } finally {
            core.profiler()
                    .record(
                            module.id(),
                            ProfilerCategory.COMMAND,
                            System.nanoTime()
                                    - start,
                            Bukkit.isPrimaryThread()
                    );
        }
    }

    @Override
    public List<String> tabComplete(
            CommandSender sender,
            String alias,
            String[] args
    ) throws IllegalArgumentException {

        if (!testPermissionSilent(sender)) {
            return List.of();
        }

        try {
            List<String> completions =
                    executor.tabComplete(
                            sender,
                            args
                    );

            if (
                    completions == null
                            || completions.isEmpty()
            ) {
                return List.of();
            }

            String current =
                    args.length == 0
                            ? ""
                            : args[
                            args.length - 1
                            ]
                            .toLowerCase(
                                    Locale.ROOT
                            );

            List<String> filtered =
                    new ArrayList<>();

            for (
                    String completion :
                    completions
            ) {
                if (
                        completion == null
                                || completion.isBlank()
                ) {
                    continue;
                }

                if (
                        completion.toLowerCase(
                                        Locale.ROOT
                                )
                                .startsWith(
                                        current
                                )
                ) {
                    filtered.add(
                            completion
                    );
                }
            }

            return filtered;

        } catch (Exception exception) {
            module.logger()
                    .error(
                            "Error generando tab completion para /"
                                    + getName()
                                    + ".",
                            exception
                    );

            return List.of();
        }
    }

    public AriatusModule module() {
        return module;
    }

    public AriatusCommandExecutor executor() {
        return executor;
    }
}