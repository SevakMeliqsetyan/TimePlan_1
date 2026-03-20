package sevak.meliqsetyan.samsung_project_2.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PersonalTaskDao {
    @Query("SELECT * FROM personal_tasks WHERE cardId = :cardId AND dateEpochDay = :dateEpochDay ORDER BY timeMinutes ASC, id ASC")
    LiveData<List<PersonalTaskEntity>> observeForDay(long cardId, long dateEpochDay);

    @Insert
    long insert(PersonalTaskEntity entity);

    @Delete
    void delete(PersonalTaskEntity entity);

    @Query("DELETE FROM personal_tasks WHERE id = :taskId")
    void deleteById(long taskId);
}

