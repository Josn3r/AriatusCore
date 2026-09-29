package net.ariatus.project.logger;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.module.AriatusModule;

import java.util.logging.Level;

public final class LoggerService {

    private final AriatusCore core;

    public LoggerService(AriatusCore core) {
        this.core = core;
    }

    public void info(String message) {
        log(LogLevel.INFO, "Core", message, null);
    }

    public void warn(String message) {
        log(LogLevel.WARN, "Core", message, null);
    }

    public void error(String message) {
        log(LogLevel.ERROR, "Core", message, null);
    }

    public void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, "Core", message, throwable);
    }

    public void debug(String message) {
        if (!isDebugEnabled()) {
            return;
        }

        log(LogLevel.DEBUG, "Core", message, null);
    }

    public void info(AriatusModule module, String message) {
        log(LogLevel.INFO, module.id(), message, null);
    }

    public void warn(AriatusModule module, String message) {
        log(LogLevel.WARN, module.id(), message, null);
    }

    public void error(AriatusModule module, String message) {
        log(LogLevel.ERROR, module.id(), message, null);
    }

    public void error(AriatusModule module, String message, Throwable throwable) {
        log(LogLevel.ERROR, module.id(), message, throwable);
    }

    public void debug(AriatusModule module, String message) {
        if (!isDebugEnabled()) {
            return;
        }

        log(LogLevel.DEBUG, module.id(), message, null);
    }

    private void log(LogLevel level, String source, String message, Throwable throwable) {
        String formatted = "[Ariatus/" + source + "/" + level.name() + "] " + message;

        if (throwable != null) {
            core.getLogger().log(Level.SEVERE, formatted, throwable);
            return;
        }

        switch (level) {
            case INFO, DEBUG -> core.getLogger().info(formatted);
            case WARN -> core.getLogger().warning(formatted);
            case ERROR -> core.getLogger().severe(formatted);
        }
    }

    private boolean isDebugEnabled() {
        return core.configManager().getBoolean("ariatus.debug", false);
    }
}