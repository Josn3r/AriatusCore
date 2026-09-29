package net.ariatus.project.task;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.module.AriatusModule;
import net.ariatus.project.profiler.ModuleProfiler;
import net.ariatus.project.profiler.ProfilerCategory;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class AriatusTaskManager {

    private final AriatusCore core;
    private final ModuleProfiler profiler;

    private final Map<String, Set<BukkitTask>> tasksByModule =
            new ConcurrentHashMap<>();

    public AriatusTaskManager(
            AriatusCore core
    ) {
        this.core =
                Objects.requireNonNull(
                        core,
                        "core"
                );

        this.profiler =
                core.profiler();
    }

    public BukkitTask run(
            AriatusModule module,
            Runnable runnable
    ) {
        String moduleId =
                moduleId(module);

        BukkitTask task =
                Bukkit.getScheduler()
                        .runTask(
                                core,
                                measured(
                                        moduleId,
                                        ProfilerCategory.TASK_SYNC,
                                        runnable
                                )
                        );

        return track(
                moduleId,
                task
        );
    }

    public BukkitTask runAsync(
            AriatusModule module,
            Runnable runnable
    ) {
        String moduleId =
                moduleId(module);

        BukkitTask task =
                Bukkit.getScheduler()
                        .runTaskAsynchronously(
                                core,
                                measured(
                                        moduleId,
                                        ProfilerCategory.TASK_ASYNC,
                                        runnable
                                )
                        );

        return track(
                moduleId,
                task
        );
    }

    public BukkitTask runLater(
            AriatusModule module,
            Runnable runnable,
            long delayTicks
    ) {
        String moduleId =
                moduleId(module);

        BukkitTask task =
                Bukkit.getScheduler()
                        .runTaskLater(
                                core,
                                measured(
                                        moduleId,
                                        ProfilerCategory.TASK_SYNC,
                                        runnable
                                ),
                                delayTicks
                        );

        return track(
                moduleId,
                task
        );
    }

    public BukkitTask runLaterAsync(
            AriatusModule module,
            Runnable runnable,
            long delayTicks
    ) {
        String moduleId =
                moduleId(module);

        BukkitTask task =
                Bukkit.getScheduler()
                        .runTaskLaterAsynchronously(
                                core,
                                measured(
                                        moduleId,
                                        ProfilerCategory.TASK_ASYNC,
                                        runnable
                                ),
                                delayTicks
                        );

        return track(
                moduleId,
                task
        );
    }

    public BukkitTask runRepeating(
            AriatusModule module,
            Runnable runnable,
            long delayTicks,
            long periodTicks
    ) {
        String moduleId =
                moduleId(module);

        BukkitTask task =
                Bukkit.getScheduler()
                        .runTaskTimer(
                                core,
                                measured(
                                        moduleId,
                                        ProfilerCategory.TASK_SYNC,
                                        runnable
                                ),
                                delayTicks,
                                periodTicks
                        );

        return track(
                moduleId,
                task
        );
    }

    public BukkitTask runRepeatingAsync(
            AriatusModule module,
            Runnable runnable,
            long delayTicks,
            long periodTicks
    ) {
        String moduleId =
                moduleId(module);

        BukkitTask task =
                Bukkit.getScheduler()
                        .runTaskTimerAsynchronously(
                                core,
                                measured(
                                        moduleId,
                                        ProfilerCategory.TASK_ASYNC,
                                        runnable
                                ),
                                delayTicks,
                                periodTicks
                        );

        return track(
                moduleId,
                task
        );
    }

    public void cancelAll(
            AriatusModule module
    ) {
        String moduleId =
                moduleId(module);

        Set<BukkitTask> tasks =
                tasksByModule.remove(
                        moduleId
                );

        if (tasks == null) {
            return;
        }

        tasks.forEach(task -> {
            if (!task.isCancelled()) {
                task.cancel();
            }
        });
    }

    public int activeTasks(
            AriatusModule module
    ) {
        String moduleId =
                moduleId(module);

        Set<BukkitTask> tasks =
                tasksByModule.get(
                        moduleId
                );

        if (tasks == null) {
            return 0;
        }

        tasks.removeIf(task ->
                task.isCancelled()
                        || (
                        !Bukkit.getScheduler()
                                .isQueued(
                                        task.getTaskId()
                                )
                                && !Bukkit.getScheduler()
                                .isCurrentlyRunning(
                                        task.getTaskId()
                                )
                )
        );

        if (tasks.isEmpty()) {
            tasksByModule.remove(
                    moduleId,
                    tasks
            );

            return 0;
        }

        return tasks.size();
    }

    public void cancelAll() {
        tasksByModule.values()
                .forEach(tasks ->
                        tasks.forEach(task -> {
                            if (!task.isCancelled()) {
                                task.cancel();
                            }
                        })
                );

        tasksByModule.clear();
    }

    private BukkitTask track(
            String moduleId,
            BukkitTask task
    ) {
        tasksByModule
                .computeIfAbsent(
                        moduleId,
                        ignored ->
                                ConcurrentHashMap.newKeySet()
                )
                .add(task);

        return task;
    }

    private Runnable measured(
            String moduleId,
            ProfilerCategory category,
            Runnable runnable
    ) {
        Objects.requireNonNull(
                runnable,
                "runnable"
        );

        return () -> {
            long start =
                    System.nanoTime();

            try {
                runnable.run();

            } catch (Exception exception) {
                profiler.error(
                        moduleId,
                        category
                );

                core.loggerService()
                        .error(
                                "Error en task del módulo "
                                        + moduleId
                                        + ".",
                                exception
                        );

            } finally {
                profiler.record(
                        moduleId,
                        category,
                        System.nanoTime()
                                - start,
                        Bukkit.isPrimaryThread()
                );
            }
        };
    }

    private String moduleId(
            AriatusModule module
    ) {
        return Objects.requireNonNull(
                        module,
                        "module"
                )
                .id()
                .toLowerCase(
                        Locale.ROOT
                );
    }
}