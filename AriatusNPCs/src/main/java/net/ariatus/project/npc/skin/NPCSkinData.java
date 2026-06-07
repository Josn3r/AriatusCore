package net.ariatus.project.npc.skin;

public class NPCSkinData {

    private NPCSkinMode mode = NPCSkinMode.NONE;
    private String source = "";
    private boolean slim = false;

    private String value = "";
    private String signature = "";

    public NPCSkinMode mode() {
        return mode;
    }

    public void mode(NPCSkinMode mode) {
        this.mode = mode;
    }

    public String source() {
        return source;
    }

    public void source(String source) {
        this.source = source;
    }

    public boolean slim() {
        return slim;
    }

    public void slim(boolean slim) {
        this.slim = slim;
    }

    public String value() {
        return value;
    }

    public void value(String value) {
        this.value = value;
    }

    public String signature() {
        return signature;
    }

    public void signature(String signature) {
        this.signature = signature;
    }

    public boolean hasTexture() {
        return value != null && !value.isBlank() && signature != null && !signature.isBlank();
    }
}