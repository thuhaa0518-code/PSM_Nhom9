package com.example.pms_nhom9.api.model;

public class EventResponse {
    public int    id;
    public int    userId;
    public String title;
    public String location;
    public String note;
    public long   startTime;
    public long   endTime;
    public String color;
    public int    priority;
    public int    isRepeat;
    public String repeatDays;
    public int    isCompleted;
    public int    reminderMinutes;

    // Convert sang Event model của Room (để dùng chung adapter)
    public com.example.pms_nhom9.models.Event toEvent() {
        com.example.pms_nhom9.models.Event e = new com.example.pms_nhom9.models.Event(
                userId, title, location, note,
                startTime, endTime, color, priority,
                isRepeat == 1, repeatDays);
        e.setId(id);
        e.setCompleted(isCompleted == 1);
        e.setReminderMinutes(reminderMinutes);
        return e;
    }
}
