package com.example.pms_nhom9.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.pms_nhom9.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MoodCheckInHelper {

    private static final String KEY_LAST_MOOD_DATE = "last_mood_date";
    private static final String DATE_FMT = "yyyy-MM-dd";

    /** Trả về true nếu hôm nay chưa check-in mood */
    public static boolean shouldShow(Context ctx) {
        String today = new SimpleDateFormat(DATE_FMT, Locale.getDefault())
                .format(new Date());
        String last = ctx.getSharedPreferences("psm_prefs", Context.MODE_PRIVATE)
                .getString(KEY_LAST_MOOD_DATE, "");
        return !today.equals(last);
    }

    /** Lưu ngày hôm nay đã check-in */
    public static void markDone(Context ctx) {
        String today = new SimpleDateFormat(DATE_FMT, Locale.getDefault())
                .format(new Date());
        ctx.getSharedPreferences("psm_prefs", Context.MODE_PRIVATE)
                .edit().putString(KEY_LAST_MOOD_DATE, today).apply();
    }

    /** Hiện dialog mood check-in, gọi onDone khi xong */
    public static void show(Context ctx, Runnable onDone) {
        Dialog dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_mood_checkin);
        dialog.setCancelable(false);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (ctx.getResources().getDisplayMetrics().widthPixels * 0.9f),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Lấy tên người dùng
        String name = ctx.getSharedPreferences("psm_prefs", Context.MODE_PRIVATE)
                .getString("logged_user_name", "bạn");
        String greeting = buildGreeting(name);

        TextView tvGreeting  = dialog.findViewById(R.id.tvMoodGreeting);
        TextView tvFeedback  = dialog.findViewById(R.id.tvMoodFeedback);
        View btnStart        = dialog.findViewById(R.id.btnStartDay);

        tvGreeting.setText(greeting);

        // Mood options
        LinearLayout[] moodLayouts = {
                dialog.findViewById(R.id.moodTired),
                dialog.findViewById(R.id.moodNormal),
                dialog.findViewById(R.id.moodGood),
                dialog.findViewById(R.id.moodFire)
        };
        TextView[] moodIcons = {
                dialog.findViewById(R.id.iconTired),
                dialog.findViewById(R.id.iconNormal),
                dialog.findViewById(R.id.iconGood),
                dialog.findViewById(R.id.iconFire)
        };
        String[] feedbacks = {
                "Hãy nghỉ ngơi đủ giấc nhé! App sẽ nhắc bạn những việc quan trọng nhất 💤",
                "Ngày bình thường cũng là một ngày tốt! Hãy hoàn thành từng việc nhỏ 😌",
                "Tốt lắm! App sẽ xếp các task quan trọng vào khung giờ vàng của bạn 😊",
                "Tuyệt vời! Hôm nay là ngày để chinh phục mọi mục tiêu! 🔥"
        };

        final int[] selected = {-1};

        for (int i = 0; i < moodLayouts.length; i++) {
            final int idx = i;
            moodLayouts[i].setOnClickListener(v -> {
                selected[0] = idx;
                // Reset tất cả
                for (TextView icon : moodIcons) icon.setBackground(null);
                // Highlight selected
                moodIcons[idx].setBackgroundResource(R.drawable.bg_mood_selected);
                // Hiện feedback
                tvFeedback.setText(feedbacks[idx]);
                tvFeedback.setVisibility(View.VISIBLE);
            });
        }

        btnStart.setOnClickListener(v -> {
            markDone(ctx);
            dialog.dismiss();
            if (onDone != null) onDone.run();
        });

        dialog.show();
    }

    private static String buildGreeting(String name) {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String timeGreet;
        if (hour < 12) timeGreet = "Chào buổi sáng";
        else if (hour < 18) timeGreet = "Chào buổi chiều";
        else timeGreet = "Chào buổi tối";
        // Lấy tên đầu tiên
        String firstName = name.trim().contains(" ")
                ? name.trim().substring(name.trim().lastIndexOf(' ') + 1)
                : name.trim();
        return timeGreet + ", " + firstName + "!";
    }
}
