package sevak.meliqsetyan.samsung_project_2.data.db;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "work_bookings",
        indices = {
                @Index(value = {"cardId", "dateEpochDay"}),
                @Index(value = {"cardId", "dateEpochDay", "startMinutes"}, unique = true)
        }
)
public class WorkBookingEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long cardId;
    public long dateEpochDay; // LocalDate.toEpochDay()
    public int startMinutes; // start minute of slot
    public String clientName;

    public WorkBookingEntity(long cardId, long dateEpochDay, int startMinutes, String clientName) {
        this.cardId = cardId;
        this.dateEpochDay = dateEpochDay;
        this.startMinutes = startMinutes;
        this.clientName = clientName;
    }
}

