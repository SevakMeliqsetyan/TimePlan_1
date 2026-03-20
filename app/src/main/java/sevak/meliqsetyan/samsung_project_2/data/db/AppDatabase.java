package sevak.meliqsetyan.samsung_project_2.data.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                CardEntity.class,
                UserProfileEntity.class,
                PersonalTaskEntity.class,
                WorkBookingEntity.class
        },
        version = 3,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract CardDao cardDao();
    public abstract UserProfileDao userProfileDao();
    public abstract PersonalTaskDao personalTaskDao();
    public abstract WorkBookingDao workBookingDao();
}

