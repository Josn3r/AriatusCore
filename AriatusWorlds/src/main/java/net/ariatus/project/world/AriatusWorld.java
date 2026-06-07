package net.ariatus.project.world;

import net.ariatus.project.api.world.WorldView;

public class AriatusWorld implements WorldView {

    private final String id;
    private final String displayName;
    private final String folder;
    private final String environment;
    private final String type;
    private final boolean generateStructures;
    private final String seed;
    private final boolean enabled;
    private final boolean autoLoad;

    private boolean loaded;

    public AriatusWorld(
            String id,
            String displayName,
            String folder,
            String environment,
            String type,
            boolean generateStructures,
            String seed,
            boolean enabled,
            boolean autoLoad,
            boolean loaded
    ) {
        this.id = id;
        this.displayName = displayName;
        this.folder = folder;
        this.environment = environment;
        this.type = type;
        this.generateStructures = generateStructures;
        this.seed = seed;
        this.enabled = enabled;
        this.autoLoad = autoLoad;
        this.loaded = loaded;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public String folder() {
        return folder;
    }

    public String environment() {
        return environment;
    }

    public String type() {
        return type;
    }

    public boolean generateStructures() {
        return generateStructures;
    }

    public String seed() {
        return seed;
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public boolean autoLoad() {
        return autoLoad;
    }

    @Override
    public boolean loaded() {
        return loaded;
    }

    public void loaded(boolean loaded) {
        this.loaded = loaded;
    }
}