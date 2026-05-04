package com.example.pms_nhom9.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.example.pms_nhom9.database.AppDatabase;
import com.example.pms_nhom9.models.Event;
import com.example.pms_nhom9.utils.EventAlarmScheduler;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !"android.intent.action.QUICKBOOT_POWERON".equals(action)) return;

        SharedPreferences prefs = context.getSharedPreferences("psm_prefs", Context.MODE_PRIVATE);
        int userId = prefs.getInt("logged_user_id", -1);
        if (userId == -1) return;

        // Lên lịch lại tất cả sự kiện trong tương lai có reminder
        Executors.newSingleThreadExecutor().execute(() -> {
            long now = System.currentTimeMillis();
            Calendar end = Calendar.getInstance();
            end.add(Calendar.DAY_OF_MONTH, 30);

            List<Event> events = AppDatabase.getInstance(context)
                    .eventDao().getEventsInRangeSync(userId, now, end.getTimeInMillis());

            for (Event event : events) {
                int reminderMin = event.getReminderMinutes();
                if (reminderMin > 0) {
                    EventAlarmScheduler.scheduleMinutes(context, event, reminderMin);
                }
            }
        });
    }
}
