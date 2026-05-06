package com.example.pms_nhom9.receivers;

// ============================================================
// FILE: NotificationReceiver.java
// NGƯỜI PHỤ TRÁCH: NHƯ
// MỤC ĐÍCH: BroadcastReceiver nhận alarm và hiển thị notification lên màn hình
//
// CHỨC NĂNG CHÍNH:
//   - Nhận Intent từ AlarmManager khi đến giờ nhắc nhở
//   - Tăng badge count (số đỏ trên icon Notification)
//   - Tạo Notification Channel (bắt buộc từ Android 8+)
//   - Hiển thị heads-up notification với tiêu đề, địa điểm, thời gian
//   - Khi bấm vào notification → mở MainActivity tab Notification
//
// CÁCH HOẠT ĐỘNG:
//   1. EventAlarmScheduler.scheduleMinutes() đặt alarm vào AlarmManager
//   2. Khi đến giờ → hệ thống gọi onReceive() của receiver này
//   3. onReceive() đọc dữ liệu từ Intent (event_id, event_title, location...)
//   4. Tăng badge, tạo channel, hiện notification
//
// WAKELOCK:
//   - Giữ CPU hoạt động tối đa 10 giây khi xử lý
//   - Đảm bảo notification được hiển thị kể cả khi màn hình tắt
//   - Luôn release trong finally block để tránh memory leak
//
// NOTIFICATION CHANNEL (Android 8+):
//   - CHANNEL_ID: "psm_event_channel"
//   - IMPORTANCE_HIGH: hiển thị heads-up (popup trên màn hình)
//   - Xóa channel cũ trước khi tạo mới để đảm bảo cài đặt đúng
//
// HEADS-UP NOTIFICATION:
//   - setFullScreenIntent(contentPi, true): hiện popup kể cả khi màn hình tắt
//   - PRIORITY_MAX: ưu tiên cao nhất
//   - DEFAULT_SOUND | DEFAULT_VIBRATE: âm thanh + rung mặc định
//   - setAutoCancel(true): tự xóa khi người dùng bấm vào
// ============================================================

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.pms_nhom9.R;
import com.example.pms_nhom9.activities.MainActivity;
import com.example.pms_nhom9.utils.NotificationBadgeManager;

public class NotificationReceiver extends BroadcastReceiver {

    // ID và tên của Notification Channel
    // CHANNEL_ID phải nhất quán khi tạo channel và khi build notification
    public static final String CHANNEL_ID   = "psm_event_channel";
    public static final String CHANNEL_NAME = "Nhắc nhở sự kiện";

    @Override
    public void onReceive(Context context, Intent intent) {
        // ── WakeLock: giữ CPU hoạt động khi xử lý ────────────────────
        // PARTIAL_WAKE_LOCK: giữ CPU nhưng không bật màn hình
        // Cần thiết vì alarm có thể kích hoạt khi thiết bị đang ngủ
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm != null
                ? pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PSM:NotifWakeLock")
                : null;
        if (wl != null) wl.acquire(10_000L); // giữ tối đa 10 giây

        try {
            // ── Đọc dữ liệu từ Intent ─────────────────────────────────
            // Các extra này được truyền từ EventAlarmScheduler.buildPendingIntent()
            int    eventId       = intent.getIntExtra("event_id", -1);
            String title         = intent.getStringExtra("event_title");
            String location      = intent.getStringExtra("event_location");
            int    reminderValue = intent.getIntExtra("reminder_value", 30);
            int    reminderUnit  = intent.getIntExtra("reminder_unit", 0);

            // Bỏ qua nếu dữ liệu không hợp lệ
            if (title == null || eventId == -1) return;

            // ── Tăng badge count ──────────────────────────────────────
            // Số đỏ trên icon Notification ở bottom nav sẽ tăng lên 1
            NotificationBadgeManager.increment(context);

            // ── Tạo Notification Channel (bắt buộc Android 8+) ───────
            createChannel(context);

            // ── Intent mở tab Notification khi bấm vào notification ──
            Intent openIntent = new Intent(context, MainActivity.class);
            openIntent.putExtra("open_tab", "notification"); // MainActivity sẽ đọc extra này
            openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent contentPi = PendingIntent.getActivity(
                    context, eventId, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // ── Xây dựng nội dung notification ───────────────────────
            // buildTimeLabel(): "Còn 15 phút nữa" / "Còn 1 giờ nữa" / "Còn 1 ngày nữa"
            String timeLabel = buildTimeLabel(reminderValue, reminderUnit);
            // Nếu có địa điểm: "📍 Thư viện tầng 3 · Còn 15 phút nữa"
            // Nếu không: "Còn 15 phút nữa"
            String body = (location != null && !location.isEmpty())
                    ? "📍 " + location + " · " + timeLabel
                    : timeLabel;

            // ── Build và hiển thị notification ───────────────────────
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_nav_notification) // icon nhỏ trên status bar
                    .setContentTitle("⏰ " + title)               // tiêu đề: "⏰ Họp nhóm BTL"
                    .setContentText(body)                          // nội dung ngắn
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body)) // nội dung dài khi mở rộng
                    .setPriority(NotificationCompat.PRIORITY_MAX)  // ưu tiên cao nhất → heads-up
                    .setCategory(NotificationCompat.CATEGORY_REMINDER) // phân loại: nhắc nhở
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // hiện trên màn khóa
                    .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE) // âm thanh + rung
                    .setAutoCancel(true)          // tự xóa khi người dùng bấm vào
                    .setContentIntent(contentPi)  // action khi bấm vào notification
                    .setFullScreenIntent(contentPi, true); // heads-up kể cả khi màn hình tắt

            NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                // notify(id, notification): id = eventId để mỗi sự kiện có notification riêng
                // Nếu cùng id → notification mới sẽ thay thế notification cũ
                nm.notify(eventId, builder.build());
            }

        } finally {
            // ── Luôn release WakeLock dù có lỗi hay không ────────────
            // Nếu không release → pin sẽ cạn nhanh
            if (wl != null && wl.isHeld()) wl.release();
        }
    }

    // ── Tạo text mô tả thời gian nhắc nhở ────────────────────────────
    // Ví dụ: value=15, unit=0 → "Còn 15 phút nữa"
    //        value=1,  unit=1 → "Còn 1 giờ nữa"
    //        value=1,  unit=2 → "Còn 1 ngày nữa"
    private String buildTimeLabel(int value, int unit) {
        switch (unit) {
            case 1: return "Còn " + value + " giờ nữa";
            case 2: return "Còn " + value + " ngày nữa";
            default: return "Còn " + value + " phút nữa"; // unit=0 hoặc mặc định
        }
    }

    // ── Tạo Notification Channel ──────────────────────────────────────
    // Bắt buộc từ Android 8.0 (API 26 = Oreo)
    // Notification Channel xác định cách hiển thị notification (âm thanh, rung, ưu tiên...)
    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            // Xóa channel cũ trước khi tạo mới
            // Lý do: một số cài đặt channel (như IMPORTANCE) không thể thay đổi sau khi tạo
            // → xóa và tạo lại để đảm bảo IMPORTANCE_HIGH
            nm.deleteNotificationChannel(CHANNEL_ID);

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,                              // ID duy nhất
                    CHANNEL_NAME,                            // Tên hiển thị trong Settings
                    NotificationManager.IMPORTANCE_HIGH);    // IMPORTANCE_HIGH → hiện heads-up popup
            channel.setDescription("Nhắc nhở trước khi sự kiện bắt đầu");
            channel.enableVibration(true);  // bật rung
            channel.setShowBadge(true);     // hiện badge trên icon app
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC); // hiện trên màn khóa
            nm.createNotificationChannel(channel);
        }
        // Android < 8: không cần channel, notification hoạt động trực tiếp
    }
}
