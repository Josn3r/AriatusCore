package net.ariatus.project.logger;

import net.ariatus.project.AriatusCore;
import net.ariatus.project.module.AriatusModule;

public class LoggerService {

    private final AriatusCore core;

    public LoggerService(AriatusCore core) {
        this.core = core;
    }

    public void info(String message) {
        log(LogLevel.INFO, "Core", message);
    }

    public void warn(String message) {
        log(LogLevel.WARN, "Core", message);
    }

    public void error(String message) {
        log(LogLevel.ERROR, "Core", message);
    }

    public void debug(String message) {
        if (!isDebugEnabled()) return;
        log(LogLevel.DEBUG, "Core", message);
    }

    public void info(AriatusModule module, String message) {
        log(LogLevel.INFO, module.id(), message);
    }

    public void warn(AriatusModule module, String message) {
        log(LogLevel.WARN, module.id(), message);
    }

    public void error(AriatusModule module, String message) {
        log(LogLevel.ERROR, module.id(), message);
    }

    public void debug(AriatusModule module, String message) {
        if (!isDebugEnabled()) return;
        log(LogLevel.DEBUG, module.id(), message);
    }

    private void log(LogLevel level, String source, String message) {
        String formatted = "[Ariatus/" + source + "/" + level.name() + "] " + message;

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