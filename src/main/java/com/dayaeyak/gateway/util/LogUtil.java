package com.dayaeyak.gateway.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogUtil {

    public static void info(String method, String path, String level, String message) {
        log.info("[{}][{}][{}][{}]", method, path, level, message);
    }

    public static void warn(String method, String path, String level, String message) {
        log.warn("[{}][{}][{}][{}]", method, path, level, message);
    }

    public static void warn(String method, String path, String level, String... messages) {
        int messageLength = (messages != null) ? messages.length : 0;

        StringBuilder format = new StringBuilder("[{}][{}][{}]");

        String[] args = new String[3 + messageLength];

        args[0] = method;
        args[1] = path;
        args[2] = level;

        for (int i = 0; i < messageLength; i++) {
            format.append("[{}]");
            args[3 + i] = messages[i];
        }

        log.warn(format.toString(), args);
    }
}
