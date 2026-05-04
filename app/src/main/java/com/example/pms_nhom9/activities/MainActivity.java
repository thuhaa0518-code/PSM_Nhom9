package com.example.pms_nhom9.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.pms_nhom9.R;
import com.example.pms_nhom9.fragments.CalendarFragment;
import com.example.pms_nhom9.fragments.HomeFragment;
import com.example.pms_nhom9.fragments.NotificationFragment;
import com.example.pms_nhom9.fragments.ProfileFragment;
import com.example.pms_nhom9.fragments.SettingsFragment;
import com.example.pms_nhom9.utils.NotificationBadgeManager;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private FloatingActionButton fabAddEvent;
    private static final int REQUEST_ADD_EVENT = 100;
    private static final int REQUEST_NOTIF_PERMISSION = 101;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_EVENT && resultCode == RESULT_OK) {
            loadFragment(new HomeFragment());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestNotificationPermission();

        bottomNav   = findViewById(R.id.bottomNav);
        fabAddEvent = findViewById(R.id.fabAddEvent);

        // Kiểm tra nếu mở từ notification → chuyển sang tab thông báo
        String openTab = getIntent().getStringExtra("open_tab");
        if ("notification".equals(openTab)) {
            loadFragment(new NotificationFragment());
            bottomNav.setSelectedItemId(R.id.nav_notification);
        } else {
            loadFragment(new HomeFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                loadFragment(new HomeFragment());
                return true;
            }
            if (id == R.id.nav_calendar) {
                loadFragment(new CalendarFragment());
                return true;
            }
            if (id == R.id.nav_notification) {
                // Reset badge khi mở tab thông báo
                NotificationBadgeManager.reset(this);
                updateNotificationBadge();
                loadFragment(new NotificationFragment());
                return true;
            }
            if (id == R.id.nav_settings) {
                loadFragment(new SettingsFragment());
                return true;
            }
            return false;
        });

        fabAddEvent.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEventActivity.class);
            startActivityForResult(intent, REQUEST_ADD_EVENT);
        });

        // Hiển thị badge ngay khi vào app
        updateNotificationBadge();
    }

    public void updateNotificationBadge() {
        int count = NotificationBadgeManager.getCount(this);
        BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_notification);
        badge.setBackgroundColor(0xFFEF4444);
        badge.setBadgeTextColor(0xFFFFFFFF);
        if (count > 0) {
            badge.setVisible(true);
            badge.setNumber(count);
        } else {
            badge.clearNumber();
            badge.setVisible(false);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIF_PERMISSION);
            }
        }
        // Android 12+ (S): xin quyền đặt exact alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.AlarmManager am =
                    (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                Intent intent = new Intent(
                        android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNotificationBadge();
        // Sync events từ API về Room mỗi khi vào app
        com.example.pms_nhom9.api.SyncManager.syncAll(this, null);
        // Hiện mood check-in lần đầu trong ngày
        // Tạm tắt để debug crash
        // if (com.example.pms_nhom9.utils.MoodCheckInHelper.shouldShow(this)) {
        //     com.example.pms_nhom9.utils.MoodCheckInHelper.show(this, null);
        // }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
