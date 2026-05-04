package com.example.pms_nhom9.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "events")
public class Event {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int userId;         // liên kết với User
    private String title;       // tên sự kiện
    private String location;    // địa điểm
    private String note;        // ghi chú
    private long startTime;     // timestamp milliseconds
    private long endTime;       // timestamp milliseconds
    private String color;       // mã màu hex, vd: "#E24B4A"
    private int priority;       // 0=thấp, 1=trung bình, 2=cao
    private boolean isRepeat;   // có lặp lại không
    private String repeatDays;  // "2,3,4,5,6" = T2 T3 T4 T5 T6
    private boolean isCompleted;
    private int reminderMinutes; // số phút nhắc trước (0 = không nhắc)

    public Event(int userId, String title, String location, String note,
                 long startTime, long endTime, String color,
                 int priority, boolean isRepeat, String repeatDays) {
        this.userId     = userId;
        this.title      = title;
        this.location   = location;
        this.note       = note;
        this.startTime  = startTime;
        this.endTime    = endTime;
        this.color      = color;
        this.priority   = priority;
        this.isRepeat   = isRepeat;
        this.repeatDays = repeatDays;
        this.isCompleted = false;
    }

    // --- Getters & Setters ---
    public int getId()                      { return id; }
    public void setId(int id)               { this.id = id; }
    public int getUserId()                  { return userId; }
    public void setUserId(int v)            { this.userId = v; }
    public String getTitle()                { return title; }
    public void setTitle(String v)          { this.title = v; }
    public String getLocation()             { return location; }
    public void setLocation(String v)       { this.location = v; }
    public String getNote()                 { return note; }
    public void setNote(String v)           { this.note = v; }
    public long getStartTime()              { return startTime; }
    public void setStartTime(long v)        { this.startTime = v; }
    public long getEndTime()                { return endTime; }
    public void setEndTime(long v)          { this.endTime = v; }
    public String getColor()                { return color; }
    public void setColor(String v)          { this.color = v; }
    public int getPriority()                { return priority; }
    public void setPriority(int v)          { this.priority = v; }
    public boolean isRepeat()               { return isRepeat; }
    public void setRepeat(boolean v)        { this.isRepeat = v; }
    public String getRepeatDays()           { return repeatDays; }
    public void setRepeatDays(String v)     { this.repeatDays = v; }
    public boolean isCompleted()            { return isCompleted; }
    public void setCompleted(boolean v)     { this.isCompleted = v; }
    public int getReminderMinutes()         { return reminderMinutes; }
    public void setReminderMinutes(int v)   { this.reminderMinutes = v; }
}


