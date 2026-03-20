package sevak.meliqsetyan.samsung_project_2.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_profile")
public class UserProfileEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String firstName;

    @NonNull
    public String lastName;

    public long createdAtEpochMs;

    public UserProfileEntity(@NonNull String firstName, @NonNull String lastName, long createdAtEpochMs) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.createdAtEpochMs = createdAtEpochMs;
    }
}

