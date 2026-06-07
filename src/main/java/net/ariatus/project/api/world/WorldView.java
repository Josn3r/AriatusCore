package net.ariatus.project.api.world;

public interface WorldView {

    String id();

    String displayName();

    String folder();

    boolean loaded();

    boolean enabled();

    boolean autoLoad();
}