package com.example.nestchat.api;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {

    private static final String PREF_NAME = "nestchat_auth";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_ACCOUNT = "account";
    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_AVATAR_URL = "avatar_url";

    private static SharedPreferences prefs;

    public static void init(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void saveLoginInfo(String accessToken, String refreshToken,
                                     String userId, String account,
                                     String nickname, String avatarUrl) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_ACCOUNT, account)
                .putString(KEY_NICKNAME, nickname)
                .putString(KEY_AVATAR_URL, avatarUrl)
                .apply();
    }

    public static String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, "");
    }

    public static String getUserId() {
        return prefs.getString(KEY_USER_ID, "");
    }

    public static String getAccount() {
        return prefs.getString(KEY_ACCOUNT, "");
    }

    public static String getNickname() {
        return prefs.getString(KEY_NICKNAME, "");
    }

    public static String getAvatarUrl() {
        return prefs.getString(KEY_AVATAR_URL, "");
    }

    public static boolean isLoggedIn() {
        return !getAccessToken().isEmpty();
    }

    public static void clear() {
        prefs.edit().clear().apply();
    }
}
