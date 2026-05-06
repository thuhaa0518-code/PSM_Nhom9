package com.example.pms_nhom9.activities;

// ============================================================
// FILE: MainActivity.java
// NGƯỜI PHỤ TRÁCH: LY
// MỤC ĐÍCH: Activity chính của app, chứa Bottom Navigation và FAB
//
// CHỨC NĂNG CHÍNH:
//   - Quản lý Bottom Navigation: Home | Calendar | Notification | Settings
//   - FAB (+) ở giữa bottom nav → mở AddEventActivity tạo sự kiện mới
//   - Hiển thị badge số thông báo chưa đọc trên icon Notification
//   - Xin quyền POST_NOTIFICATIONS (Android 13+) và SCHEDULE_EXACT_ALARM (Android 12+)
//   - Tự động sync dữ liệu từ server về Room DB mỗi khi vào app (onResume)
//   - Xử lý mở từ notification → tự động chuyển sang tab Notification
//
// CẤU TRÚC LAYOUT (activity_main.xml):
//   - FrameLayout (id: fragmentContainer): chứa các Fragment
//   - BottomNavigationView (id: bottomNav): thanh điều hướng dưới
//   - FloatingActionButton (id: fabAddEvent): nút + ở giữa
//
// LUỒNG ĐIỀU HƯỚNG:
//   Bấm tab Home → loadFragment(HomeFragment)
//   Bấm tab Calendar → loadFragment(CalendarFragment)
//   Bấm tab Notification → reset badge + loadFragment(NotificationFragment)
//   Bấm tab Settings → loadFragment(SettingsFragment)
//   Bấm FAB → startActivityForResult(AddEventActivity, REQUEST_ADD_EVENT=100)
//   Khi AddEventActivity trả về RESULT_OK → reload HomeFragment
// ============================================================

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

    private BottomNavigationView bottomNav; // thanh điều hướng dưới cùng
    private FloatingActionButton fabAddEvent; // nút + tạo sự kiện mới

    // Request codes để phân biệt kết quả từ các Activity khác nhau
    private static final int REQUEST_ADD_EVENT       = 100; // mã cho AddEventActivity
    private static final int REQUEST_NOTIF_PERMISSION = 101; // mã xin quyền thông báo

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Khi AddEventActivity đóng và trả về RESULT_OK (lưu thành công)
        // → reload HomeFragment để hiển thị sự kiện mới
        if (requestCode == REQUEST_ADD_EVENT && resultCode == RESULT_OK) {
            loadFragment(new HomeFragment());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Gắn layout activity_main.xml (có bottom nav + fragment container + FAB)
        setContentView(R.layout.activity_main);

        // Xin quyền thông báo và exact alarm ngay khi mở app
        requestNotificationPermission();

        // Ánh xạ view
        bottomNav   = findViewById(R.id.bottomNav);
        fabAddEvent = findViewById(R.id.fabAddEvent);

        // ── Kiểm tra nếu mở từ notification ──────────────────────────
        // NotificationReceiver truyền extra "open_tab" = "notification"
        // khi người dùng bấm vào notification
        String openTab = getIntent().getStringExtra("open_tab");
        if ("notification".equals(openTab)) {
            loadFragment(new NotificationFragment());
            bottomNav.setSelectedItemId(R.id.nav_notification); // highlight tab notification
        } else {
            loadFragment(new HomeFragment()); // mặc định mở tab Home
        }

        // ── Xử lý click bottom navigation ────────────────────────────
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
                // Reset badge về 0 khi người dùng mở tab thông báo
                // → xóa số đỏ trên icon
                NotificationBadgeManager.reset(this);
                updateNotificationBadge(); // cập nhật UI badge ngay lập tức
                loadFragment(new NotificationFragment());
                return true;
            }
            if (id == R.id.nav_settings) {
                loadFragment(new SettingsFragment());
                return true;
            }
            return false;
        });

        // ── FAB: mở màn tạo sự kiện mới ──────────────────────────────
        fabAddEvent.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEventActivity.class);
            // startActivityForResult: chờ kết quả từ AddEventActivity
            // khi đóng sẽ gọi onActivityResult() ở trên
            startActivityForResult(intent, REQUEST_ADD_EVENT);
        });

        // Hiển thị badge ngay khi vào app (có thể có thông báo từ lần trước)
        updateNotificationBadge();
    }

    // ── Cập nhật badge số thông báo trên icon ────────────────────────
    // Được gọi từ: onCreate, onResume, NotificationFragment (khi reset)
    public void updateNotificationBadge() {
        int count = NotificationBadgeManager.getCount(this); // lấy số từ SharedPreferences
        BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_notification);
        badge.setBackgroundColor(0xFFEF4444); // màu đỏ
        badge.setBadgeTextColor(0xFFFFFFFF);  // chữ trắng
        if (count > 0) {
            badge.setVisible(true);
            badge.setNumber(count); // hiện số: "3", "5"...
        } else {
            badge.clearNumber();
            badge.setVisible(false); // ẩn badge nếu = 0
        }
    }

    // ── Xin quyền hệ thống ───────────────────────────────────────────
    private void requestNotificationPermission() {
        // Android 13+ (TIRAMISU) yêu cầu xin quyền POST_NOTIFICATIONS tường minh
        // Các phiên bản cũ hơn không cần xin
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Hiện dialog hỏi người dùng có cho phép thông báo không
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIF_PERMISSION);
            }
        }
        // Android 12+ (S) yêu cầu quyền đặt exact alarm
        // Nếu không có quyền → mở Settings để người dùng cấp
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.AlarmManager am =
                    (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                // Mở trang Settings → "Alarms & reminders" để cấp quyền
                Intent intent = new Intent(
                        android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Cập nhật badge mỗi khi quay lại app (có thể có alarm mới kích hoạt)
        updateNotificationBadge();
        // Sync events từ API về Room DB mỗi khi vào app
        // null = không cần callback sau khi sync xong
        com.example.pms_nhom9.api.SyncManager.syncAll(this, null);
    }

    // ── Thay thế Fragment trong container ────────────────────────────
    // FragmentManager quản lý vòng đời các Fragment
    // replace() thay thế Fragment hiện tại bằng Fragment mới
    // commit() xác nhận transaction
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment) // thay thế vào FrameLayout
                .commit();
    }
}
