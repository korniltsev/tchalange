package ru.korniltsev.telegram.common;

public interface FlowHistoryStripper {
    boolean shouldRemovePath(Object path);
}
