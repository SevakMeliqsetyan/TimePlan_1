package sevak.meliqsetyan.samsung_project_2.data.db;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "personal_tasks",
        indices = {
                @Index(value = {"cardId", "dateEpochDay"})
        }
)
public class PersonalTaskEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long cardId;
    public long dateEpochDay; // LocalDate.toEpochDay()
    public int timeMinutes; // minutes from 00:00
    public String title;

    public PersonalTaskEntity(long cardId, long dateEpochDay, int timeMinutes, String title) {
        this.cardId = cardId;
        this.dateEpochDay = dateEpochDay;
        this.timeMinutes = timeMinutes;
        this.title = title;
    }
}

