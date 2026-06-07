package net.ariatus.project.npc.attribute;

public enum NPCPoseType {

    STANDING,
    CROUCHING,
    SWIMMING,
    SLEEPING,
    SITTING;

    public static NPCPoseType parse(String input) {
        return valueOf(input.trim().toUpperCase());
    }
}