package com.yourname.got.logic.util;

public class Result {
    private final boolean success;
    private final String message;
    private final Object gameInstance;

    public Result(boolean success, String message) {
        this(success, message, null);
    }

    public Result(boolean success, String message, Object gameInstance) {
        this.success = success;
        this.message = message;
        this.gameInstance = gameInstance;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Object getGameInstance() {
        return gameInstance;
    }
}
