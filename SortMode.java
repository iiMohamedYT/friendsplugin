package com.friends.plugin.gui;

public enum SortMode {
    DEFAULT("Default", "sorting by date added but showing online players first."),
    ALPHABETICAL("Alphabetical", "sorting by names in alphabetical order."),
    LAST_SEEN("Last seen", "sorted by time when players were online.");

    private final String label;
    private final String description;

    SortMode(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public SortMode next() {
        SortMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
