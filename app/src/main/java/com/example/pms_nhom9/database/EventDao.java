package com.example.pms_nhom9.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.pms_nhom9.models.Event;

import java.util.List;

@Dao
public interface EventDao {

    @Insert
    long insertEvent(Event event);

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    long insertOrReplaceEvent(Event event);

    @Update
    void updateEvent(Event event);

    @Delete
    void deleteEvent(Event event);

    @Query("SELECT * FROM events WHERE id = :id")
    Event getEventById(int id);

    // Lấy tất cả sự kiện của user trong 1 ngày
    // startOfDay và endOfDay là timestamp đầu/cuối ngày
    @Query("SELECT * FROM events WHERE userId = :userId " +
            "AND startTime >= :startOfDay AND startTime <= :endOfDay " +
            "ORDER BY startTime ASC")
    LiveData<List<Event>> getEventsByDay(int userId, long startOfDay, long endOfDay);

    // Lấy sự kiện ưu tiên cao trong ngày
    @Query("SELECT * FROM events WHERE userId = :userId AND priority = 2 " +
            "AND startTime >= :startOfDay AND startTime <= :endOfDay " +
            "ORDER BY startTime ASC")
    LiveData<List<Event>> getHighPriorityEvents(int userId, long startOfDay, long endOfDay);

    // Đếm số sự kiện trong ngày (dùng cho badge)
    @Query("SELECT COUNT(*) FROM events WHERE userId = :userId " +
            "AND startTime >= :startOfDay AND startTime <= :endOfDay")
    int countEventsByDay(int userId, long startOfDay, long endOfDay);

    // Lấy sự kiện theo tháng (cho màn Calendar)
    @Query("SELECT * FROM events WHERE userId = :userId " +
            "AND startTime >= :startOfMonth AND startTime <= :endOfMonth " +
            "ORDER BY startTime ASC")
    LiveData<List<Event>> getEventsByMonth(int userId, long startOfMonth, long endOfMonth);

    // Thêm vào EventDao.java

    // Query đồng bộ (không LiveData) — dùng cho CalendarFragment
    @Query("SELECT * FROM events WHERE userId = :userId " +
            "AND startTime >= :from AND startTime <= :to " +
            "ORDER BY startTime ASC")
    List<Event> getEventsInRangeSync(int userId, long from, long to);

    // Kiểm tra xung đột thời gian (trừ chính sự kiện đang sửa)
    @Query("SELECT * FROM events WHERE userId = :userId " +
            "AND id != :excludeId " +
            "AND startTime < :endTime AND endTime > :startTime " +
            "LIMIT 1")
    Event getConflictingEvent(int userId, long startTime, long endTime, int excludeId);

    // Xóa toàn bộ sự kiện của một user (dùng khi sync lại từ server)
    @Query("DELETE FROM events WHERE userId = :userId")
    void deleteAllByUser(int userId);
}