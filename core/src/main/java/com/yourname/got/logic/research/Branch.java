package com.yourname.got.logic.research;

/** The three columns of the tree. */
public enum Branch {
    ECONOMY("Economy", "Resources, storage and knowledge"),
    MILITARY("Military", "Training, marching and fighting"),
    DEFENSE("Defense", "Towers, walls and structures");

    public final String label, description;

    Branch(String label, String description) {
        this.label = label;
        this.description = description;
    }
}
