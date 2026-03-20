package sevak.meliqsetyan.samsung_project_2.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CardDao {
    @Query("SELECT * FROM cards ORDER BY createdAtEpochMs DESC")
    LiveData<List<CardEntity>> observeAll();

    @Query("SELECT * FROM cards WHERE id = :cardId LIMIT 1")
    LiveData<CardEntity> observeById(long cardId);

    @Query("SELECT * FROM cards WHERE id = :cardId LIMIT 1")
    CardEntity getByIdSync(long cardId);

    @Query("SELECT * FROM cards WHERE type = :type AND isSelf = 1 ORDER BY createdAtEpochMs DESC LIMIT 1")
    LiveData<CardEntity> observeSelfByType(String type);

    @Query("SELECT * FROM cards WHERE type = :type AND isSelf = 1 ORDER BY createdAtEpochMs DESC LIMIT 1")
    CardEntity getSelfByTypeSync(String type);

    @Insert
    long insert(CardEntity entity);

    @Update
    void update(CardEntity entity);

    @Query("DELETE FROM cards WHERE id = :cardId")
    void deleteById(long cardId);
}

