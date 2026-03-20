package sevak.meliqsetyan.samsung_project_2.data.db;

import android.content.Context;

import androidx.room.Room;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DbProvider {
    private static volatile AppDatabase db;
    private static final ExecutorService io = Executors.newSingleThreadExecutor();

    private DbProvider() {}

    public static AppDatabase db(Context context) {
        if (db == null) {
            synchronized (DbProvider.class) {
                if (db == null) {
                    db = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "samsung_planner.db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return db;
    }

    public static ExecutorService io() {
        return io;
    }
}

