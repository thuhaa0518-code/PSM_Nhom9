package com.example.pms_nhom9.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pms_nhom9.R;
import com.example.pms_nhom9.activities.EventDetailActivity;
import com.example.pms_nhom9.adapters.CalendarDayAdapter;
import com.example.pms_nhom9.adapters.DateStripAdapter;
import com.example.pms_nhom9.adapters.EventAdapter;
import com.example.pms_nhom9.adapters.UpcomingEventAdapter;
import com.example.pms_nhom9.adapters.WeekDateStripAdapter;
import com.example.pms_nhom9.database.AppDatabase;
import com.example.pms_nhom9.database.EventDao;
import com.example.pms_nhom9.models.CalendarDay;
import com.example.pms_nhom9.models.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class CalendarFragment extends Fragment {

    // ── Views ──────────────────────────────────────────────────────────
    private TextView tvMonthYear, tvEventCount;
    private TextView tabDay, tabWeek, tabMonth;
    private ImageButton btnPrev, btnNext;

    // Month view
    private LinearLayout viewMonth;
    private RecyclerView rvGrid, rvUpcoming;
    private CalendarDayAdapter dayAdapter;
    private UpcomingEventAdapter upcomingAdapter;

    // Week view
    private LinearLayout viewWeek;
    private TextView tvWeekLabel;
    private RecyclerView rvWeekStrip, rvWeekEvents;
    private WeekDateStripAdapter weekStripAdapter;
    private EventAdapter weekAdapter;

    // Day view
    private LinearLayout viewDay, layoutDayEmpty;
    private TextView tvDayViewLabel;
    private RecyclerView rvDayStrip, rvDayEvents;
    private DateStripAdapter dayStripAdapter;
    private EventAdapter dayAdapter2;

    // ── State ──────────────────────────────────────────────────────────
    private enum Tab { DAY, WEEK, MONTH }
    private Tab currentTab = Tab.MONTH;

    private final Calendar currentMonth = Calendar.getInstance();
    private final Calendar currentWeek  = Calendar.getInstance();
    private long selectedDayMillis      = System.currentTimeMillis();

    private EventDao eventDao;
    private int userId;

    private final SimpleDateFormat monthFmt =
            new SimpleDateFormat("'Tháng' M - yyyy", new Locale("vi", "VN"));
    private final SimpleDateFormat timeFmt =
            new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat dayFmt =
            new SimpleDateFormat("EEEE, d/M", new Locale("vi", "VN"));

    // ── Lifecycle ──────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireContext().getSharedPreferences("psm_prefs", 0);
        userId   = prefs.getInt("logged_user_id", -1);
        eventDao = AppDatabase.getInstance(requireContext()).eventDao();

        bindViews(view);
        setupTabs();
        setupMonthView();
        setupWeekView();
        setupDayView();
        switchTab(Tab.MONTH);
    }

    // ── Bind ───────────────────────────────────────────────────────────
    private void bindViews(View v) {
        tvMonthYear  = v.findViewById(R.id.tvMonthYear);
        tvEventCount = v.findViewById(R.id.tvEventCount);
        btnPrev      = v.findViewById(R.id.btnPrevMonth);
        btnNext      = v.findViewById(R.id.btnNextMonth);
        tabDay       = v.findViewById(R.id.tabDay);
        tabWeek      = v.findViewById(R.id.tabWeek);
        tabMonth     = v.findViewById(R.id.tabMonth);

        viewMonth    = v.findViewById(R.id.viewMonth);
        rvGrid       = v.findViewById(R.id.rvCalendarGrid);
        rvUpcoming   = v.findViewById(R.id.rvUpcomingEvents);

        viewWeek     = v.findViewById(R.id.viewWeek);
        tvWeekLabel  = v.findViewById(R.id.tvWeekLabel);
        rvWeekStrip  = v.findViewById(R.id.rvWeekStrip);
        rvWeekEvents = v.findViewById(R.id.rvWeekEvents);

        viewDay        = v.findViewById(R.id.viewDay);
        tvDayViewLabel = v.findViewById(R.id.tvDayViewLabel);
        rvDayStrip     = v.findViewById(R.id.rvDayStrip);
        rvDayEvents    = v.findViewById(R.id.rvDayEvents);
        layoutDayEmpty = v.findViewById(R.id.layoutDayEmpty);
    }

    // ── Tabs ───────────────────────────────────────────────────────────
    private void setupTabs() {
        tabDay.setOnClickListener(v   -> switchTab(Tab.DAY));
        tabWeek.setOnClickListener(v  -> switchTab(Tab.WEEK));
        tabMonth.setOnClickListener(v -> switchTab(Tab.MONTH));

        btnPrev.setOnClickListener(v -> onPrev());
        btnNext.setOnClickListener(v -> onNext());
    }

    private void switchTab(Tab tab) {
        currentTab = tab;

        // Reset tab styles
        tabDay.setBackgroundResource(R.drawable.bg_tab_normal);
        tabDay.setTextColor(0xFF9CA3AF); tabDay.setTypeface(null, android.graphics.Typeface.NORMAL);
        tabWeek.setBackgroundResource(R.drawable.bg_tab_normal);
        tabWeek.setTextColor(0xFF9CA3AF); tabWeek.setTypeface(null, android.graphics.Typeface.NORMAL);
        tabMonth.setBackgroundResource(R.drawable.bg_tab_normal);
        tabMonth.setTextColor(0xFF9CA3AF); tabMonth.setTypeface(null, android.graphics.Typeface.NORMAL);

        viewMonth.setVisibility(View.GONE);
        viewWeek.setVisibility(View.GONE);
        viewDay.setVisibility(View.GONE);

        switch (tab) {
            case DAY:
                tabDay.setBackgroundResource(R.drawable.bg_tab_active);
                tabDay.setTextColor(0xFFFFFFFF);
                tabDay.setTypeface(null, android.graphics.Typeface.BOLD);
                viewDay.setVisibility(View.VISIBLE);
                renderDay();
                break;
            case WEEK:
                tabWeek.setBackgroundResource(R.drawable.bg_tab_active);
                tabWeek.setTextColor(0xFFFFFFFF);
                tabWeek.setTypeface(null, android.graphics.Typeface.BOLD);
                viewWeek.setVisibility(View.VISIBLE);
                renderWeek();
                break;
            case MONTH:
                tabMonth.setBackgroundResource(R.drawable.bg_tab_active);
                tabMonth.setTextColor(0xFFFFFFFF);
                tabMonth.setTypeface(null, android.graphics.Typeface.BOLD);
                viewMonth.setVisibility(View.VISIBLE);
                renderMonth();
                break;
        }
    }

    private void onPrev() {
        switch (currentTab) {
            case MONTH:
                currentMonth.add(Calendar.MONTH, -1);
                renderMonth();
                break;
            case WEEK:
                currentWeek.add(Calendar.WEEK_OF_YEAR, -1);
                renderWeek();
                break;
            case DAY:
                Calendar prev = Calendar.getInstance();
                prev.setTimeInMillis(selectedDayMillis);
                prev.add(Calendar.DAY_OF_MONTH, -1);
                selectedDayMillis = prev.getTimeInMillis();
                renderDay();
                break;
        }
    }

    private void onNext() {
        switch (currentTab) {
            case MONTH:
                currentMonth.add(Calendar.MONTH, 1);
                renderMonth();
                break;
            case WEEK:
                currentWeek.add(Calendar.WEEK_OF_YEAR, 1);
                renderWeek();
                break;
            case DAY:
                Calendar next = Calendar.getInstance();
                next.setTimeInMillis(selectedDayMillis);
                next.add(Calendar.DAY_OF_MONTH, 1);
                selectedDayMillis = next.getTimeInMillis();
                renderDay();
                break;
        }
    }

    // ── MONTH ──────────────────────────────────────────────────────────
    private void loadUpcomingFromDate(long dateMillis) {
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(dateMillis);
        start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0);
        Calendar end = (Calendar) start.clone();
        end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Event> events = getEventsSync(userId, start.getTimeInMillis(), end.getTimeInMillis());
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (upcomingAdapter != null) upcomingAdapter.setEvents(events);
                });
            }
        });
    }

    private void setupMonthView() {
        dayAdapter = new CalendarDayAdapter(new ArrayList<>(), (day, pos) -> {
            if (day.getDay() == 0) return;
            Calendar sel = (Calendar) currentMonth.clone();
            sel.set(Calendar.DAY_OF_MONTH, day.getDay());
            selectedDayMillis = sel.getTimeInMillis();
            loadUpcomingFromDate(selectedDayMillis);
        });
        rvGrid.setLayoutManager(new GridLayoutManager(getContext(), 7));
        rvGrid.setAdapter(dayAdapter);

        upcomingAdapter = new UpcomingEventAdapter(new ArrayList<>(), event -> openDetail(event));
        rvUpcoming.setLayoutManager(new LinearLayoutManager(getContext()));
        rvUpcoming.setAdapter(upcomingAdapter);
    }

    private void renderMonth() {
        tvMonthYear.setText(monthFmt.format(currentMonth.getTime()));
        List<CalendarDay> days = buildCalendarDays();
        loadEventColorsForMonth(days);
    }

    private List<CalendarDay> buildCalendarDays() {
        List<CalendarDay> days = new ArrayList<>();
        Calendar cal = (Calendar) currentMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDow = cal.get(Calendar.DAY_OF_WEEK);
        int offset   = (firstDow == Calendar.SUNDAY) ? 6 : firstDow - 2;
        for (int i = 0; i < offset; i++) days.add(new CalendarDay(0, false));

        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar today = Calendar.getInstance();
        for (int d = 1; d <= maxDay; d++) {
            cal.set(Calendar.DAY_OF_MONTH, d);
            boolean isToday = today.get(Calendar.YEAR)  == currentMonth.get(Calendar.YEAR)
                    && today.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH)
                    && today.get(Calendar.DAY_OF_MONTH) == d;
            days.add(new CalendarDay(d, cal.getTimeInMillis(), isToday, true));
        }
        while (days.size() % 7 != 0) days.add(new CalendarDay(0, false));
        return days;
    }

    private void loadEventColorsForMonth(List<CalendarDay> days) {
        Calendar start = (Calendar) currentMonth.clone();
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0);
        Calendar end = (Calendar) currentMonth.clone();
        end.set(Calendar.DAY_OF_MONTH, currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH));
        end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59); end.set(Calendar.SECOND, 59);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Event> events = getEventsSync(userId, start.getTimeInMillis(), end.getTimeInMillis());
            for (Event event : events) {
                Calendar evCal = Calendar.getInstance();
                evCal.setTimeInMillis(event.getStartTime());
                int evDay = evCal.get(Calendar.DAY_OF_MONTH);
                for (CalendarDay cd : days) {
                    if (cd.getDay() == evDay && cd.isCurrentMonth()
                            && cd.getEventColors() != null && cd.getEventColors().size() < 3) {
                        cd.getEventColors().add(event.getColor());
                        break;
                    }
                }
            }
            int total = events.size();
            // Giữ lại list events của tháng để hiển thị bên dưới
            final List<Event> monthEvents = events;
            requireActivity().runOnUiThread(() -> {
                dayAdapter = new CalendarDayAdapter(days, (day, pos) -> {
                    if (day.getDay() == 0) return;
                    // Lọc sự kiện của ngày được chọn từ danh sách tháng
                    int selectedDay = day.getDay();
                    List<Event> dayEvents = new ArrayList<>();
                    for (Event e : monthEvents) {
                        Calendar ec = Calendar.getInstance();
                        ec.setTimeInMillis(e.getStartTime());
                        if (ec.get(Calendar.DAY_OF_MONTH) == selectedDay
                                && ec.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH)
                                && ec.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR)) {
                            dayEvents.add(e);
                        }
                    }
                    upcomingAdapter.setEvents(dayEvents);
                });
                rvGrid.setAdapter(dayAdapter);
                tvEventCount.setText(total > 0 ? total + " sự kiện" : "");
                // Hiển thị tất cả sự kiện tháng mặc định
                upcomingAdapter.setEvents(monthEvents);
            });
        });
    }

    // ── WEEK ───────────────────────────────────────────────────────────
    private void setupWeekView() {
        weekAdapter = new EventAdapter(new ArrayList<>(), event -> openDetail(event));
        rvWeekEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        rvWeekEvents.setAdapter(weekAdapter);
    }

    private void renderWeek() {
        // Tìm T2 của tuần hiện tại
        Calendar monday = (Calendar) currentWeek.clone();
        int dow = monday.get(Calendar.DAY_OF_WEEK);
        int diff = (dow == Calendar.SUNDAY) ? -6 : Calendar.MONDAY - dow;
        monday.add(Calendar.DAY_OF_MONTH, diff);

        // Tiêu đề tuần
        Calendar sunday = (Calendar) monday.clone();
        sunday.add(Calendar.DAY_OF_MONTH, 6);
        SimpleDateFormat wFmt = new SimpleDateFormat("d/M", Locale.getDefault());
        tvWeekLabel.setText("Tuần: " + wFmt.format(monday.getTime())
                + " - " + wFmt.format(sunday.getTime()));
        tvMonthYear.setText(monthFmt.format(monday.getTime()));

        // Xây danh sách 7 ngày
        List<Calendar> weekDays = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Calendar d = (Calendar) monday.clone();
            d.add(Calendar.DAY_OF_MONTH, i);
            weekDays.add(d);
        }

        // Tìm index ngày hôm nay (hoặc ngày đang chọn) trong tuần
        Calendar today = Calendar.getInstance();
        int selectedIdx = 0;
        for (int i = 0; i < weekDays.size(); i++) {
            Calendar d = weekDays.get(i);
            if (d.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                    && d.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                selectedIdx = i;
                break;
            }
        }

        final int finalSelectedIdx = selectedIdx;
        final Calendar finalMonday = monday;

        weekStripAdapter = new WeekDateStripAdapter(weekDays, selectedIdx, (date, pos) -> {
            selectedDayMillis = date.getTimeInMillis();
            // Lọc sự kiện của ngày được chọn từ danh sách tuần
            loadWeekEventsForDay(finalMonday, date.getTimeInMillis());
        });
        rvWeekStrip.setLayoutManager(
                new androidx.recyclerview.widget.GridLayoutManager(getContext(), 7));
        rvWeekStrip.setAdapter(weekStripAdapter);

        // Load events → cập nhật danh sách + chấm màu
        renderWeekEvents(monday);
    }

    private void loadWeekEventsForDay(Calendar monday, long dayMillis) {
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(dayMillis);
        start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0);
        Calendar end = (Calendar) start.clone();
        end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Event> events = getEventsSync(userId, start.getTimeInMillis(), end.getTimeInMillis());
            requireActivity().runOnUiThread(() -> weekAdapter.setEvents(events));
        });
    }

    private void renderWeekEvents(Calendar monday) {
        Calendar end = (Calendar) monday.clone();
        end.add(Calendar.DAY_OF_MONTH, 6);
        end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59);

        Calendar start = (Calendar) monday.clone();
        start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Event> events = getEventsSync(userId, start.getTimeInMillis(), end.getTimeInMillis());

            // Tính chấm màu cho từng ngày trong tuần
            List<List<String>> colorsByDay = new ArrayList<>();
            for (int i = 0; i < 7; i++) colorsByDay.add(new ArrayList<>());

            for (Event e : events) {
                Calendar ec = Calendar.getInstance();
                ec.setTimeInMillis(e.getStartTime());
                // Tính index ngày trong tuần (0=T2 ... 6=CN)
                Calendar d = (Calendar) monday.clone();
                for (int i = 0; i < 7; i++) {
                    if (d.get(Calendar.DAY_OF_YEAR) == ec.get(Calendar.DAY_OF_YEAR)
                            && d.get(Calendar.YEAR) == ec.get(Calendar.YEAR)) {
                        if (colorsByDay.get(i).size() < 3) {
                            colorsByDay.get(i).add(e.getColor());
                        }
                        break;
                    }
                    d.add(Calendar.DAY_OF_MONTH, 1);
                }
            }

            requireActivity().runOnUiThread(() -> {
                weekAdapter.setEvents(events);
                if (weekStripAdapter != null) weekStripAdapter.updateColors(colorsByDay);
            });
        });
    }

    // ── DAY ────────────────────────────────────────────────────────────
    private void setupDayView() {
        // Date strip: 7 ngày xung quanh hôm nay
        List<Calendar> stripDays = new ArrayList<>();
        for (int i = -3; i <= 3; i++) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_MONTH, i);
            stripDays.add(c);
        }
        dayStripAdapter = new DateStripAdapter(stripDays, (date, pos) -> {
            selectedDayMillis = date.getTimeInMillis();
            renderDayEvents();
        });
        rvDayStrip.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvDayStrip.setAdapter(dayStripAdapter);
        rvDayStrip.scrollToPosition(3);

        dayAdapter2 = new EventAdapter(new ArrayList<>(), event -> openDetail(event));
        rvDayEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDayEvents.setAdapter(dayAdapter2);
    }

    private void renderDay() {
        tvMonthYear.setText(dayFmt.format(new Date(selectedDayMillis)));
        renderDayEvents();
    }

    private void renderDayEvents() {
        // Label ngày
        SimpleDateFormat lbl = new SimpleDateFormat("EEEE, d 'tháng' M", new Locale("vi", "VN"));
        tvDayViewLabel.setText(lbl.format(new Date(selectedDayMillis)));

        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(selectedDayMillis);
        start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0);
        Calendar end = (Calendar) start.clone();
        end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Event> events = getEventsSync(userId, start.getTimeInMillis(), end.getTimeInMillis());
            requireActivity().runOnUiThread(() -> {
                dayAdapter2.setEvents(events);
                boolean empty = events.isEmpty();
                rvDayEvents.setVisibility(empty ? View.GONE : View.VISIBLE);
                layoutDayEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            });
        });
    }

    // ── Helpers ────────────────────────────────────────────────────────
    private List<Event> getEventsSync(int uid, long from, long to) {
        return AppDatabase.getInstance(requireContext())
                .eventDao().getEventsInRangeSync(uid, from, to);
    }

    private void openDetail(Event event) {
        Intent intent = new Intent(requireContext(), EventDetailActivity.class);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getId());
        startActivity(intent);
    }
}
