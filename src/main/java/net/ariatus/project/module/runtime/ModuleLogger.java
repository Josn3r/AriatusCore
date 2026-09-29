package net.ariatus.project.module.runtime;

import net.ariatus.project.logger.LoggerService;
import net.ariatus.project.module.AriatusModule;

import java.util.Objects;

public final class ModuleLogger {

    private final AriatusModule module;
    private final LoggerService logger;

    public ModuleLogger(AriatusModule module, LoggerService logger) {
        this.module = Objects.requireNonNull(module, "module");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void info(String message) {
        logger.info(module, message);
    }

    public void warn(String message) {
        logger.warn(module, message);
    }

    public void error(String message) {
        logger.error(module, message);
    }

    public void error(String message, Throwable throwable) {
        logger.error(module, message, throwable);
    }

    public void debug(String message) {
        logger.debug(module, message);
    }
}