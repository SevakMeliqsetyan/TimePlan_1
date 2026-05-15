package sevak.meliqsetyan.samsung_project_2.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BookingRequestDao {
    @Query("SELECT * FROM booking_requests WHERE cardId = :cardId AND isPending = 1")
    LiveData<List<BookingRequestEntity>> observePendingForCard(long cardId);

    @Insert
    long insert(BookingRequestEntity request);

    @Update
    void update(BookingRequestEntity request);

    @Query("DELETE FROM booking_requests WHERE id = :id")
    void delete(long id);
}
