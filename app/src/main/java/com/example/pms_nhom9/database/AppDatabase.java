package com.example.pms_nhom9.database;


import android.content.Context;


import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;


import com.example.pms_nhom9.models.Event;
import com.example.pms_nhom9.models.User;


@Database(entities = {User.class, Event.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {


    private static AppDatabase instance;


    public abstract UserDao userDao();
    public abstract EventDao eventDao();


    // Singleton — chỉ tạo 1 instance duy nhất trong toàn app
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "psm_database"
                    )
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }
}

