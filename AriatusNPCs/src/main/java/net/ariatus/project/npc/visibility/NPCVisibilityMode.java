package net.ariatus.project.npc.visibility;

public enum NPCVisibilityMode {

    ALL,
    MANUAL,
    PERMISSION;

    public static NPCVisibilityMode parse(String input) {
        return valueOf(input.trim().toUpperCase());
    }
}