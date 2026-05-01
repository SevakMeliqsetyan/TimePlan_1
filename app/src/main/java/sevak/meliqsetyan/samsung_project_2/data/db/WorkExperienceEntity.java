package sevak.meliqsetyan.samsung_project_2.data.db;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "work_experience",
        foreignKeys = @ForeignKey(
                entity = CardEntity.class,
                parentColumns = "id",
                childColumns = "cardId",
                onDelete = ForeignKey.CASCADE
        )
)
public class WorkExperienceEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long cardId;
    public String label;
    public String value;

    public WorkExperienceEntity(long cardId, String label, String value) {
        this.cardId = cardId;
        this.label = label;
        this.value = value;
    }
}