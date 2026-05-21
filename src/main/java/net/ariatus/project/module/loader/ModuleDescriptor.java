package net.ariatus.project.module.loader;

import java.io.File;
import java.util.List;

public record ModuleDescriptor(
        String id,
        String name,
        String main,
        String version,
        List<String> dependencies,
        File file
) {
}