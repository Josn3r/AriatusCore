package net.ariatus.project.task;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.module.AriatusModule;
import net.ariatus.project.module.ExternalAriatusModule;
import net.ariatus.project.profiler.ModuleProfiler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class AriatusTaskManager {

    private final AriatusCore core;
    private final ModuleProfiler profiler;

    private final Map<String, List<BukkitTask>> tasksByModule = new HashMap<>();
    private final Map<String, TaskMetrics> metricsByModule = new HashMap<>();

    public AriatusTaskManager(AriatusCore core) {
        this.core = core;
        this.profiler = core.profiler();
    }

    public BukkitTask runRepeating(AriatusModule module, Runnable runnable, long delayTicks, long periodTicks) {
        String moduleId = module.id().toLowerCase();

        Runnable measuredRunnable = () -> runMeasured(moduleId, runnable);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(
                core,
                measuredRunnable,
                delayTicks,
                periodTicks
        );

        tasksByModule
                .computeIfAbsent(moduleId, id -> new ArrayList<>())
                .add(task);

        return task;
    }

    public BukkitTask runLater(AriatusModule module, Runnable runnable, long delayTicks) {
        String moduleId = module.id().toLowerCase();

        Runnable measuredRunnable = () -> runMeasured(moduleId, runnable);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(
                core,
                measuredRunnable,
                delayTicks
        );

        tasksByModule
                .computeIfAbsent(moduleId, id -> new ArrayList<>())
                .add(task);

        return task;
    }

    public BukkitTask run(AriatusModule module, Runnable runnable) {
        String moduleId = module.id().toLowerCase();
        Runnable measuredRunnable = () -> runMeasured(moduleId, runnable);
        BukkitTask task = Bukkit.getScheduler().runTask(core, measuredRunnable);
        tasksByModule.computeIfAbsent(moduleId, id -> new ArrayList<>()).add(task);
        return task;
    }

    public BukkitTask runAsync(AriatusModule module, Runnable runnable) {
        String moduleId = module.id().toLowerCase();
        Runnable measuredRunnable = () -> runMeasured(moduleId, runnable);
        BukkitTask task = Bukkit.getScheduler().runTaskAsynchronously(core, measuredRunnable);
        tasksByModule.computeIfAbsent(moduleId, id -> new ArrayList<>()).add(task);
        return task;
    }

    public void cancelAll(AriatusModule module) {
        String moduleId = module.id().toLowerCase();

        List<BukkitTask> tasks = tasksByModule.remove(moduleId);

        if (tasks == null) {
            return;
        }

        for (BukkitTask task : tasks) {
            if (!task.isCancelled()) {
                task.cancel();
            }
        }
    }

    public int activeTasks(AriatusModule module) {
        String moduleId = module.id().toLowerCase();

        List<BukkitTask> tasks = tasksByModule.getOrDefault(moduleId, List.of());

        return (int) tasks.stream()
                .filter(task -> !task.isCancelled())
                .count();
    }

    public TaskMetrics metrics(AriatusModule module) {
        return metricsByModule.computeIfAbsent(
                module.id().toLowerCase(),
                id -> new TaskMetrics()
        );
    }

    public void cancelAll() {
        for (List<BukkitTask> tasks : tasksByModule.values()) {
            for (BukkitTask task : tasks) {
                if (!task.isCancelled()) {
                    task.cancel();
                }
            }
        }

        tasksByModule.clear();
    }

    private void runMeasured(String moduleId, Runnable runnable) {
        long start = System.nanoTime();
        try {
            runnable.run();
        } catch (Exception exception) {
            metricsByModule.computeIfAbsent(moduleId, id -> new TaskMetrics()).recordError();
            profiler.error(moduleId);
            core.getLogger().warning(
                    "[AriatusTaskManager] Error en task del módulo " + moduleId + ": " + exception.getMessage()
            );
        } finally {
            long end = System.nanoTime();
            long elapsed = end - start;
            metricsByModule.computeIfAbsent(moduleId, id -> new TaskMetrics()).record(elapsed);
            profiler.record(moduleId, elapsed);
        }
    }
}