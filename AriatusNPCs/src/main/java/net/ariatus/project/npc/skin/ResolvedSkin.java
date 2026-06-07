package net.ariatus.project.npc.skin;

public record ResolvedSkin(
        String value,
        String signature
) {

    public boolean valid() {
        return value != null
                && !value.isBlank()
                && signature != null
                && !signature.isBlank();
    }
}