package net.ariatus.project.npc;

public record NPCAction(
        NPCActionType type,
        String value
) {

    public static NPCAction parse(String typeName, String value) {
        NPCActionType type = NPCActionType.valueOf(typeName.trim().toUpperCase());
        return new NPCAction(type, value == null ? "" : value);
    }

    public static NPCAction parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Acción vacía.");
        }

        String[] split = raw.split(":", 2);
        NPCActionType type = NPCActionType.valueOf(split[0].trim().toUpperCase());
        String value = split.length > 1 ? split[1] : "";

        return new NPCAction(type, value);
    }

    public String serialize() {
        if (value == null || value.isBlank()) {
            return type.name();
        }

        return type.name() + ":" + value;
    }
}