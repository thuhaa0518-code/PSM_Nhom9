package com.example.pms_nhom9.receivers;

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

    public static final String CHANNEL_ID   = "psm_event_channel";
    public static final String CHANNEL_NAME = "Nhắc nhở sự kiện";

    @Override
    public void onReceive(Context context, Intent intent) {
        // WakeLock: đảm bảo CPU không sleep khi xử lý
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm != null
                ? pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PSM:NotifWakeLock")
                : null;
        if (wl != null) wl.acquire(10_000L);

        try {
            int    eventId       = intent.getIntExtra("event_id", -1);
            String title         = intent.getStringExtra("event_title");
            String location      = intent.getStringExtra("event_location");
            int    reminderValue = intent.getIntExtra("reminder_value", 30);
            int    reminderUnit  = intent.getIntExtra("reminder_unit", 0);

            if (title == null || eventId == -1) return;

            // Tăng badge count
            NotificationBadgeManager.increment(context);

            // Tạo channel trước
            createChannel(context);

            // Intent mở tab thông báo khi bấm vào notification
            Intent openIntent = new Intent(context, MainActivity.class);
            openIntent.putExtra("open_tab", "notification");
            openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent contentPi = PendingIntent.getActivity(
                    context, eventId, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Nội dung thông báo
            String timeLabel = buildTimeLabel(reminderValue, reminderUnit);
            String body = (location != null && !location.isEmpty())
                    ? "📍 " + location + " · " + timeLabel
                    : timeLabel;

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_nav_notification)
                    .setContentTitle("⏰ " + title)
                    .setContentText(body)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE)
                    .setAutoCancel(true)
                    .setContentIntent(contentPi)
                    // Heads-up: cần fullScreenIntent trên Android 10+
                    .setFullScreenIntent(contentPi, true);

            NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(eventId, builder.build());

        } finally {
            if (wl != null && wl.isHeld()) wl.release();
        }
    }

    private String buildTimeLabel(int value, int unit) {
        switch (unit) {
            case 1: return "Còn " + value + " giờ nữa";
            case 2: return "Còn " + value + " ngày nữa";
            default: return "Còn " + value + " phút nữa";
        }
    }

    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            // Xóa channel cũ nếu có để tạo lại với IMPORTANCE_HIGH
            nm.deleteNotificationChannel(CHANNEL_ID);

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Nhắc nhở trước khi sự kiện bắt đầu");
            channel.enableVibration(true);
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(channel);
        }
    }
}
