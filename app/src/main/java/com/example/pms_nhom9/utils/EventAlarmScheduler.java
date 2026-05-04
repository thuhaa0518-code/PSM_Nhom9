package com.example.pms_nhom9.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.pms_nhom9.models.Event;
import com.example.pms_nhom9.receivers.NotificationReceiver;

public class EventAlarmScheduler {

    /**
     * Lên lịch alarm theo thời gian nhắc nhở người dùng chọn.
     * @param reminderValue  số lượng (vd: 10)
     * @param reminderUnit   đơn vị: 0=Phút, 1=Giờ, 2=Ngày
     */
    public static void schedule(Context ctx, Event event,
                                int reminderValue, int reminderUnit) {
        // Không nhắc nếu sự kiện đã kết thúc
        if (event.getEndTime() <= System.currentTimeMillis()) return;
        long offsetMs = toMillis(reminderValue, reminderUnit);
        long triggerAt = event.getStartTime() - offsetMs;
        setAlarm(ctx, event, triggerAt, reminderValue, reminderUnit);
    }

    /** Lên lịch mặc định 30 phút trước (dùng khi không có reminder setting) */
    public static void schedule(Context ctx, Event event) {
        schedule(ctx, event, 30, 0);
    }

    /** Lên lịch theo số phút cụ thể (đã quy đổi) */
    public static void scheduleMinutes(Context ctx, Event event, int totalMinutes) {
        // Không nhắc nếu sự kiện đã kết thúc
        if (event.getEndTime() <= System.currentTimeMillis()) return;
        long triggerAt = event.getStartTime() - totalMinutes * 60L * 1000;
        // Nếu thời điểm nhắc đã qua nhưng sự kiện chưa kết thúc → fire ngay
        setAlarm(ctx, event, triggerAt, totalMinutes, 0);
    }

    /** Huỷ alarm */
    public static void cancel(Context ctx, Event event) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        // Dùng FLAG_NO_CREATE để match bất kỳ PendingIntent nào với requestCode = event.getId()
        Intent intent = new Intent(ctx, NotificationReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                ctx, event.getId(), intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) am.cancel(pi);
    }

    /** Snooze: nhắc lại sau X phút */
    public static void snooze(Context ctx, Event event, int minutes) {
        long triggerAt = System.currentTimeMillis() + minutes * 60 * 1000L;
        setAlarm(ctx, event, triggerAt, minutes, 0);
    }

    // ── private helpers ────────────────────────────────────────────────

    private static void setAlarm(Context ctx, Event event, long triggerAt,
                                  int reminderValue, int reminderUnit) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = buildPendingIntent(ctx, event, reminderValue, reminderUnit);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                // Fallback: inexact alarm (có thể trễ vài phút nhưng vẫn fire)
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    private static PendingIntent buildPendingIntent(Context ctx, Event event,
                                                     int reminderValue, int reminderUnit) {
        Intent intent = new Intent(ctx, NotificationReceiver.class);
        intent.putExtra("event_id",       event.getId());
        intent.putExtra("event_title",    event.getTitle());
        intent.putExtra("event_location", event.getLocation() != null ? event.getLocation() : "");
        intent.putExtra("event_start",    event.getStartTime());
        intent.putExtra("reminder_value", reminderValue);
        intent.putExtra("reminder_unit",  reminderUnit);
        return PendingIntent.getBroadcast(
                ctx,
                event.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static long toMillis(int value, int unit) {
        switch (unit) {
            case 1: return value * 60L * 60 * 1000;       // Giờ
            case 2: return value * 24L * 60 * 60 * 1000;  // Ngày
            default: return value * 60L * 1000;            // Phút
        }
    }
}
