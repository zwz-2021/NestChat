package com.nestchat.server.security;

public class UserContext {

    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();

    public static void set(String userId) {
        USER_ID.set(userId);
    }

    public static String get() {
        return USER_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
    }
}
