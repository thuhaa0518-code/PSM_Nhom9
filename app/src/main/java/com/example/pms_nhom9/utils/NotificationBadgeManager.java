package com.example.pms_nhom9.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class NotificationBadgeManager {

    private static final String PREFS = "notif_badge_prefs";
    private static final String KEY_COUNT = "unread_count";

    public static void increment(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int current = prefs.getInt(KEY_COUNT, 0);
        prefs.edit().putInt(KEY_COUNT, current + 1).apply();
    }

    public static void reset(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putInt(KEY_COUNT, 0).apply();
    }

    public static int getCount(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_COUNT, 0);
    }
}
