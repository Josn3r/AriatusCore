package net.ariatus.project.module;

import net.ariatus.project.AriatusCore;

public abstract class ExternalAriatusModule implements AriatusModule {

    private AriatusCore core;

    public void initialize(AriatusCore core) {
        this.core = core;
    }

    public AriatusCore core() {
        return core;
    }
}