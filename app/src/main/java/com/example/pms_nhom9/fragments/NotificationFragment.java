package com.example.pms_nhom9.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pms_nhom9.R;
import com.example.pms_nhom9.activities.EventDetailActivity;
import com.example.pms_nhom9.adapters.NotificationAdapter;
import com.example.pms_nhom9.database.AppDatabase;
import com.example.pms_nhom9.database.EventDao;
import com.example.pms_nhom9.models.Event;
import com.example.pms_nhom9.models.NotificationItem;
import com.example.pms_nhom9.utils.EventAlarmScheduler;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class NotificationFragment extends Fragment {

    private RecyclerView rvNotifications;
    private TextView tvSubtitle;
    private LinearLayout layoutSnackbar;
    private TextView tvSnackbarMsg;

    private NotificationAdapter adapter;
    private final List<NotificationItem> notifItems = new ArrayList<>();
    private EventDao eventDao;
    private int userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs =
                requireContext().getSharedPreferences("psm_prefs", 0);
        userId   = prefs.getInt("logged_user_id", -1);
        eventDao = AppDatabase.getInstance(requireContext()).eventDao();

        rvNotifications = view.findViewById(R.id.rvNotifications);
        tvSubtitle      = view.findViewById(R.id.tvNotifSubtitle);
        layoutSnackbar  = view.findViewById(R.id.layoutSnackbar);
        tvSnackbarMsg   = view.findViewById(R.id.tvSnackbarMsg);

        adapter = new NotificationAdapter(notifItems, new NotificationAdapter.Listener() {
            @Override
            public void onSnooze(NotificationItem item) {
                showSnoozeSheet(item);
            }

            @Override
            public void onViewDetail(NotificationItem item) {
                Intent intent = new Intent(requireContext(), EventDetailActivity.class);
                intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, item.getEventId());
                startActivity(intent);
            }

            @Override
            public void onDismiss(NotificationItem item) {
                item.setType(NotificationItem.Type.DONE);
                resortAndNotify();
                showSnackbar("Sự kiện đã bị bỏ qua!");
            }

            @Override
            public void onMarkDone(NotificationItem item) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    Event event = eventDao.getEventById(item.getEventId());
                    if (event != null) {
                        event.setCompleted(true);
                        eventDao.updateEvent(event);
                    }
                });
                item.setType(NotificationItem.Type.DONE);
                resortAndNotify();
                showSnackbar("Sự kiện đã hoàn thành!");
            }
        });

        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        rvNotifications.setAdapter(adapter);

        loadNotifications();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reset badge khi đang xem màn thông báo
        com.example.pms_nhom9.utils.NotificationBadgeManager.reset(requireContext());
        if (getActivity() instanceof com.example.pms_nhom9.activities.MainActivity) {
            ((com.example.pms_nhom9.activities.MainActivity) getActivity()).updateNotificationBadge();
        }
        loadNotifications();
    }

    private void loadNotifications() {
        long now = System.currentTimeMillis();

        // Lấy sự kiện hôm nay
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startDay = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        long endDay = cal.getTimeInMillis();

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Event> events = eventDao.getEventsInRangeSync(userId, startDay, endDay);
            List<NotificationItem> upcoming = new ArrayList<>();
            List<NotificationItem> past     = new ArrayList<>();

            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

            for (Event event : events) {
                long startDiff = event.getStartTime() - now;
                long endDiff   = event.getEndTime()   - now;
                long diffMin   = startDiff / 60000;
                String timeLabel = "Hôm nay, " + timeFmt.format(new java.util.Date(event.getStartTime()));
                String subtitle  = buildSubtitle(event);

                NotificationItem.Type type;
                if (startDiff > 0) {
                    // Chưa bắt đầu → UPCOMING
                    if (diffMin < 60) {
                        timeLabel = diffMin + " phút nữa";
                    } else {
                        timeLabel = (diffMin / 60) + " giờ nữa";
                    }
                    type = NotificationItem.Type.UPCOMING;
                } else if (endDiff > 0) {
                    // Đang diễn ra → UPCOMING (chưa quá hạn)
                    long runningMin = -startDiff / 60000;
                    if (runningMin < 60) {
                        timeLabel = "Đang diễn ra · " + runningMin + " phút trước";
                    } else {
                        timeLabel = "Đang diễn ra · " + (runningMin / 60) + " giờ trước";
                    }
                    type = event.isCompleted()
                            ? NotificationItem.Type.DONE
                            : NotificationItem.Type.UPCOMING;
                } else {
                    // Đã kết thúc → URGENT hoặc DONE
                    long ago = (-startDiff) / 60000;
                    if (ago < 60) {
                        timeLabel = ago + " phút trước";
                    } else {
                        timeLabel = "Hôm nay, " + timeFmt.format(new java.util.Date(event.getStartTime()));
                    }
                    type = event.isCompleted()
                            ? NotificationItem.Type.DONE
                            : NotificationItem.Type.URGENT;
                }

                NotificationItem ni = new NotificationItem(
                        event.getId(), event.getTitle(), subtitle, timeLabel, type);
                ni.setCompleted(event.isCompleted());
                ni.setEventColor(event.getColor());

                if (startDiff > 0) {
                    upcoming.add(ni); // chưa bắt đầu → lên đầu
                } else if (endDiff > 0 && !event.isCompleted()) {
                    upcoming.add(ni); // đang diễn ra → cũng lên đầu
                } else {
                    past.add(ni);     // đã kết thúc → xuống dưới
                }
            }

            // Sắp xếp: upcoming theo startTime ASC (gần nhất lên đầu)
            // past theo endTime DESC (kết thúc gần nhất lên đầu trong nhóm đã qua)
            upcoming.sort((a, b) -> {
                Event ea = findEvent(events, a.getEventId());
                Event eb = findEvent(events, b.getEventId());
                if (ea == null || eb == null) return 0;
                return Long.compare(ea.getStartTime(), eb.getStartTime());
            });
            past.sort((a, b) -> {
                Event ea = findEvent(events, a.getEventId());
                Event eb = findEvent(events, b.getEventId());
                if (ea == null || eb == null) return 0;
                return Long.compare(eb.getEndTime(), ea.getEndTime());
            });

            // Ghép: upcoming trước, past sau
            List<NotificationItem> sorted = new ArrayList<>();
            sorted.addAll(upcoming);
            sorted.addAll(past);

            requireActivity().runOnUiThread(() -> {
                notifItems.clear();
                notifItems.addAll(sorted);
                adapter.notifyDataSetChanged();
                tvSubtitle.setText(sorted.size() + " nhắc nhở hôm nay");
            });
        });
    }

    private Event findEvent(List<Event> events, int id) {
        for (Event e : events) if (e.getId() == id) return e;
        return null;
    }

    private String buildSubtitle(Event event) {
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String start = timeFmt.format(new Date(event.getStartTime()));
        String end   = timeFmt.format(new Date(event.getEndTime()));
        String loc   = event.getLocation();
        if (loc != null && !loc.isEmpty()) {
            return loc + ". " + start + "-" + end;
        }
        return start + "-" + end;
    }

    private void showSnoozeSheet(NotificationItem item) {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_snooze, null);
        sheet.setContentView(sheetView);

        sheetView.findViewById(R.id.optionSnooze5).setOnClickListener(v -> {
            snoozeEvent(item, 5);
            sheet.dismiss();
        });
        sheetView.findViewById(R.id.optionSnooze10).setOnClickListener(v -> {
            snoozeEvent(item, 10);
            sheet.dismiss();
        });
        sheetView.findViewById(R.id.optionSnooze15).setOnClickListener(v -> {
            snoozeEvent(item, 15);
            sheet.dismiss();
        });

        sheet.show();
    }

    private void snoozeEvent(NotificationItem item, int minutes) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Event event = eventDao.getEventById(item.getEventId());
            if (event != null) {
                EventAlarmScheduler.snooze(requireContext(), event, minutes);
            }
        });
        showSnackbar("Sẽ nhắc lại sau " + minutes + " phút!");
    }

    private void showSnackbar(String msg) {
        tvSnackbarMsg.setText(msg);
        layoutSnackbar.setVisibility(View.VISIBLE);
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> layoutSnackbar.setVisibility(View.GONE), 2500);
    }

    /**
     * Re-sort danh sách: UPCOMING lên trên, DONE/URGENT xuống dưới.
     * Gọi sau khi đổi type của một item.
     */
    private void resortAndNotify() {
        List<NotificationItem> upcoming = new ArrayList<>();
        List<NotificationItem> done     = new ArrayList<>();
        for (NotificationItem ni : notifItems) {
            if (ni.getType() == NotificationItem.Type.UPCOMING) {
                upcoming.add(ni);
            } else {
                done.add(ni);
            }
        }
        notifItems.clear();
        notifItems.addAll(upcoming);
        notifItems.addAll(done);
        adapter.notifyDataSetChanged();
    }
}
