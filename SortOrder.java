package com.friends.plugin.gui;

public enum SortOrder {
    NORMAL,
    REVERSED;

    public SortOrder flip() {
        return this == NORMAL ? REVERSED : NORMAL;
    }
}
