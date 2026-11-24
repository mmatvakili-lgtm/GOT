package com.yourname.got;

import com.yourname.got.logic.util.GameLog;
import com.yourname.got.support.Check;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

/** The logger writes to logs/got.log and never throws. */
class GameLogTest {
    @Test
    void messagesReachTheLogFile() throws Exception {
        Check.clear();
        GameLog.setConsoleEnabled(false);
        try {
            GameLog.info("log-test-info-marker");
            GameLog.warn("log-test-warn-marker");
            GameLog.error("log-test-error-marker");
            GameLog.info(null); // must not throw
            GameLog.flush();
        } finally {
            GameLog.setConsoleEnabled(true);
        }
        File file = new File("logs" + File.separator + "got.log");
        Check.that(file.exists(), "logs/got.log was created");
        String text = file.exists() ? Files.readString(file.toPath()) : "";
        Check.that(text.contains("[INFO] log-test-info-marker"), "info lines carry the INFO level");
        Check.that(text.contains("[WARN] log-test-warn-marker"), "warn lines carry the WARN level");
        Check.that(text.contains("[ERROR] log-test-error-marker"), "error lines carry the ERROR level");
        Check.assertAll();
    }
}
