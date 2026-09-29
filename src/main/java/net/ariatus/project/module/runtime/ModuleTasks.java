package net.ariatus.project.module.runtime;

import net.ariatus.project.module.AriatusModule;
import net.ariatus.project.task.AriatusTaskManager;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

public final class ModuleTasks {

    private final AriatusModule module;
    private final AriatusTaskManager manager;

    public ModuleTasks(AriatusModule module, AriatusTaskManager manager) {
        this.module = Objects.requireNonNull(module, "module");
        this.manager = Objects.requireNonNull(manager, "manager");
    }

    public BukkitTask run(Runnable task) {
        return manager.run(module, task);
    }

    public BukkitTask async(Runnable task) {
        return manager.runAsync(module, task);
    }

    public BukkitTask later(long delayTicks, Runnable task) {
        return manager.runLater(module, task, delayTicks);
    }

    public BukkitTask laterAsync(long delayTicks, Runnable task) {
        return manager.runLaterAsync(module, task, delayTicks);
    }

    public BukkitTask repeat(long periodTicks, Runnable task) {
        return manager.runRepeating(module, task, periodTicks, periodTicks);
    }

    public BukkitTask repeat(long delayTicks, long periodTicks, Runnable task) {
        return manager.runRepeating(module, task, delayTicks, periodTicks);
    }

    public BukkitTask repeatAsync(long periodTicks, Runnable task) {
        return manager.runRepeatingAsync(module, task, periodTicks, periodTicks);
    }

    public BukkitTask repeatAsync(long delayTicks, long periodTicks, Runnable task) {
        return manager.runRepeatingAsync(module, task, delayTicks, periodTicks);
    }

    public int active() {
        return manager.activeTasks(module);
    }

    public void cancelAll() {
        manager.cancelAll(module);
    }
}