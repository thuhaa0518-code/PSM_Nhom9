package com.example.pms_nhom9.activities;


import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.pms_nhom9.R;
import com.example.pms_nhom9.database.AppDatabase;
import com.example.pms_nhom9.database.EventDao;
import com.example.pms_nhom9.models.Event;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class EventDetailActivity extends AppCompatActivity {

    // Key để truyền event ID qua Intent
    public static final String EXTRA_EVENT_ID = "extra_event_id";

    private TextView tvTitle, tvDate, tvTime, tvLocation,
            tvRepeat, tvReminder, tvPriorityBadge;
    private EditText etNote;
    private SwitchCompat switchCompleted;
    private LinearLayout cardColorBanner, rowLocation, rowRepeat;
    private Button btnEdit, btnFocusMode;
    private ImageButton btnBack, btnDelete;

    private EventDao eventDao;
    private Event currentEvent;

    private final SimpleDateFormat dateFmt =
            new SimpleDateFormat("EEEE, d/M/yyyy", new Locale("vi", "VN"));
    private final SimpleDateFormat timeFmt =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        eventDao = AppDatabase.getInstance(this).eventDao();

        bindViews();

        // Lấy event ID từ Intent
        int eventId = getIntent().getIntExtra(EXTRA_EVENT_ID, -1);
        if (eventId == -1) {
            Toast.makeText(this, "Không tìm thấy sự kiện", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadEvent(eventId);
    }

    private void bindViews() {
        btnBack          = findViewById(R.id.btnBack);
        btnDelete        = findViewById(R.id.btnDelete);
        tvTitle          = findViewById(R.id.tvDetailTitle);
        tvDate           = findViewById(R.id.tvDetailDate);
        tvTime           = findViewById(R.id.tvDetailTime);
        tvLocation       = findViewById(R.id.tvDetailLocation);
        tvRepeat         = findViewById(R.id.tvDetailRepeat);
        tvReminder       = findViewById(R.id.tvDetailReminder);
        tvPriorityBadge  = findViewById(R.id.tvPriorityBadge);
        etNote           = findViewById(R.id.etDetailNote);
        switchCompleted  = findViewById(R.id.switchCompleted);
        cardColorBanner  = findViewById(R.id.cardColorBanner);
        rowLocation      = findViewById(R.id.rowLocation);
        rowRepeat        = findViewById(R.id.rowRepeat);
        btnEdit          = findViewById(R.id.btnEdit);
        btnFocusMode     = findViewById(R.id.btnFocusMode);

        btnBack.setOnClickListener(v -> finish());
    }

    // Tải sự kiện từ API theo ID
    private void loadEvent(int eventId) {
        // Lấy từ API — dùng getEvents với range rộng rồi filter theo id
        // Hoặc đơn giản hơn: vẫn dùng Room local làm cache
        // Ở đây dùng Room để load nhanh, API sync ở background
        Executors.newSingleThreadExecutor().execute(() -> {
            currentEvent = eventDao.getEventById(eventId);
            runOnUiThread(() -> {
                if (currentEvent == null) {
                    Toast.makeText(this, "Không tìm thấy sự kiện", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                populateUI();
                setupListeners();
            });
        });
    }

    // Đổ dữ liệu lên UI
    private void populateUI() {
        // Tên sự kiện
        tvTitle.setText(currentEvent.getTitle());

        // Gạch ngang nếu đã hoàn thành
        if (currentEvent.isCompleted()) {
            tvTitle.setPaintFlags(
                    tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            switchCompleted.setChecked(true);
        }

        // Banner màu
        try {
            cardColorBanner.setBackgroundColor(
                    Color.parseColor(currentEvent.getColor()));
        } catch (Exception ignored) {}

        // Badge ưu tiên
        String[] priorityLabels = {"Thấp", "Trung bình", "Cao"};
        int[]    priorityColors  = {0xFF1D9E75, 0xFFEF9F27, 0xFFE24B4A};
        int p = currentEvent.getPriority();
        tvPriorityBadge.setText(priorityLabels[p]);
        tvPriorityBadge.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(priorityColors[p]));

        // Ngày
        tvDate.setText(dateFmt.format(new Date(currentEvent.getStartTime())));

        // Giờ + thời lượng
        long diffMs  = currentEvent.getEndTime() - currentEvent.getStartTime();
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs);
        String duration;
        if (minutes < 60) {
            duration = minutes + " phút";
        } else {
            long h = minutes / 60, m = minutes % 60;
            duration = m > 0 ? h + " tiếng " + m + " phút" : h + " tiếng";
        }
        tvTime.setText(
                timeFmt.format(new Date(currentEvent.getStartTime())) +
                        " → " +
                        timeFmt.format(new Date(currentEvent.getEndTime())) +
                        " (" + duration + ")"
        );

        // Địa điểm
        String loc = currentEvent.getLocation();
        if (loc != null && !loc.isEmpty()) {
            rowLocation.setVisibility(View.VISIBLE);
            tvLocation.setText(loc);
        }

        // Lặp lại
        if (currentEvent.isRepeat() &&
                currentEvent.getRepeatDays() != null &&
                !currentEvent.getRepeatDays().isEmpty()) {
            rowRepeat.setVisibility(View.VISIBLE);
            tvRepeat.setText("Lặp lại: " +
                    formatRepeatDays(currentEvent.getRepeatDays()));
        }

        // Ghi chú
        if (currentEvent.getNote() != null) {
            etNote.setText(currentEvent.getNote());
        }

        // Nhắc nhở
        int reminderMin = currentEvent.getReminderMinutes();
        if (reminderMin > 0) {
            String reminderText;
            if (reminderMin % (60 * 24) == 0) {
                reminderText = "Trước " + (reminderMin / (60 * 24)) + " ngày";
            } else if (reminderMin % 60 == 0) {
                reminderText = "Trước " + (reminderMin / 60) + " giờ";
            } else {
                reminderText = "Trước " + reminderMin + " phút";
            }
            tvReminder.setText(reminderText);
            tvReminder.setTextColor(0xFF111827);
        } else {
            tvReminder.setText("Chưa bật nhắc nhở");
            tvReminder.setTextColor(0xFF9CA3AF);
        }
    }

    private void setupListeners() {
        // Toggle hoàn thành
        switchCompleted.setOnCheckedChangeListener((btn, isChecked) -> {
            currentEvent.setCompleted(isChecked);

            // Gạch ngang tên
            if (isChecked) {
                tvTitle.setPaintFlags(
                        tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                tvTitle.setPaintFlags(
                        tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            }

            // Lưu trạng thái
            saveCompleted();
        });

        // Nút Chỉnh sửa → mở AddEventActivity ở chế độ edit
        btnEdit.setOnClickListener(v -> {
            // Lưu ghi chú trước khi rời
            saveNote();
            Intent intent = new Intent(this, AddEventActivity.class);
            intent.putExtra(AddEventActivity.EXTRA_EDIT_EVENT_ID,
                    currentEvent.getId());
            startActivityForResult(intent, 200);
        });

        // Nút xóa
        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog());

        // Nút Focus Mode
        btnFocusMode.setOnClickListener(v -> {
            Intent intent = new Intent(this, FocusModeActivity.class);
            intent.putExtra(FocusModeActivity.EXTRA_EVENT_TITLE,
                    currentEvent.getTitle());
            startActivity(intent);
        });
    }

    // Dialog xác nhận xóa
    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa sự kiện")
                .setMessage("Bạn có chắc muốn xóa \"" +
                        currentEvent.getTitle() + "\" không?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteEvent())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteEvent() {
        // Huỷ alarm trước
        com.example.pms_nhom9.utils.EventAlarmScheduler.cancel(this, currentEvent);

        new com.example.pms_nhom9.api.ApiEventRepository(this)
                .deleteEvent(currentEvent.getId(),
                        new com.example.pms_nhom9.api.ApiEventRepository.Callback<String>() {
                    @Override public void onSuccess(String data) {
                        // Xoá trong Room local DB
                        final Event toDelete = currentEvent;
                        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                            eventDao.deleteEvent(toDelete);
                            runOnUiThread(() -> {
                                Toast.makeText(EventDetailActivity.this,
                                        "Đã xóa sự kiện", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            });
                        });
                    }
                    @Override public void onError(String message) {
                        // API lỗi (offline hoặc server lỗi) → vẫn xoá local
                        final Event toDelete = currentEvent;
                        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                            eventDao.deleteEvent(toDelete);
                            runOnUiThread(() -> {
                                Toast.makeText(EventDetailActivity.this,
                                        "Đã xóa sự kiện", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            });
                        });
                    }
                });
    }

    private void saveCompleted() {
        new com.example.pms_nhom9.api.ApiEventRepository(this)
                .updateEvent(currentEvent, new com.example.pms_nhom9.api.ApiEventRepository.Callback<com.example.pms_nhom9.models.Event>() {
                    @Override public void onSuccess(com.example.pms_nhom9.models.Event data) {}
                    @Override public void onError(String message) {}
                });
    }

    private void saveNote() {
        String note = etNote.getText().toString().trim();
        if (!note.equals(currentEvent.getNote())) {
            currentEvent.setNote(note);
            saveCompleted();
        }
    }

    // Khi quay về từ màn Edit
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK) {
            // Reload lại dữ liệu sau khi sửa
            loadEvent(currentEvent.getId());
        }
    }

    // Chuyển "2,3,4" → "T2, T3, T4"
    private String formatRepeatDays(String repeatDays) {
        String[] parts = repeatDays.split(",");
        String[] labels = {"", "CN", "T2", "T3", "T4", "T5", "T6", "T7"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            try {
                int d = Integer.parseInt(parts[i].trim());
                if (d >= 1 && d <= 7) {
                    if (i > 0) sb.append(", ");
                    sb.append(labels[d]);
                }
            } catch (NumberFormatException ignored) {}
        }
        return sb.toString();
    }
}