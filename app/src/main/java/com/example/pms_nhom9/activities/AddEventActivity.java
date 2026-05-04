package com.example.pms_nhom9.activities;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.pms_nhom9.R;
import com.example.pms_nhom9.database.AppDatabase;
import com.example.pms_nhom9.database.EventDao;
import com.example.pms_nhom9.models.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;


public class AddEventActivity extends AppCompatActivity {

    public static final String EXTRA_EDIT_EVENT_ID = "extra_edit_event_id";
    private int editEventId = -1; // -1 = tạo mới, khác -1 = đang sửa
    private Event editingEvent = null;

    // --- Views ---
    private EditText etTitle, etLocation, etReminderMinutes, etStartTime, etEndTime;
    private TextView tvPickedDate;
    private SwitchCompat switchRepeat, switchReminder;
    private LinearLayout layoutRepeatDays, layoutReminderTime;
    private Button btnSave;
    private ImageButton btnBack;

    // Các ô màu
    private View colorRed, colorBlue, colorGreen,
            colorYellow, colorPurple, colorPink;

    // Các ô chọn ngày lặp
    private TextView dayT2, dayT3, dayT4, dayT5, dayT6, dayT7, dayCN;

    // Các chấm ưu tiên
    private View dotHigh, dotMed, dotLow;

    // --- State ---
    private Calendar pickedDate   = null; // null = chưa chọn ngày
    private Calendar startTimeCal = Calendar.getInstance();
    private Calendar endTimeCal   = Calendar.getInstance();

    private String selectedColor  = "#7F77DD"; // mặc định tím
    private int    selectedPriority = 1;        // mặc định trung bình

    // Danh sách ngày lặp đang được chọn (2=T2, 3=T3 ... 1=CN)
    private final List<Integer> selectedRepeatDays = new ArrayList<>();

    private EventDao eventDao;
    private int userId;
    private Spinner spinnerReminderUnit;

    private final SimpleDateFormat dateFmt = new SimpleDateFormat("EEE, d/M/yyyy", new Locale("vi","VN"));
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        // Lấy userId
        SharedPreferences prefs = getSharedPreferences("psm_prefs", MODE_PRIVATE);
        userId   = prefs.getInt("logged_user_id", -1);
        eventDao = AppDatabase.getInstance(this).eventDao();

        bindViews();
        editEventId = getIntent().getIntExtra(EXTRA_EDIT_EVENT_ID, -1);
        if (editEventId != -1) {
            // Chế độ sửa: đổi tiêu đề header
            TextView tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
            if (tvHeaderTitle != null) tvHeaderTitle.setText("Chi tiết sự kiện");
            loadEventForEdit(editEventId);
        } else {
            setupInitialValues();
        }

        setupListeners();
    }

    private void loadEventForEdit(int id) {
        Executors.newSingleThreadExecutor().execute(() -> {
            editingEvent = AppDatabase.getInstance(this)
                    .eventDao().getEventById(id);
            runOnUiThread(() -> {
                if (editingEvent == null) { finish(); return; }

                // Tên + địa điểm
                etTitle.setText(editingEvent.getTitle());
                etLocation.setText(editingEvent.getLocation() != null ? editingEvent.getLocation() : "");

                // Ngày + giờ
                if (pickedDate == null) pickedDate = Calendar.getInstance();
                pickedDate.setTimeInMillis(editingEvent.getStartTime());
                startTimeCal.setTimeInMillis(editingEvent.getStartTime());
                endTimeCal.setTimeInMillis(editingEvent.getEndTime());
                tvPickedDate.setText(dateFmt.format(pickedDate.getTime()));
                etStartTime.setText(timeFmt.format(startTimeCal.getTime()));
                etEndTime.setText(timeFmt.format(endTimeCal.getTime()));

                // Màu sắc
                selectedColor = editingEvent.getColor();
                if (selectedColor == null) selectedColor = colorValues[4];
                for (int i = 0; i < colorValues.length; i++) {
                    if (colorValues[i].equals(selectedColor)) {
                        updateColorUI(i);
                        break;
                    }
                }

                // Ưu tiên
                selectedPriority = editingEvent.getPriority();
                updatePriorityUI(selectedPriority);

                // Lặp lại
                switchRepeat.setChecked(editingEvent.isRepeat());
                if (editingEvent.isRepeat()) {
                    layoutRepeatDays.setVisibility(View.VISIBLE);
                    // Restore các ngày lặp đã chọn
                    String repeatDaysStr = editingEvent.getRepeatDays();
                    if (repeatDaysStr != null && !repeatDaysStr.isEmpty()) {
                        selectedRepeatDays.clear();
                        TextView[] dayViews = {dayT2, dayT3, dayT4, dayT5, dayT6, dayT7, dayCN};
                        int[] dayNums = {2, 3, 4, 5, 6, 7, 1};
                        for (String part : repeatDaysStr.split(",")) {
                            try {
                                int dayNum = Integer.parseInt(part.trim());
                                selectedRepeatDays.add(dayNum);
                                for (int i = 0; i < dayNums.length; i++) {
                                    if (dayNums[i] == dayNum) {
                                        dayViews[i].setBackgroundResource(R.drawable.bg_day_selected);
                                        dayViews[i].setTextColor(0xFFFFFFFF);
                                    }
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }

                // Nhắc nhở
                int reminderMin = editingEvent.getReminderMinutes();
                if (reminderMin > 0) {
                    switchReminder.setChecked(true);
                    layoutReminderTime.setVisibility(View.VISIBLE);
                    // Quy đổi ngược về value + unit
                    if (reminderMin % (60 * 24) == 0) {
                        etReminderMinutes.setText(String.valueOf(reminderMin / (60 * 24)));
                        if (spinnerReminderUnit != null) spinnerReminderUnit.setSelection(2); // Ngày
                    } else if (reminderMin % 60 == 0) {
                        etReminderMinutes.setText(String.valueOf(reminderMin / 60));
                        if (spinnerReminderUnit != null) spinnerReminderUnit.setSelection(1); // Giờ
                    } else {
                        etReminderMinutes.setText(String.valueOf(reminderMin));
                        if (spinnerReminderUnit != null) spinnerReminderUnit.setSelection(0); // Phút
                    }
                } else {
                    switchReminder.setChecked(false);
                    layoutReminderTime.setVisibility(View.GONE);
                }
            });
        });
    }

    // --- Ánh xạ toàn bộ view ---
    private void bindViews() {
        btnBack             = findViewById(R.id.btnBack);
        etTitle             = findViewById(R.id.etEventTitle);
        etLocation          = findViewById(R.id.etLocation);
        etReminderMinutes   = findViewById(R.id.etReminderMinutes);
        tvPickedDate        = findViewById(R.id.tvPickedDate);
        etStartTime         = findViewById(R.id.tvStartTime);
        etEndTime           = findViewById(R.id.tvEndTime);
        switchRepeat        = findViewById(R.id.switchRepeat);
        switchReminder      = findViewById(R.id.switchReminder);
        layoutRepeatDays    = findViewById(R.id.layoutRepeatDays);
        layoutReminderTime  = findViewById(R.id.layoutReminderTime);
        btnSave             = findViewById(R.id.btnSaveEvent);
        spinnerReminderUnit = findViewById(R.id.spinnerReminderUnit);

        // Setup spinner đơn vị nhắc nhở
        if (spinnerReminderUnit != null) {
            ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item,
                    new String[]{"Phút", "Giờ", "Ngày"});
            unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerReminderUnit.setAdapter(unitAdapter);
        }

        // Màu sắc
        colorRed    = findViewById(R.id.colorRed);
        colorBlue   = findViewById(R.id.colorBlue);
        colorGreen  = findViewById(R.id.colorGreen);
        colorYellow = findViewById(R.id.colorYellow);
        colorPurple = findViewById(R.id.colorPurple);
        colorPink   = findViewById(R.id.colorPink);

        // Ngày lặp
        dayT2 = findViewById(R.id.dayT2);
        dayT3 = findViewById(R.id.dayT3);
        dayT4 = findViewById(R.id.dayT4);
        dayT5 = findViewById(R.id.dayT5);
        dayT6 = findViewById(R.id.dayT6);
        dayT7 = findViewById(R.id.dayT7);
        dayCN = findViewById(R.id.dayCN);

        // Ưu tiên
        dotHigh = findViewById(R.id.dotPriorityHigh);
        dotMed  = findViewById(R.id.dotPriorityMed);
        dotLow  = findViewById(R.id.dotPriorityLow);
    }

    // --- Giá trị mặc định khi mở màn ---
    private void setupInitialValues() {
        // Ngày: để trống, người dùng tự chọn
        tvPickedDate.setText("Chọn ngày");
        tvPickedDate.setTextColor(0xFF9CA3AF); // màu hint xám
        pickedDate = null; // chưa chọn ngày

        // Giờ mặc định: 00:00
        startTimeCal = Calendar.getInstance();
        startTimeCal.set(Calendar.HOUR_OF_DAY, 0);
        startTimeCal.set(Calendar.MINUTE, 0);
        startTimeCal.set(Calendar.SECOND, 0);

        endTimeCal = (Calendar) startTimeCal.clone();

        etStartTime.setText("00:00");
        etEndTime.setText("00:00");

        // Highlight ưu tiên trung bình mặc định
        updatePriorityUI(1);
    }

    // --- Gắn toàn bộ listener ---
    private void setupListeners() {

        // Nút back — dùng tvHeaderTitle vì layout không có btnBack riêng
        TextView tvHeader = findViewById(R.id.tvHeaderTitle);
        if (tvHeader != null) tvHeader.setOnClickListener(v -> finish());
        // btnBack là ImageButton trong layout mới
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Chọn ngày
        tvPickedDate.setOnClickListener(v -> showDatePicker());

        // Chọn giờ bắt đầu / kết thúc — nhập thẳng, tự động thêm dấu ":"
        setupTimeInput(etStartTime, startTimeCal);
        setupTimeInput(etEndTime, endTimeCal);

        // Toggle lặp
        switchRepeat.setOnCheckedChangeListener((btn, isChecked) ->
                layoutRepeatDays.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        // Toggle nhắc nhở
        switchReminder.setOnCheckedChangeListener((btn, isChecked) ->
                layoutReminderTime.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        // Chọn màu
        setupColorListeners();

        // Chọn ngày lặp
        setupRepeatDayListeners();

        // Chọn ưu tiên
        dotHigh.setOnClickListener(v -> { selectedPriority = 2; updatePriorityUI(2); });
        dotMed.setOnClickListener(v  -> { selectedPriority = 1; updatePriorityUI(1); });
        dotLow.setOnClickListener(v  -> { selectedPriority = 0; updatePriorityUI(0); });

        // Nút lưu
        btnSave.setOnClickListener(v -> saveEvent());
    }

    // --- Date Picker ---
    private void showDatePicker() {
        Calendar base = (pickedDate != null) ? pickedDate : Calendar.getInstance();
        new DatePickerDialog(this,
                (view, year, month, day) -> {
                    if (pickedDate == null) pickedDate = Calendar.getInstance();
                    pickedDate.set(year, month, day);
                    tvPickedDate.setText(dateFmt.format(pickedDate.getTime()));
                    tvPickedDate.setTextColor(0xFF2C2C2A);
                },
                base.get(Calendar.YEAR),
                base.get(Calendar.MONTH),
                base.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    // --- Time input: nhập thẳng HH:mm, tự động chèn ":" sau 2 chữ số giờ ---
    private void setupTimeInput(EditText et, Calendar cal) {
        et.addTextChangedListener(new android.text.TextWatcher() {
            private boolean editing = false;

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (editing) return;
                editing = true;
                String text = s.toString().replace(":", "");
                if (text.length() >= 2) {
                    String formatted = text.substring(0, 2) + ":" + text.substring(2);
                    s.replace(0, s.length(), formatted);
                }
                editing = false;
            }
        });

        // Khi rời khỏi field: validate và cập nhật Calendar
        et.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validateAndApplyTime(et, cal);
        });
    }

    /** Validate chuỗi HH:mm và cập nhật vào Calendar; reset về 00:00 nếu sai */
    private void validateAndApplyTime(EditText et, Calendar cal) {
        String text = et.getText().toString().trim();
        try {
            String[] parts = text.split(":");
            if (parts.length != 2) throw new NumberFormatException();
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            if (h < 0 || h > 23 || m < 0 || m > 59) throw new NumberFormatException();
            cal.set(Calendar.HOUR_OF_DAY, h);
            cal.set(Calendar.MINUTE, m);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            et.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Giờ không hợp lệ (00:00 – 23:59)", Toast.LENGTH_SHORT).show();
            et.setText(String.format(Locale.getDefault(), "%02d:%02d",
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));
        }
    }

    /** Parse giờ từ EditText (định dạng HH:mm), trả về false nếu không hợp lệ */
    private boolean parseTimeFromInput() {
        try {
            String[] startParts = etStartTime.getText().toString().trim().split(":");
            String[] endParts   = etEndTime.getText().toString().trim().split(":");
            if (startParts.length != 2 || endParts.length != 2) {
                Toast.makeText(this, "Định dạng giờ phải là HH:mm", Toast.LENGTH_SHORT).show();
                return false;
            }
            int sh = Integer.parseInt(startParts[0]), sm = Integer.parseInt(startParts[1]);
            int eh = Integer.parseInt(endParts[0]),   em = Integer.parseInt(endParts[1]);
            if (sh < 0 || sh > 23 || sm < 0 || sm > 59 ||
                eh < 0 || eh > 23 || em < 0 || em > 59) {
                Toast.makeText(this, "Giờ không hợp lệ", Toast.LENGTH_SHORT).show();
                return false;
            }
            // Reset hoàn toàn trước khi set để tránh lỗi AM/PM và giây thừa
            startTimeCal.set(Calendar.HOUR_OF_DAY, sh);
            startTimeCal.set(Calendar.MINUTE, sm);
            startTimeCal.set(Calendar.SECOND, 0);
            startTimeCal.set(Calendar.MILLISECOND, 0);
            endTimeCal.set(Calendar.HOUR_OF_DAY, eh);
            endTimeCal.set(Calendar.MINUTE, em);
            endTimeCal.set(Calendar.SECOND, 0);
            endTimeCal.set(Calendar.MILLISECOND, 0);
            if (!endTimeCal.after(startTimeCal)) {
                Toast.makeText(this, "Giờ kết thúc phải sau giờ bắt đầu", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Định dạng giờ phải là HH:mm", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    // --- Color picker ---
    private final String[] colorValues = {
            "#E24B4A", "#378ADD", "#1D9E75",
            "#EF9F27", "#7F77DD", "#D4537E"
    };
    private final View[] colorViews = new View[6];
    private final TextView[] checkViews = new TextView[6];

    private void setupColorListeners() {
        colorViews[0] = colorRed;
        colorViews[1] = colorBlue;
        colorViews[2] = colorGreen;
        colorViews[3] = colorYellow;
        colorViews[4] = colorPurple;
        colorViews[5] = colorPink;

        // Bind check views bằng findViewById trực tiếp
        checkViews[0] = findViewById(R.id.checkRed);
        checkViews[1] = findViewById(R.id.checkBlue);
        checkViews[2] = findViewById(R.id.checkGreen);
        checkViews[3] = findViewById(R.id.checkYellow);
        checkViews[4] = findViewById(R.id.checkPurple);
        checkViews[5] = findViewById(R.id.checkPink);

        for (int i = 0; i < colorViews.length; i++) {
            final int index = i;
            colorViews[i].setOnClickListener(v -> {
                selectedColor = colorValues[index];
                updateColorUI(index);
            });
        }

        // Mặc định chọn tím (index 4)
        updateColorUI(4);
    }

    private void updateColorUI(int selectedIndex) {
        for (int i = 0; i < checkViews.length; i++) {
            if (checkViews[i] != null) {
                checkViews[i].setVisibility(i == selectedIndex ? View.VISIBLE : View.GONE);
            }
            // Reset scale về 1.0 (không phóng to nữa)
            colorViews[i].setScaleX(1.0f);
            colorViews[i].setScaleY(1.0f);
        }
    }

    // --- Repeat day toggle ---
    private void setupRepeatDayListeners() {
        TextView[] dayViews = {dayT2, dayT3, dayT4, dayT5, dayT6, dayT7, dayCN};
        int[]      dayNums  = {2, 3, 4, 5, 6, 7, 1}; // Calendar.DAY_OF_WEEK

        for (int i = 0; i < dayViews.length; i++) {
            final int dayNum  = dayNums[i];
            final TextView tv = dayViews[i];
            tv.setOnClickListener(v -> {
                if (selectedRepeatDays.contains(dayNum)) {
                    selectedRepeatDays.remove(Integer.valueOf(dayNum));
                    tv.setBackgroundResource(R.drawable.bg_day_normal);
                    tv.setTextColor(0xFF888780);
                } else {
                    selectedRepeatDays.add(dayNum);
                    tv.setBackgroundResource(R.drawable.bg_day_selected);
                    tv.setTextColor(0xFFFFFFFF);
                }
            });
        }
    }

    // --- Cập nhật UI ưu tiên (phóng to chấm được chọn) ---
    private void updatePriorityUI(int priority) {
        dotHigh.setScaleX(priority == 2 ? 1.3f : 1.0f);
        dotHigh.setScaleY(priority == 2 ? 1.3f : 1.0f);
        dotMed.setScaleX(priority == 1 ? 1.3f : 1.0f);
        dotMed.setScaleY(priority == 1 ? 1.3f : 1.0f);
        dotLow.setScaleX(priority == 0 ? 1.3f : 1.0f);
        dotLow.setScaleY(priority == 0 ? 1.3f : 1.0f);
    }

    // --- Lưu sự kiện vào DB ---
    private void saveEvent() {
        String title    = etTitle.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        // Validate tên
        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Vui lòng nhập tên sự kiện");
            etTitle.requestFocus();
            return;
        }

        if (pickedDate == null) {
            Toast.makeText(this, "Vui lòng chọn ngày", Toast.LENGTH_SHORT).show();
            return;
        }

        // Luôn parse giờ trực tiếp từ EditText (tránh trường hợp user chưa rời focus)
        int sh, sm, eh, em;
        try {
            String[] sp = etStartTime.getText().toString().trim().split(":");
            String[] ep = etEndTime.getText().toString().trim().split(":");
            if (sp.length != 2 || ep.length != 2) throw new NumberFormatException();
            sh = Integer.parseInt(sp[0]); sm = Integer.parseInt(sp[1]);
            eh = Integer.parseInt(ep[0]); em = Integer.parseInt(ep[1]);
            if (sh < 0 || sh > 23 || sm < 0 || sm > 59 ||
                eh < 0 || eh > 23 || em < 0 || em > 59) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Giờ không hợp lệ (00:00 – 23:59)", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate giờ kết thúc phải sau giờ bắt đầu
        if (eh < sh || (eh == sh && em <= sm)) {
            Toast.makeText(this, "Giờ kết thúc phải sau giờ bắt đầu", Toast.LENGTH_SHORT).show();
            return;
        }

        // Xây dựng timestamp hoàn chỉnh bằng cách tính thủ công
        // để tránh hoàn toàn mọi vấn đề AM/PM của Calendar
        // Lấy midnight của ngày đã chọn (00:00:00.000)
        Calendar midnight = Calendar.getInstance();
        midnight.set(pickedDate.get(Calendar.YEAR),
                     pickedDate.get(Calendar.MONTH),
                     pickedDate.get(Calendar.DAY_OF_MONTH),
                     0, 0, 0);
        midnight.set(Calendar.MILLISECOND, 0);
        long midnightMs = midnight.getTimeInMillis();

        // Cộng thêm giờ/phút dưới dạng milliseconds — không qua Calendar.set(HOUR)
        long startMs = midnightMs + sh * 3600_000L + sm * 60_000L;
        long endMs   = midnightMs + eh * 3600_000L + em * 60_000L;

        startTimeCal = Calendar.getInstance();
        startTimeCal.setTimeInMillis(startMs);

        endTimeCal = Calendar.getInstance();
        endTimeCal.setTimeInMillis(endMs);

        // Tạo chuỗi repeatDays: "2,3,4"
        StringBuilder repeatBuilder = new StringBuilder();
        for (int i = 0; i < selectedRepeatDays.size(); i++) {
            repeatBuilder.append(selectedRepeatDays.get(i));
            if (i < selectedRepeatDays.size() - 1) repeatBuilder.append(",");
        }
        String repeatDays = repeatBuilder.toString();

        // Lưu vào database trên thread phụ
        // Lấy reminder setting
        boolean hasReminder = switchReminder.isChecked();
        int reminderVal = 30;
        int reminderUnit = 0;
        if (hasReminder && etReminderMinutes != null) {
            try { reminderVal = Integer.parseInt(etReminderMinutes.getText().toString().trim()); }
            catch (NumberFormatException ignored) {}
            if (spinnerReminderUnit != null)
                reminderUnit = spinnerReminderUnit.getSelectedItemPosition();
        }
        final int finalReminderVal  = reminderVal;
        final int finalReminderUnit = reminderUnit;
        final boolean finalHasReminder = hasReminder;

        Executors.newSingleThreadExecutor().execute(() -> {
            // Kiểm tra xung đột thời gian
            int excludeId = (editingEvent != null) ? editingEvent.getId() : -1;
            Event conflict = eventDao.getConflictingEvent(
                    userId,
                    startTimeCal.getTimeInMillis(),
                    endTimeCal.getTimeInMillis(),
                    excludeId);

            if (conflict != null) {
                // Có xung đột → hỏi người dùng
                java.text.SimpleDateFormat fmt =
                        new java.text.SimpleDateFormat("HH:mm, d/M/yyyy", java.util.Locale.getDefault());
                String conflictInfo = "\"" + conflict.getTitle() + "\"\n"
                        + fmt.format(new java.util.Date(conflict.getStartTime()))
                        + " → " + fmt.format(new java.util.Date(conflict.getEndTime()));

                runOnUiThread(() ->
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("⚠ Trùng lịch")
                        .setMessage("Đã có sự kiện trong khoảng thời gian này:\n\n"
                                + conflictInfo + "\n\nBạn vẫn muốn lưu?")
                        .setPositiveButton("Vẫn lưu", (d, w) ->
                                doSave(finalReminderVal, finalReminderUnit, finalHasReminder, repeatDays))
                        .setNegativeButton("Huỷ", null)
                        .show()
                );
                return;
            }

            doSave(finalReminderVal, finalReminderUnit, finalHasReminder, repeatDays);
        });
    }

    private void doSave(int reminderVal, int reminderUnit,
                        boolean hasReminder, String repeatDays) {
        String title    = etTitle.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        int totalMinutes = 0;
        if (hasReminder) {
            switch (reminderUnit) {
                case 1: totalMinutes = reminderVal * 60; break;
                case 2: totalMinutes = reminderVal * 60 * 24; break;
                default: totalMinutes = reminderVal; break;
            }
        }
        final int finalTotalMinutes = totalMinutes;

        // Tạo Event object
        Event event;
        if (editingEvent != null) {
            event = editingEvent;
            event.setTitle(title);
            event.setLocation(location);
            event.setStartTime(startTimeCal.getTimeInMillis());
            event.setEndTime(endTimeCal.getTimeInMillis());
            event.setColor(selectedColor);
            event.setPriority(selectedPriority);
            event.setRepeat(switchRepeat.isChecked());
            event.setRepeatDays(repeatDays);
            event.setReminderMinutes(finalTotalMinutes);
        } else {
            event = new Event(userId, title, location, "",
                    startTimeCal.getTimeInMillis(), endTimeCal.getTimeInMillis(),
                    selectedColor, selectedPriority, switchRepeat.isChecked(), repeatDays);
            event.setReminderMinutes(finalTotalMinutes);
        }

        final Event finalEvent = event;
        com.example.pms_nhom9.api.ApiEventRepository repo =
                new com.example.pms_nhom9.api.ApiEventRepository(this);

        com.example.pms_nhom9.api.ApiEventRepository.Callback<Event> cb =
                new com.example.pms_nhom9.api.ApiEventRepository.Callback<Event>() {
            @Override public void onSuccess(Event saved) {
                // Lên lịch alarm nếu có reminder
                if (hasReminder && finalTotalMinutes > 0) {
                    com.example.pms_nhom9.utils.EventAlarmScheduler
                            .scheduleMinutes(AddEventActivity.this, saved, finalTotalMinutes);
                }
                // Sync về Room để hiển thị ngay
                com.example.pms_nhom9.api.SyncManager.syncAll(AddEventActivity.this, null);
                runOnUiThread(() -> {
                    Toast.makeText(AddEventActivity.this, "Lưu thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }
            @Override public void onError(String message) {
                runOnUiThread(() ->
                    Toast.makeText(AddEventActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        };

        if (editingEvent != null) {
            com.example.pms_nhom9.utils.EventAlarmScheduler.cancel(this, editingEvent);
            repo.updateEvent(finalEvent, cb);
        } else {
            repo.createEvent(finalEvent, cb);
        }
    }
}
