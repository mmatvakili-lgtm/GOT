package com.yourname.got.logic.enums;

public enum ResourceType {
    Wood,
    Food,
    Coin,
    Stone;

    public static ResourceType fromInt(int index) {
        ResourceType[] values = ResourceType.values();
        if (index >= 0 && index < values.length) {
            return values[index];
        }
        throw new IllegalArgumentException("Invalid resource index: " + index);
    }

}

