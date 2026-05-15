package sevak.meliqsetyan.samsung_project_2.data.db;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "booking_requests",
        foreignKeys = @ForeignKey(
                entity = CardEntity.class,
                parentColumns = "id",
                childColumns = "cardId",
                onDelete = ForeignKey.CASCADE
        )
)
public class BookingRequestEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long cardId; // ID of the work card being booked
    public long requesterCardId; // ID of the requester's work card
    public String requesterName;
    public long dateEpochDay;
    public int startMinutes;
    public boolean isPending;

    public BookingRequestEntity(long cardId, long requesterCardId, String requesterName, long dateEpochDay, int startMinutes) {
        this.cardId = cardId;
        this.requesterCardId = requesterCardId;
        this.requesterName = requesterName;
        this.dateEpochDay = dateEpochDay;
        this.startMinutes = startMinutes;
        this.isPending = true;
    }
}
