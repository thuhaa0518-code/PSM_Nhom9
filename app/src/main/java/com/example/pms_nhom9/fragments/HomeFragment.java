package com.example.pms_nhom9.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pms_nhom9.R;
import com.example.pms_nhom9.activities.EventDetailActivity;
import com.example.pms_nhom9.adapters.DateStripAdapter;
import com.example.pms_nhom9.adapters.EventAdapter;
import com.example.pms_nhom9.models.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private HomeViewModel viewModel;
    private EventAdapter eventAdapter;
    private DateStripAdapter dateAdapter;

    private TextView tvGreeting, tvDayLabel, tvAvatar;
    private TextView chipAll, chipHighPriority, tvSectionLabel;
    private RecyclerView rvEvents, rvDateStrip;
    private View layoutEmpty;

    // Ngày đang chọn (mặc định hôm nay)
    private Calendar selectedCal = Calendar.getInstance();

    private final SimpleDateFormat dayLabelFmt =
            new SimpleDateFormat("EEEE, d 'tháng' M", new Locale("vi", "VN"));
    private final SimpleDateFormat sectionFmt =
            new SimpleDateFormat("d 'tháng' M", new Locale("vi", "VN"));

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        tvGreeting      = view.findViewById(R.id.tvGreeting);
        tvDayLabel      = view.findViewById(R.id.tvDayLabel);
        tvAvatar        = view.findViewById(R.id.tvAvatar);
        rvEvents        = view.findViewById(R.id.rvEvents);
        rvDateStrip     = view.findViewById(R.id.rvDateStrip);
        layoutEmpty     = view.findViewById(R.id.layoutEmpty);
        chipAll         = view.findViewById(R.id.chipAll);
        chipHighPriority = view.findViewById(R.id.chipHighPriority);
        tvSectionLabel  = view.findViewById(R.id.tvSectionLabel);

        setupGreeting();
        setupDateStrip();
        setupEventList();
        observeEvents();
    }

    private void setupGreeting() {
        SharedPreferences prefs =
                requireContext().getSharedPreferences("psm_prefs", 0);
        String name = prefs.getString("logged_user_name", "bạn");
        tvGreeting.setText("Chào " + name + " !");
        tvAvatar.setText(name.substring(0, 1).toUpperCase());
        tvDayLabel.setText(dayLabelFmt.format(new Date()));
    }

    private void setupDateStrip() {
        List<Calendar> days = new ArrayList<>();
        // 7 ngày: hôm nay ở giữa (index 3)
        for (int i = -3; i <= 3; i++) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, i);
            days.add(cal);
        }

        dateAdapter = new DateStripAdapter(days, (date, position) -> {
            selectedCal = date;
            // Reset chips ngay lập tức khi đổi ngày
            chipAll.setText("...");
            chipHighPriority.setText("...");
            viewModel.selectDate(date.getTimeInMillis());
            updateSectionLabel();
        });

        rvDateStrip.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvDateStrip.setAdapter(dateAdapter);
        rvDateStrip.scrollToPosition(3);
    }

    private void setupEventList() {
        eventAdapter = new EventAdapter(new ArrayList<>(), event -> {
            Intent intent = new Intent(requireContext(), EventDetailActivity.class);
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getId());
            startActivityForResult(intent, 100);
        });
        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        rvEvents.setAdapter(eventAdapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @androidx.annotation.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            // Reload sau khi xoá hoặc sửa sự kiện
            viewModel.selectDate(viewModel.getSelectedDate());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload khi quay lại fragment
        viewModel.selectDate(viewModel.getSelectedDate());
    }

    private void observeEvents() {
        viewModel.getEventsForSelectedDay().observe(getViewLifecycleOwner(), events -> {
            if (events == null || events.isEmpty()) {
                rvEvents.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
                chipAll.setText("0 sự kiện");
                chipHighPriority.setText("0 ưu tiên cao");
            } else {
                rvEvents.setVisibility(View.VISIBLE);
                layoutEmpty.setVisibility(View.GONE);
                eventAdapter.setEvents(events);

                // Đếm chip
                int total = events.size();
                int high  = 0;
                for (Event e : events) if (e.getPriority() == 2) high++;
                chipAll.setText(total + " sự kiện");
                chipHighPriority.setText(high + " ưu tiên cao");
            }
        });
    }

    private void updateSectionLabel() {
        Calendar today = Calendar.getInstance();
        boolean isToday = selectedCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && selectedCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
        tvSectionLabel.setText(isToday ? "Hôm nay" : sectionFmt.format(selectedCal.getTime()));
    }
}
