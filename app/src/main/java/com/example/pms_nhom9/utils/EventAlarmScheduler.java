package com.example.pms_nhom9.utils;

// ============================================================
// FILE: EventAlarmScheduler.java
// NGƯỜI PHỤ TRÁCH: NHƯ
// MỤC ĐÍCH: Lên lịch, hủy và snooze alarm nhắc nhở sự kiện
//
// CHỨC NĂNG CHÍNH:
//   - schedule(): lên lịch alarm trước X phút/giờ/ngày
//   - scheduleMinutes(): lên lịch với tổng số phút đã quy đổi
//   - cancel(): hủy alarm của một sự kiện
//   - snooze(): lên lịch lại alarm sau X phút kể từ bây giờ
//
// CÁCH HOẠT ĐỘNG:
//   1. Người dùng tạo sự kiện với nhắc nhở "15 phút trước"
//   2. AddEventActivity gọi scheduleMinutes(event, 15)
//   3. triggerAt = startTime - 15 * 60 * 1000 (15 phút trước giờ bắt đầu)
//   4. AlarmManager đặt alarm tại thời điểm triggerAt
//   5. Khi đến giờ → hệ thống gọi NotificationReceiver.onReceive()
//   6. NotificationReceiver hiện notification lên màn hình
//
// LƯU Ý ANDROID VERSIONS:
//   - Android 12+ (S): cần quyền SCHEDULE_EXACT_ALARM
//     → nếu không có quyền: dùng inexact alarm (có thể trễ vài phút)
//   - Android < 12: luôn dùng exact alarm
//   - RTC_WAKEUP: đánh thức thiết bị kể cả khi đang ngủ (màn hình tắt)
//   - setExactAndAllowWhileIdle: hoạt động kể cả khi thiết bị ở chế độ Doze
//
// PENDING INTENT:
//   - requestCode = event.getId(): mỗi sự kiện có 1 alarm riêng biệt
//   - FLAG_UPDATE_CURRENT: nếu alarm đã tồn tại → cập nhật data mới
//   - FLAG_IMMUTABLE: bắt buộc từ Android 12+
// ============================================================

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.pms_nhom9.models.Event;
import com.example.pms_nhom9.receivers.NotificationReceiver;

public class EventAlarmScheduler {

    /**
     * Lên lịch alarm theo giá trị và đơn vị người dùng chọn.
     *
     * @param ctx           Context của Activity/Fragment gọi
     * @param event         Sự kiện cần nhắc nhở
     * @param reminderValue Số lượng (ví dụ: 15)
     * @param reminderUnit  Đơn vị: 0=Phút, 1=Giờ, 2=Ngày
     *
     * Ví dụ: schedule(ctx, event, 15, 0) → nhắc trước 15 phút
     *        schedule(ctx, event, 1, 1)  → nhắc trước 1 giờ
     *        schedule(ctx, event, 1, 2)  → nhắc trước 1 ngày
     */
    public static void schedule(Context ctx, Event event,
                                int reminderValue, int reminderUnit) {
        // Không lên lịch nếu sự kiện đã kết thúc (tránh alarm vô nghĩa)
        if (event.getEndTime() <= System.currentTimeMillis()) return;

        long offsetMs  = toMillis(reminderValue, reminderUnit); // đổi sang milliseconds
        long triggerAt = event.getStartTime() - offsetMs;       // thời điểm kích hoạt alarm
        setAlarm(ctx, event, triggerAt, reminderValue, reminderUnit);
    }

    /**
     * Lên lịch mặc định 30 phút trước khi sự kiện bắt đầu.
     * Dùng khi không có cài đặt reminder cụ thể.
     */
    public static void schedule(Context ctx, Event event) {
        schedule(ctx, event, 30, 0); // 30 phút, đơn vị Phút
    }

    /**
     * Lên lịch với tổng số phút đã quy đổi sẵn.
     * Được gọi từ AddEventActivity sau khi tính totalMinutes từ value + unit.
     *
     * @param totalMinutes Tổng số phút trước khi sự kiện bắt đầu
     *
     * Ví dụ: totalMinutes=60 → nhắc trước 1 giờ
     *        totalMinutes=1440 → nhắc trước 1 ngày
     */
    public static void scheduleMinutes(Context ctx, Event event, int totalMinutes) {
        // Không lên lịch nếu sự kiện đã kết thúc
        if (event.getEndTime() <= System.currentTimeMillis()) return;

        // triggerAt = giờ bắt đầu sự kiện - X phút (đổi sang ms)
        // 60L * 1000 = 60000ms = 1 phút (dùng 60L để tránh overflow int)
        long triggerAt = event.getStartTime() - totalMinutes * 60L * 1000;
        // Nếu triggerAt đã qua (nhắc trong quá khứ) nhưng sự kiện chưa kết thúc
        // → AlarmManager sẽ fire ngay lập tức
        setAlarm(ctx, event, triggerAt, totalMinutes, 0);
    }

    /**
     * Hủy alarm của một sự kiện.
     * Được gọi trước khi xóa sự kiện hoặc trước khi cập nhật alarm mới.
     */
    public static void cancel(Context ctx, Event event) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(ctx, NotificationReceiver.class);
        // FLAG_NO_CREATE: chỉ tìm PendingIntent đã tồn tại, KHÔNG tạo mới
        // Nếu không tìm thấy → trả về null (không có alarm để hủy)
        PendingIntent pi = PendingIntent.getBroadcast(
                ctx, event.getId(), intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) am.cancel(pi); // hủy alarm nếu tìm thấy
    }

    /**
     * Snooze: lên lịch lại alarm sau X phút kể từ BÂY GIỜ.
     * Khác với schedule(): snooze tính từ thời điểm hiện tại, không phải từ startTime.
     *
     * @param minutes Số phút snooze (5, 10, hoặc 15)
     */
    public static void snooze(Context ctx, Event event, int minutes) {
        // triggerAt = bây giờ + X phút
        long triggerAt = System.currentTimeMillis() + minutes * 60 * 1000L;
        setAlarm(ctx, event, triggerAt, minutes, 0);
    }

    // ── Private helpers ───────────────────────────────────────────────

    /**
     * Đặt alarm thực sự vào AlarmManager.
     * Xử lý khác biệt giữa các phiên bản Android.
     */
    private static void setAlarm(Context ctx, Event event, long triggerAt,
                                  int reminderValue, int reminderUnit) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        PendingIntent pi = buildPendingIntent(ctx, event, reminderValue, reminderUnit);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+: kiểm tra quyền trước khi đặt exact alarm
            if (am.canScheduleExactAlarms()) {
                // Có quyền → đặt exact alarm (chính xác đến giây)
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                // Không có quyền → dùng inexact alarm (có thể trễ vài phút)
                // Vẫn hoạt động nhưng không chính xác 100%
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } else {
            // Android < 12: luôn dùng exact alarm, không cần xin quyền
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
        // RTC_WAKEUP: dùng đồng hồ thực (real-time clock) và đánh thức thiết bị
        // setExactAndAllowWhileIdle: hoạt động kể cả khi thiết bị ở chế độ Doze (tiết kiệm pin)
    }

    /**
     * Tạo PendingIntent chứa thông tin sự kiện để gửi cho NotificationReceiver.
     * requestCode = event.getId() đảm bảo mỗi sự kiện có alarm riêng biệt.
     */
    private static PendingIntent buildPendingIntent(Context ctx, Event event,
                                                     int reminderValue, int reminderUnit) {
        Intent intent = new Intent(ctx, NotificationReceiver.class);
        // Truyền dữ liệu sự kiện vào Intent để NotificationReceiver dùng khi hiện notification
        intent.putExtra("event_id",       event.getId());           // ID để tăng badge
        intent.putExtra("event_title",    event.getTitle());        // Tiêu đề notification
        intent.putExtra("event_location", event.getLocation() != null ? event.getLocation() : "");
        intent.putExtra("event_start",    event.getStartTime());    // Giờ bắt đầu
        intent.putExtra("reminder_value", reminderValue);           // Số lượng nhắc
        intent.putExtra("reminder_unit",  reminderUnit);            // Đơn vị nhắc

        return PendingIntent.getBroadcast(
                ctx,
                event.getId(),          // requestCode = eventId: mỗi sự kiện 1 alarm riêng
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                // FLAG_UPDATE_CURRENT: nếu alarm đã tồn tại → cập nhật data mới
                // FLAG_IMMUTABLE: bắt buộc từ Android 12+
        );
    }

    /**
     * Chuyển đổi value + unit sang milliseconds.
     * Dùng để tính offset trước giờ bắt đầu sự kiện.
     */
    private static long toMillis(int value, int unit) {
        switch (unit) {
            case 1: return value * 60L * 60 * 1000;       // Giờ: value * 3,600,000 ms
            case 2: return value * 24L * 60 * 60 * 1000;  // Ngày: value * 86,400,000 ms
            default: return value * 60L * 1000;            // Phút: value * 60,000 ms
        }
    }
}
