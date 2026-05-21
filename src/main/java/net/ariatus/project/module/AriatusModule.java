package net.ariatus.project.module;

import java.util.List;

public interface AriatusModule {

    String id();

    String name();

    default List<String> dependencies() {
        return List.of();
    }

    void enable();

    void disable();

    void reload();

    ModuleStatus status();

}