package com.example.pms_nhom9.utils;

// ============================================================
// FILE: NotificationBadgeManager.java
// NGƯỜI PHỤ TRÁCH: NHƯ
// MỤC ĐÍCH: Quản lý số badge (chấm đỏ) trên icon Notification ở bottom nav
//
// CHỨC NĂNG CHÍNH:
//   - Lưu số thông báo chưa đọc vào SharedPreferences
//   - increment(): tăng số badge lên 1 (gọi khi alarm kích hoạt)
//   - reset(): đặt về 0 (gọi khi người dùng mở tab Notification)
//   - getCount(): lấy số hiện tại (gọi để hiển thị trên icon)
//
// CÁCH HOẠT ĐỘNG:
//   1. Alarm kích hoạt → NotificationReceiver.onReceive()
//      → gọi NotificationBadgeManager.increment()
//      → số badge tăng từ 0 → 1 → 2...
//   2. MainActivity.updateNotificationBadge()
//      → gọi getCount() → hiện số đỏ trên icon
//   3. Người dùng bấm tab Notification
//      → gọi reset() → số về 0 → badge ẩn đi
//
// LƯU TRỮ:
//   SharedPreferences file: "notif_badge_prefs"
//   Key: "unread_count" (int)
// ============================================================

import android.content.Context;
import android.content.SharedPreferences;

public class NotificationBadgeManager {

    // Tên file SharedPreferences riêng cho badge (tách biệt với "psm_prefs")
    private static final String PREFS = "notif_badge_prefs";
    // Key lưu số thông báo chưa đọc
    private static final String KEY_COUNT = "unread_count";

    /**
     * Tăng số badge lên 1.
     * Được gọi bởi NotificationReceiver mỗi khi alarm kích hoạt.
     * Ví dụ: badge đang là 2 → sau khi gọi increment() → badge = 3
     */
    public static void increment(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int current = prefs.getInt(KEY_COUNT, 0); // lấy số hiện tại, mặc định 0
        prefs.edit().putInt(KEY_COUNT, current + 1).apply(); // tăng lên 1 và lưu
    }

    /**
     * Đặt số badge về 0.
     * Được gọi khi người dùng mở tab Notification (đã xem thông báo).
     * Sau khi gọi reset() → badge biến mất khỏi icon.
     */
    public static void reset(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putInt(KEY_COUNT, 0).apply();
    }

    /**
     * Lấy số badge hiện tại.
     * Được gọi bởi MainActivity.updateNotificationBadge() để hiển thị số trên icon.
     * Trả về 0 nếu chưa có thông báo nào.
     */
    public static int getCount(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_COUNT, 0); // mặc định 0 nếu chưa có giá trị
    }
}
