package com.yourname.got.logic.login;

import java.io.Serializable;
import java.time.LocalDateTime;

public class SystemMessage implements Serializable {
    private String content;
    private LocalDateTime timestamp;
    private boolean read;

    public SystemMessage(String content) {
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.read = false;
    }

    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isRead() { return read; }

    public void markAsRead() { this.read = true; }

    @Override
    public String toString() {
        return (read ? "[READ] " : "[NEW] ") + timestamp + " - " + content;
    }
}

