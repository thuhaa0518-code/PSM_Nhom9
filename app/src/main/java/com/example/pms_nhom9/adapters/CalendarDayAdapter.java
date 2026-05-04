package com.example.pms_nhom9.adapters;


import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pms_nhom9.R;
import com.example.pms_nhom9.models.CalendarDay;

import java.util.List;

public class CalendarDayAdapter
        extends RecyclerView.Adapter<CalendarDayAdapter.DayViewHolder> {

    private final List<CalendarDay> days;
    private int selectedPosition = -1;
    private final OnDayClickListener listener;

    public interface OnDayClickListener {
        void onDayClick(CalendarDay day, int position);
    }

    public CalendarDayAdapter(List<CalendarDay> days, OnDayClickListener listener) {
        this.days     = days;
        this.listener = listener;
    }

    public void setSelectedPosition(int pos) {
        int prev = selectedPosition;
        selectedPosition = pos;
        if (prev >= 0) notifyItemChanged(prev);
        notifyItemChanged(selectedPosition);
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder h, int position) {
        CalendarDay day = days.get(position);

        // Ô trống (padding đầu tháng)
        if (day.getDay() == 0) {
            h.tvDay.setText("");
            h.tvDay.setBackground(null);
            h.dot1.setVisibility(View.GONE);
            h.dot2.setVisibility(View.GONE);
            h.dot3.setVisibility(View.GONE);
            return;
        }

        h.tvDay.setText(String.valueOf(day.getDay()));

        // Màu chữ: xám nếu không thuộc tháng hiện tại
        if (!day.isCurrentMonth()) {
            h.tvDay.setTextColor(0xFFB4B2A9);
        } else {
            h.tvDay.setTextColor(0xFF2C2C2A);
        }

        // Background: tím nếu selected, vàng nhạt nếu today, trong suốt bình thường
        if (position == selectedPosition) {
            h.tvDay.setBackgroundResource(R.drawable.bg_date_selected);
            h.tvDay.setTextColor(0xFFFFFFFF);
        } else if (day.isToday()) {
            h.tvDay.setBackgroundResource(R.drawable.bg_date_today);
            h.tvDay.setTextColor(0xFFA855F7);
        } else {
            h.tvDay.setBackgroundResource(R.drawable.bg_date_normal);
        }

        // Hiển thị chấm màu sự kiện (tối đa 3)
        List<String> colors = day.getEventColors();
        View[] dots = {h.dot1, h.dot2, h.dot3};
        for (int i = 0; i < 3; i++) {
            if (colors != null && i < colors.size()) {
                dots[i].setVisibility(View.VISIBLE);
                try {
                    dots[i].setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                    Color.parseColor(colors.get(i))));
                } catch (Exception e) {
                    dots[i].setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFF7F77DD));
                }
            } else {
                dots[i].setVisibility(View.GONE);
            }
        }

        // Click chọn ngày
        h.itemView.setOnClickListener(v -> {
            setSelectedPosition(h.getAdapterPosition());
            listener.onDayClick(day, h.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() { return days.size(); }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay;
        View dot1, dot2, dot3;

        DayViewHolder(@NonNull View v) {
            super(v);
            tvDay = v.findViewById(R.id.tvCalDay);
            dot1  = v.findViewById(R.id.dot1);
            dot2  = v.findViewById(R.id.dot2);
            dot3  = v.findViewById(R.id.dot3);
        }
    }
}