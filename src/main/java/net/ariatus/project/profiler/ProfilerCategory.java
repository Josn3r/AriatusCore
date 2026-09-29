package net.ariatus.project.profiler;

public enum ProfilerCategory {

    TASK_SYNC("Task Sync"),
    TASK_ASYNC("Task Async"),
    COMMAND("Command"),
    INTERNAL_EVENT("Internal Event"),
    DATABASE("Database");

    private final String displayName;

    ProfilerCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}