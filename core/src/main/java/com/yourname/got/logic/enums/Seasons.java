package com.yourname.got.logic.enums;

public enum Seasons {
    SPRING,SUMMER,FALL,WINTER;
    public static Seasons fromInt(int index) {
        Seasons[] values = Seasons.values();
        if (index >= 0 && index < values.length) {
            return values[index];
        }
        throw new IllegalArgumentException("Invalid SEASON index: " + index);
    }
}
