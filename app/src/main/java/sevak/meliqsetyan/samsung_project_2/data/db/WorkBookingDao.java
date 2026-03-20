package sevak.meliqsetyan.samsung_project_2.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface WorkBookingDao {
    @Query("SELECT * FROM work_bookings WHERE cardId = :cardId AND dateEpochDay = :dateEpochDay ORDER BY startMinutes ASC")
    LiveData<List<WorkBookingEntity>> observeForDay(long cardId, long dateEpochDay);

    @Query("SELECT * FROM work_bookings WHERE cardId = :cardId AND dateEpochDay BETWEEN :fromEpochDay AND :toEpochDay")
    List<WorkBookingEntity> getBetweenDaysSync(long cardId, long fromEpochDay, long toEpochDay);

    @Query("SELECT COUNT(*) FROM work_bookings WHERE cardId = :cardId AND dateEpochDay = :dateEpochDay")
    int countForDaySync(long cardId, long dateEpochDay);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(WorkBookingEntity entity);

    @Query("UPDATE work_bookings SET clientName = :clientName WHERE cardId = :cardId AND dateEpochDay = :dateEpochDay AND startMinutes = :startMinutes")
    void updateClientName(long cardId, long dateEpochDay, int startMinutes, String clientName);

    @Query("DELETE FROM work_bookings WHERE cardId = :cardId AND dateEpochDay = :dateEpochDay AND startMinutes = :startMinutes")
    void deleteSlot(long cardId, long dateEpochDay, int startMinutes);
}

