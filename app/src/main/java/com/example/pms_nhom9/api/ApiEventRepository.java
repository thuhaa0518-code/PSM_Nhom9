package com.example.pms_nhom9.api;

import android.content.Context;

import com.example.pms_nhom9.api.model.EventRequest;
import com.example.pms_nhom9.api.model.EventResponse;
import com.example.pms_nhom9.api.model.MessageResponse;
import com.example.pms_nhom9.models.Event;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApiEventRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private final ApiService api;

    public ApiEventRepository(Context ctx) {
        this.api = ApiClient.getService(ctx);
    }

    // Lấy sự kiện theo khoảng thời gian
    public void getEvents(long from, long to, Callback<List<Event>> cb) {
        api.getEvents(from, to).enqueue(new retrofit2.Callback<List<EventResponse>>() {
            @Override
            public void onResponse(Call<List<EventResponse>> call,
                                   Response<List<EventResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Event> events = new ArrayList<>();
                    for (EventResponse r : response.body()) events.add(r.toEvent());
                    cb.onSuccess(events);
                } else {
                    cb.onError("Lỗi tải sự kiện: " + response.code());
                }
            }
            @Override public void onFailure(Call<List<EventResponse>> call, Throwable t) {
                cb.onError("Mất kết nối: " + t.getMessage());
            }
        });
    }

    // Tạo sự kiện mới
    public void createEvent(Event event, Callback<Event> cb) {
        api.createEvent(toRequest(event)).enqueue(new retrofit2.Callback<EventResponse>() {
            @Override
            public void onResponse(Call<EventResponse> call, Response<EventResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cb.onSuccess(response.body().toEvent());
                } else {
                    cb.onError("Lỗi tạo sự kiện");
                }
            }
            @Override public void onFailure(Call<EventResponse> call, Throwable t) {
                cb.onError("Mất kết nối");
            }
        });
    }

    // Cập nhật sự kiện
    public void updateEvent(Event event, Callback<Event> cb) {
        api.updateEvent(event.getId(), toRequest(event)).enqueue(new retrofit2.Callback<EventResponse>() {
            @Override
            public void onResponse(Call<EventResponse> call, Response<EventResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cb.onSuccess(response.body().toEvent());
                } else {
                    cb.onError("Lỗi cập nhật sự kiện");
                }
            }
            @Override public void onFailure(Call<EventResponse> call, Throwable t) {
                cb.onError("Mất kết nối");
            }
        });
    }

    // Xóa sự kiện
    public void deleteEvent(int eventId, Callback<String> cb) {
        api.deleteEvent(eventId).enqueue(new retrofit2.Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                if (response.isSuccessful()) cb.onSuccess("Đã xóa");
                else cb.onError("Lỗi xóa sự kiện");
            }
            @Override public void onFailure(Call<MessageResponse> call, Throwable t) {
                cb.onError("Mất kết nối");
            }
        });
    }

    private EventRequest toRequest(Event event) {
        EventRequest req = new EventRequest();
        req.title           = event.getTitle();
        req.location        = event.getLocation();
        req.note            = event.getNote();
        req.startTime       = event.getStartTime();
        req.endTime         = event.getEndTime();
        req.color           = event.getColor();
        req.priority        = event.getPriority();
        req.isRepeat        = event.isRepeat();
        req.repeatDays      = event.getRepeatDays();
        req.isCompleted     = event.isCompleted();
        req.reminderMinutes = event.getReminderMinutes();
        return req;
    }
}
