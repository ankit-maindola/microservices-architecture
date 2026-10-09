package com.example.auth_service.debug;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

final class DebugLog {

    private DebugLog() {
    }

    static void write(String hypothesisId, String location, String message, String dataJson) {
        // #region agent log
        try {
            Path cwd = Path.of(System.getProperty("user.dir"));
            Path root = cwd.getFileName().toString().equals("auth-service") ? cwd.getParent() : cwd;
            Path logFile = root.resolve("debug-9e36d5.log");
            long ts = System.currentTimeMillis();
            String line = "{\"sessionId\":\"9e36d5\",\"hypothesisId\":\"" + hypothesisId
                    + "\",\"location\":\"" + location + "\",\"message\":\"" + message
                    + "\",\"data\":" + dataJson + ",\"timestamp\":" + ts + "}\n";
            Files.writeString(logFile, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
        // #endregion
    }
}
