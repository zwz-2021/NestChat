package com.nestchat.server.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class TraceIdUtil {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static void set(String traceId) {
        TRACE_ID.set(traceId);
    }

    public static String get() {
        String id = TRACE_ID.get();
        if (id == null) {
            id = generate();
            TRACE_ID.set(id);
        }
        return id;
    }

    public static void clear() {
        TRACE_ID.remove();
    }

    public static String generate() {
        String date = LocalDate.now().format(FMT);
        String hex = Integer.toHexString(ThreadLocalRandom.current().nextInt(0x10000000, 0x7FFFFFFF));
        return date + "-" + hex;
    }
}
