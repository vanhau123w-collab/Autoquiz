package com.example.myapplication.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {User.class, Quiz.class, QuizResult.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract UserDao userDao();
    public abstract QuizDao quizDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, "autoquiz_db")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries() // Cho phep chay tren main thread cho don gian, nhung khuyen khich dung background thread sau nay
                    .build();
        }
        return instance;
    }
}
