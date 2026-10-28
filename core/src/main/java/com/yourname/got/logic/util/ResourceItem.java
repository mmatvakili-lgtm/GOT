package com.yourname.got.logic.util;

import com.yourname.got.logic.enums.ResourceType;
import java.io.Serializable;

public class ResourceItem implements Serializable {
    private static final long serialVersionUID = 1L;

    public ResourceType type;
    public int amount;

    public ResourceItem(ResourceType type, int amount) {
        this.type = type;
        this.amount = amount;
    }
}
