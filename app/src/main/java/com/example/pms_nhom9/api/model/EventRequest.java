package com.example.pms_nhom9.api.model;

public class EventRequest {
    public String title;
    public String location;
    public String note;
    public long   startTime;
    public long   endTime;
    public String color;
    public int    priority;
    public boolean isRepeat;
    public String repeatDays;
    public boolean isCompleted;
    public int    reminderMinutes;
}
