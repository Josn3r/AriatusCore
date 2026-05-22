package net.ariatus.project.module.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

public record ModuleConfig(
        File file,
        FileConfiguration configuration
) {
}