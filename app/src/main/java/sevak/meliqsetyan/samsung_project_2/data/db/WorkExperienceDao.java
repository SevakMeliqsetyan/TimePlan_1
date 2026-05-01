package sevak.meliqsetyan.samsung_project_2.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface WorkExperienceDao {
    @Insert
    long insert(WorkExperienceEntity experience);

    @Query("SELECT * FROM work_experience WHERE cardId = :cardId")
    LiveData<List<WorkExperienceEntity>> observeByCardId(long cardId);

    @Query("DELETE FROM work_experience WHERE id = :id")
    void deleteById(long id);
}