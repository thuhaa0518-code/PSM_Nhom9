package com.example.pms_nhom9.models;

public class NotificationItem {

    public enum Type { UPCOMING, URGENT, DONE }

    private int eventId;
    private String title;
    private String subtitle;
    private String timeLabel;
    private Type type;
    private boolean completed;
    private String eventColor; // màu danh mục của sự kiện

    public NotificationItem(int eventId, String title, String subtitle,
                            String timeLabel, Type type) {
        this.eventId   = eventId;
        this.title     = title;
        this.subtitle  = subtitle;
        this.timeLabel = timeLabel;
        this.type      = type;
        this.completed = false;
        this.eventColor = null;
    }

    public int getEventId()        { return eventId; }
    public String getTitle()       { return title; }
    public String getSubtitle()    { return subtitle; }
    public String getTimeLabel()   { return timeLabel; }
    public Type getType()          { return type; }
    public boolean isCompleted()   { return completed; }
    public void setCompleted(boolean v) { this.completed = v; }
    public void setType(Type t)    { this.type = t; }
    public String getEventColor()  { return eventColor; }
    public void setEventColor(String c) { this.eventColor = c; }
}
