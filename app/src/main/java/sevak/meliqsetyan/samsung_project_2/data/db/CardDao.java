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

    @Query("SELECT * FROM cards WHERE type = 'WORK' AND isSelf = 1 LIMIT 1")
    CardEntity getSelfWorkCard();

    @Query("SELECT * FROM cards WHERE isSelf = 0 ORDER BY createdAtEpochMs DESC")
    LiveData<List<CardEntity>> observeOtherCards();

    @Query("SELECT * FROM cards WHERE isSelf = 0")
    List<CardEntity> getOtherCardsSync();

    @Query("SELECT * FROM cards WHERE isSelf = 1")
    List<CardEntity> getSelfCardsSync();

    @Query("SELECT * FROM cards WHERE isSelf = 1 AND ownerUid IS NULL")
    List<CardEntity> getSelfCardsMissingUid();

    @Query("SELECT * FROM cards WHERE ownerUid = :uid AND isSelf = 0 LIMIT 1")
    CardEntity getByOwnerUidSync(String uid);

    @Insert
    long insert(CardEntity entity);

    @Update
    void update(CardEntity entity);

    @Query("DELETE FROM cards WHERE id = :cardId")
    void deleteById(long cardId);
}

