package com.example.pms_nhom9.fragments;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.pms_nhom9.database.AppDatabase;
import com.example.pms_nhom9.database.EventDao;
import com.example.pms_nhom9.models.Event;

import java.util.Calendar;
import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final EventDao eventDao;
    private final int userId;

    // Ngày đang được chọn (mặc định là hôm nay)
    private final MutableLiveData<Long> selectedDate = new MutableLiveData<>();

    // LiveData reactive: tự cập nhật khi selectedDate thay đổi
    private final LiveData<List<Event>> eventsForSelectedDay;

    public HomeViewModel(Application application) {
        super(application);
        eventDao = AppDatabase.getInstance(application).eventDao();

        // Lấy userId từ SharedPreferences
        SharedPreferences prefs = application
                .getSharedPreferences("psm_prefs", 0);
        userId = prefs.getInt("logged_user_id", -1);

        // Mặc định chọn hôm nay
        selectedDate.setValue(System.currentTimeMillis());

        // switchMap: mỗi khi selectedDate đổi → query lại DB
        eventsForSelectedDay = Transformations.switchMap(selectedDate, dateMillis -> {
            long[] range = getDayRange(dateMillis);
            return eventDao.getEventsByDay(userId, range[0], range[1]);
        });
    }

    public void selectDate(long dateMillis) {
        selectedDate.setValue(dateMillis);
    }

    public long getSelectedDate() {
        return selectedDate.getValue() != null
                ? selectedDate.getValue()
                : System.currentTimeMillis();
    }

    // Lấy sự kiện của ngày đang chọn (reactive)
    public LiveData<List<Event>> getEventsForSelectedDay() {
        return eventsForSelectedDay;
    }

    // Lấy sự kiện theo ngày cụ thể (dùng khi đổi ngày)
    public LiveData<List<Event>> getEventsByDay(long dateMillis) {
        long[] range = getDayRange(dateMillis);
        return eventDao.getEventsByDay(userId, range[0], range[1]);
    }

    // Trả về [startOfDay, endOfDay] dạng timestamp
    public long[] getDayRange(long dateMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dateMillis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        long end = cal.getTimeInMillis();

        return new long[]{start, end};
    }

    public int getUserId() { return userId; }
}