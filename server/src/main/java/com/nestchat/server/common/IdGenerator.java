package com.nestchat.server.common;

import java.util.UUID;

public class IdGenerator {

    public static String generate(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public static String timeBasedId(String prefix) {
        String ts = Long.toString(System.currentTimeMillis(), 36);
        String rand = Integer.toHexString((int) (Math.random() * 0xFFFF));
        return prefix + "_" + ts + "_" + rand;
    }
}
