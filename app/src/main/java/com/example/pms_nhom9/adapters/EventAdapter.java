package com.example.pms_nhom9.adapters;


import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pms_nhom9.R;
import com.example.pms_nhom9.activities.EventDetailActivity;
import com.example.pms_nhom9.models.Event;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> events;
    private final OnEventClickListener listener;

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public EventAdapter(List<Event> events, OnEventClickListener listener) {
        this.events   = events;
        this.listener = listener;
    }

    public void setEvents(List<Event> newEvents) {
        this.events = newEvents;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);

        // Giờ bắt đầu
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        holder.tvEventTime.setText(sdf.format(new Date(event.getStartTime())));

        // Màu chấm tròn
        try {
            holder.viewColorDot.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor(event.getColor()))
            );
        } catch (Exception e) {
            holder.viewColorDot.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#7F77DD"))
            );
        }

        // Tên sự kiện
        holder.tvEventTitle.setText(event.getTitle());

        // Địa điểm
        String loc = event.getLocation();
        holder.tvEventLocation.setText((loc != null && !loc.isEmpty()) ? loc : "");

        // Thời lượng (tính bằng phút hoặc giờ)
        long diffMs  = event.getEndTime() - event.getStartTime();
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs);
        String duration;
        if (minutes < 60) {
            duration = minutes + " phút";
        } else {
            long hours = minutes / 60;
            long mins  = minutes % 60;
            duration   = mins > 0 ? hours + " tiếng " + mins + " phút" : hours + " tiếng";
        }
        holder.tvEventDuration.setText(duration);

        // Click vào sự kiện
        holder.cardEvent.setOnClickListener(v -> {
            // Truyền event ID sang EventDetailActivity
            Intent intent = new Intent(v.getContext(), EventDetailActivity.class);
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getId());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return events != null ? events.size() : 0;
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventTime, tvEventTitle, tvEventLocation, tvEventDuration;
        View viewColorDot, cardEvent;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventTime     = itemView.findViewById(R.id.tvEventTime);
            tvEventTitle    = itemView.findViewById(R.id.tvEventTitle);
            tvEventLocation = itemView.findViewById(R.id.tvEventLocation);
            tvEventDuration = itemView.findViewById(R.id.tvEventDuration);
            viewColorDot    = itemView.findViewById(R.id.viewColorDot);
            cardEvent       = itemView.findViewById(R.id.cardEvent);
        }
    }
}