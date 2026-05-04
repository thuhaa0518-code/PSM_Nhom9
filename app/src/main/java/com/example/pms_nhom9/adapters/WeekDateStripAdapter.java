package com.example.pms_nhom9.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pms_nhom9.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Dải ngày 7 ô cho tab Tuần — giống style trang chủ (DateStripAdapter)
 * nhưng hỗ trợ hiển thị chấm màu sự kiện dưới số ngày.
 */
public class WeekDateStripAdapter
        extends RecyclerView.Adapter<WeekDateStripAdapter.VH> {

    public interface OnDayClickListener {
        void onDayClick(Calendar date, int position);
    }

    private final List<Calendar> days;
    private final List<List<String>> dayColors; // màu sự kiện mỗi ngày (tối đa 3)
    private int selectedPosition;
    private final OnDayClickListener listener;

    public WeekDateStripAdapter(List<Calendar> days, int selectedPosition,
                                OnDayClickListener listener) {
        this.days             = days;
        this.selectedPosition = selectedPosition;
        this.listener         = listener;
        this.dayColors        = new ArrayList<>();
        for (int i = 0; i < days.size(); i++) dayColors.add(new ArrayList<>());
    }

    /** Cập nhật chấm màu sự kiện cho từng ngày rồi refresh */
    public void updateColors(List<List<String>> colors) {
        dayColors.clear();
        for (List<String> c : colors) dayColors.add(new ArrayList<>(c));
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int pos) {
        int prev = selectedPosition;
        selectedPosition = pos;
        if (prev >= 0 && prev < days.size()) notifyItemChanged(prev);
        if (pos >= 0 && pos < days.size())   notifyItemChanged(pos);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_week_day, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Calendar cal = days.get(position);

        String[] dayNames = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
        int dow = cal.get(Calendar.DAY_OF_WEEK); // 1=CN, 2=T2...
        h.tvDayName.setText(dayNames[dow - 1]);

        // Màu chữ thứ: T7(7) và CN(1) đỏ
        boolean isWeekend = (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY);
        h.tvDayName.setTextColor(isWeekend ? 0xFFEF4444 : 0xFF9CA3AF);

        h.tvDayNumber.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));

        Calendar today = Calendar.getInstance();
        boolean isToday = cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);

        if (position == selectedPosition) {
            h.tvDayNumber.setBackgroundResource(R.drawable.bg_date_selected);
            h.tvDayNumber.setTextColor(0xFFFFFFFF);
        } else if (isToday) {
            h.tvDayNumber.setBackgroundResource(R.drawable.bg_date_today);
            h.tvDayNumber.setTextColor(0xFFA855F7);
        } else {
            h.tvDayNumber.setBackgroundResource(R.drawable.bg_date_normal);
            h.tvDayNumber.setTextColor(isWeekend ? 0xFFEF4444 : 0xFF111827);
        }

        // Chấm màu sự kiện
        List<String> colors = position < dayColors.size() ? dayColors.get(position) : new ArrayList<>();
        View[] dots = {h.dot1, h.dot2, h.dot3};
        for (int i = 0; i < 3; i++) {
            if (i < colors.size() && colors.get(i) != null) {
                dots[i].setVisibility(View.VISIBLE);
                try {
                    dots[i].setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                    Color.parseColor(colors.get(i))));
                } catch (Exception e) {
                    dots[i].setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFFA855F7));
                }
            } else {
                dots[i].setVisibility(View.GONE);
            }
        }

        h.itemView.setOnClickListener(v -> {
            setSelectedPosition(h.getAdapterPosition());
            listener.onDayClick(cal, h.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() { return days.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDayName, tvDayNumber;
        View dot1, dot2, dot3;

        VH(@NonNull View v) {
            super(v);
            tvDayName   = v.findViewById(R.id.tvDayName);
            tvDayNumber = v.findViewById(R.id.tvDayNumber);
            dot1        = v.findViewById(R.id.dot1);
            dot2        = v.findViewById(R.id.dot2);
            dot3        = v.findViewById(R.id.dot3);
        }
    }
}
