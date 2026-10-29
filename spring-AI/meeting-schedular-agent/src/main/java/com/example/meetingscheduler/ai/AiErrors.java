package com.example.meetingscheduler.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class AiErrors {

    private static final Pattern KEY_PATTERN = Pattern.compile("(sk-[A-Za-z0-9_\\-]+|AIza[A-Za-z0-9_\\-]+)");

    private AiErrors() {
    }

    public static String sanitize(String message) {
        if (message == null) {
            return null;
        }
        return KEY_PATTERN.matcher(message).replaceAll("sk-***");
    }

    public static String rootMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return sanitize(root.getMessage());
    }

    public static List<String> detailChain(Throwable throwable) {
        List<String> details = new ArrayList<>();
        Throwable current = throwable;
        while (current != null) {
            String message = sanitize(current.getMessage());
            if (message != null && !message.isBlank()) {
                details.add(current.getClass().getSimpleName() + ": " + message);
            }
            current = current.getCause();
        }
        return details;
    }
}
