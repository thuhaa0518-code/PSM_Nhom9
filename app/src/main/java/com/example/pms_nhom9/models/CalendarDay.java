package com.example.pms_nhom9.models;


import java.util.List;

public class CalendarDay {

    private int dayOfMonth;     // 0 = ô trống (padding đầu tháng)
    private long dateMillis;    // timestamp của ngày này
    private boolean isToday;
    private boolean isSelected;
    private boolean isCurrentMonth;

    // Danh sách màu sự kiện trong ngày (tối đa 3)
    private List<String> eventColors;

    public CalendarDay(int dayOfMonth, long dateMillis,
                       boolean isToday, boolean isCurrentMonth) {
        this.dayOfMonth      = dayOfMonth;
        this.dateMillis      = dateMillis;
        this.isToday         = isToday;
        this.isCurrentMonth  = isCurrentMonth;
        this.isSelected      = false;
        this.eventColors     = new java.util.ArrayList<>();
    }

    // Constructor rút gọn cho ô trống (padding)
    public CalendarDay(int dayOfMonth, boolean isCurrentMonth) {
        this(dayOfMonth, 0L, false, isCurrentMonth);
    }

    // Getters & Setters
    public int getDay()                     { return dayOfMonth; }
    public int getDayOfMonth()              { return dayOfMonth; }
    public long getDateMillis()             { return dateMillis; }
    public boolean isToday()               { return isToday; }
    public void setToday(boolean v)        { this.isToday = v; }
    public boolean isSelected()            { return isSelected; }
    public void setSelected(boolean v)     { this.isSelected = v; }
    public boolean isCurrentMonth()        { return isCurrentMonth; }
    public List<String> getEventColors()   { return eventColors; }
    public void setEventColors(List<String> v) { this.eventColors = v; }
}