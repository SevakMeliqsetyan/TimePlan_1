package sevak.meliqsetyan.samsung_project_2.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface UserProfileDao {

    @Query("SELECT * FROM user_profile ORDER BY createdAtEpochMs DESC LIMIT 1")
    LiveData<UserProfileEntity> observeFirst();

    @Query("SELECT * FROM user_profile ORDER BY createdAtEpochMs DESC LIMIT 1")
    UserProfileEntity getFirstSync();

    @Insert
    long insert(UserProfileEntity entity);
}

