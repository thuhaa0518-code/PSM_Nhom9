package com.example.pms_nhom9.adapters;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pms_nhom9.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DateStripAdapter extends RecyclerView.Adapter<DateStripAdapter.DateViewHolder> {

    // Danh sách 7 ngày (Calendar objects)
    private final List<Calendar> days;
    private int selectedPosition = 0;
    private final OnDateClickListener listener;

    public interface OnDateClickListener {
        void onDateClick(Calendar date, int position);
    }

    public DateStripAdapter(List<Calendar> days, OnDateClickListener listener) {
        this.days     = days;
        this.listener = listener;
        // Tìm index của hôm nay trong list
        Calendar today = Calendar.getInstance();
        for (int i = 0; i < days.size(); i++) {
            Calendar d = days.get(i);
            if (d.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                    && d.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                this.selectedPosition = i;
                break;
            }
        }
    }

    @NonNull
    @Override
    public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_date, parent, false);
        return new DateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DateViewHolder holder, int position) {
        Calendar cal = days.get(position);

        // Tên thứ: T2, T3 ... CN
        String[] dayNames = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1=CN, 2=T2...
        holder.tvDayName.setText(dayNames[dayOfWeek - 1]);

        // Số ngày
        holder.tvDayNumber.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));

        // Kiểm tra hôm nay
        Calendar today = Calendar.getInstance();
        boolean isToday = cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);

        // Highlight ngày được chọn
        if (position == selectedPosition) {
            holder.tvDayNumber.setBackgroundResource(R.drawable.bg_date_selected);
            holder.tvDayNumber.setTextColor(0xFFFFFFFF);
        } else if (isToday) {
            holder.tvDayNumber.setBackgroundResource(R.drawable.bg_date_today);
            holder.tvDayNumber.setTextColor(0xFFA855F7);
        } else {
            holder.tvDayNumber.setBackgroundResource(R.drawable.bg_date_normal);
            holder.tvDayNumber.setTextColor(0xFF111827);
        }

        holder.itemView.setOnClickListener(v -> {
            int prev = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);
            listener.onDateClick(cal, selectedPosition);
        });
    }

    @Override
    public int getItemCount() { return days.size(); }

    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayName, tvDayNumber;

        DateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayName   = itemView.findViewById(R.id.tvDayName);
            tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
        }
    }
}