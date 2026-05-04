package com.example.pms_nhom9.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pms_nhom9.R;
import com.example.pms_nhom9.models.NotificationItem;

import java.util.List;

public class NotificationAdapter
        extends RecyclerView.Adapter<NotificationAdapter.VH> {

    public interface Listener {
        void onSnooze(NotificationItem item);
        void onViewDetail(NotificationItem item);
        void onDismiss(NotificationItem item);
        void onMarkDone(NotificationItem item);
    }

    private final List<NotificationItem> items;
    private final Listener listener;

    public NotificationAdapter(List<NotificationItem> items, Listener listener) {
        this.items    = items;
        this.listener = listener;
    }

    public void updateItems(List<NotificationItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    /**
     * Tạo background card với màu nhạt từ màu danh mục sự kiện.
     * Nền = màu event pha 20% opacity, viền = màu event 60% opacity.
     */
    private void applyEventColorCard(View cardRoot, String eventColor) {
        try {
            int base = Color.parseColor(eventColor);
            int r = Color.red(base);
            int g = Color.green(base);
            int b = Color.blue(base);

            // Nền nhạt: pha với trắng 80%
            int bgColor = Color.argb(255,
                    (int)(r * 0.18 + 255 * 0.82),
                    (int)(g * 0.18 + 255 * 0.82),
                    (int)(b * 0.18 + 255 * 0.82));
            // Viền: màu gốc 60% opacity
            int strokeColor = Color.argb(180, r, g, b);

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.RECTANGLE);
            drawable.setCornerRadius(48f);
            drawable.setColor(bgColor);
            drawable.setStroke(4, strokeColor);
            cardRoot.setBackground(drawable);
        } catch (Exception e) {
            // fallback
            cardRoot.setBackgroundResource(R.drawable.bg_notif_card_blue);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        NotificationItem item = items.get(position);

        h.tvTitle.setText(item.getTitle());
        h.tvSubtitle.setText(item.getSubtitle());
        h.tvTime.setText(item.getTimeLabel());

        String eventColor = item.getEventColor();

        switch (item.getType()) {
            case UPCOMING:
                // Dùng màu danh mục nếu có, fallback về xanh tím
                if (eventColor != null && !eventColor.isEmpty()) {
                    applyEventColorCard(h.cardRoot, eventColor);
                } else {
                    h.cardRoot.setBackgroundResource(R.drawable.bg_notif_card_blue);
                }
                h.tvLabel.setText("Sắp tới");
                h.tvLabel.setTextColor(0xFF5555DD);
                if (h.tvIcon != null) h.tvIcon.setText("");
                if (h.layoutActions != null) {
                    h.layoutActions.setGravity(android.view.Gravity.START);
                }
                h.btnAction1.setText("Nhắc lại");
                h.btnAction1.setBackgroundResource(R.drawable.bg_btn_yellow_light);
                h.btnAction1.setBackgroundTintList(null);
                h.btnAction1.setTextColor(0xFFD97706);
                h.btnAction1.setVisibility(View.VISIBLE);
                h.btnAction2.setText("Xem chi tiết");
                h.btnAction2.setBackgroundResource(R.drawable.bg_btn_urgent);
                h.btnAction2.setBackgroundTintList(null);
                h.btnAction2.setTextColor(0xFFFFFFFF);
                h.btnAction2.setVisibility(View.VISIBLE);
                h.btnAction3.setText("Bỏ qua");
                h.btnAction3.setBackgroundResource(R.drawable.bg_btn_gray_light);
                h.btnAction3.setBackgroundTintList(null);
                h.btnAction3.setTextColor(0xFF374151);
                h.btnAction3.setVisibility(View.VISIBLE);
                h.btnAction1.setOnClickListener(v -> listener.onSnooze(item));
                h.btnAction2.setOnClickListener(v -> listener.onViewDetail(item));
                h.btnAction3.setOnClickListener(v -> listener.onDismiss(item));
                break;

            case URGENT:
                // Đã qua giờ nhưng chưa hoàn thành
                if (eventColor != null && !eventColor.isEmpty()) {
                    applyEventColorCard(h.cardRoot, eventColor);
                } else {
                    h.cardRoot.setBackgroundResource(R.drawable.bg_notif_card_green);
                }
                h.tvLabel.setText("Quá hạn");
                h.tvLabel.setTextColor(0xFFE24B4A);
                if (h.tvIcon != null) h.tvIcon.setText("");
                h.btnAction1.setText("Nhắc lại");
                h.btnAction1.setBackgroundResource(R.drawable.bg_btn_yellow_light);
                h.btnAction1.setBackgroundTintList(null);
                h.btnAction1.setTextColor(0xFFD97706);
                h.btnAction1.setVisibility(View.VISIBLE);
                h.btnAction2.setText("Xem chi tiết");
                h.btnAction2.setBackgroundResource(R.drawable.bg_btn_urgent);
                h.btnAction2.setBackgroundTintList(null);
                h.btnAction2.setTextColor(0xFFFFFFFF);
                h.btnAction2.setVisibility(View.VISIBLE);
                h.btnAction3.setText("Đánh dấu xong");
                h.btnAction3.setBackgroundResource(R.drawable.bg_btn_gray_light);
                h.btnAction3.setBackgroundTintList(null);
                h.btnAction3.setTextColor(0xFF374151);
                h.btnAction3.setVisibility(View.VISIBLE);
                if (h.layoutActions != null) {
                    h.layoutActions.setGravity(android.view.Gravity.START);
                }
                h.btnAction1.setOnClickListener(v -> listener.onSnooze(item));
                h.btnAction2.setOnClickListener(v -> listener.onViewDetail(item));
                h.btnAction3.setOnClickListener(v -> listener.onMarkDone(item));
                break;

            case DONE:
                // Giữ nguyên màu gốc của sự kiện, KHÔNG đổi sang màu khác
                if (eventColor != null && !eventColor.isEmpty()) {
                    applyEventColorCard(h.cardRoot, eventColor);
                } else {
                    h.cardRoot.setBackgroundResource(R.drawable.bg_notif_card_green);
                }
                h.tvLabel.setText("Đã xong");
                h.tvLabel.setTextColor(0xFF22AA44);
                if (h.tvIcon != null) h.tvIcon.setText("");
                h.btnAction1.setVisibility(View.GONE);
                h.btnAction2.setText("Xem chi tiết");
                h.btnAction2.setBackgroundResource(R.drawable.bg_btn_urgent);
                h.btnAction2.setBackgroundTintList(null);
                h.btnAction2.setTextColor(0xFFFFFFFF);
                h.btnAction2.setVisibility(View.VISIBLE);
                h.btnAction3.setVisibility(View.GONE);
                if (h.layoutActions != null) {
                    h.layoutActions.setGravity(android.view.Gravity.CENTER);
                }
                h.btnAction2.setOnClickListener(v -> listener.onViewDetail(item));
                break;
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        View cardRoot;
        android.widget.LinearLayout layoutActions;
        TextView tvLabel, tvTime, tvTitle, tvSubtitle, tvIcon;
        Button btnAction1, btnAction2, btnAction3;

        VH(@NonNull View v) {
            super(v);
            cardRoot      = v.findViewById(R.id.cardRoot);
            layoutActions = v.findViewById(R.id.layoutActions);
            tvLabel       = v.findViewById(R.id.tvNotifLabel);
            tvTime        = v.findViewById(R.id.tvNotifTime);
            tvTitle       = v.findViewById(R.id.tvNotifTitle);
            tvSubtitle    = v.findViewById(R.id.tvNotifSubtitle);
            tvIcon        = v.findViewById(R.id.tvNotifIcon);
            btnAction1    = v.findViewById(R.id.btnAction1);
            btnAction2    = v.findViewById(R.id.btnAction2);
            btnAction3    = v.findViewById(R.id.btnAction3);
        }
    }
}
