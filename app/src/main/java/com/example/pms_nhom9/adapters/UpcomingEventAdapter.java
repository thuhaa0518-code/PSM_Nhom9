package com.example.pms_nhom9.adapters;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pms_nhom9.R;
import com.example.pms_nhom9.models.Event;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UpcomingEventAdapter
        extends RecyclerView.Adapter<UpcomingEventAdapter.UpcomingViewHolder> {

    private List<Event> events;
    private final OnEventClickListener listener;

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public UpcomingEventAdapter(List<Event> events, OnEventClickListener listener) {
        this.events   = events;
        this.listener = listener;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UpcomingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_upcoming_event, parent, false);
        return new UpcomingViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull UpcomingViewHolder h, int position) {
        Event event = events.get(position);
        Date  start = new Date(event.getStartTime());

        // Số ngày
        SimpleDateFormat dayFmt  = new SimpleDateFormat("d",    Locale.getDefault());
        SimpleDateFormat nameFmt = new SimpleDateFormat("EEE",  new Locale("vi","VN"));
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm",Locale.getDefault());

        h.tvDay.setText(dayFmt.format(start));
        h.tvDayName.setText(nameFmt.format(start));
        h.tvTitle.setText(event.getTitle());

        // Giờ + địa điểm
        String loc     = event.getLocation();
        String timeStr = timeFmt.format(start);
        String sub     = (loc != null && !loc.isEmpty())
                ? timeStr + " - " + loc
                : timeStr;
        h.tvTime.setText(sub);

        h.itemView.setOnClickListener(v -> listener.onEventClick(event));
    }

    @Override
    public int getItemCount() {
        return events != null ? events.size() : 0;
    }

    static class UpcomingViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay, tvDayName, tvTitle, tvTime;

        UpcomingViewHolder(@NonNull View v) {
            super(v);
            tvDay     = v.findViewById(R.id.tvUpcomingDay);
            tvDayName = v.findViewById(R.id.tvUpcomingDayName);
            tvTitle   = v.findViewById(R.id.tvUpcomingTitle);
            tvTime    = v.findViewById(R.id.tvUpcomingTime);
        }
    }
}
