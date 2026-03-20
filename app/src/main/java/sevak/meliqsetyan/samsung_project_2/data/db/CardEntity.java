package sevak.meliqsetyan.samsung_project_2.data.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore; // Важно добавить этот импорт
import androidx.room.PrimaryKey;

@Entity(tableName = "cards")
public class CardEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String type;
    @NonNull
    public String title;
    public long createdAtEpochMs;
    public boolean isSelf;

    @Nullable
    public String firstName;
    @Nullable
    public String lastName;
    @Nullable
    public Integer age;
    @Nullable
    public String profession;
    public int sessionMinutes;
    public int workDaysMask;
    public int workStartMinutes;
    public int workEndMinutes;
    @Nullable
    public Integer breakStartMinutes;
    @Nullable
    public Integer breakEndMinutes;

    @Ignore // Room проигнорирует этот конструктор и ошибки не будет
    public CardEntity(@NonNull String type, @NonNull String title, long createdAtEpochMs) {
        this(type, title, createdAtEpochMs, false);
    }

    public CardEntity(@NonNull String type, @NonNull String title, long createdAtEpochMs, boolean isSelf) {
        this.type = type;
        this.title = title;
        this.createdAtEpochMs = createdAtEpochMs;
        this.isSelf = isSelf;
        this.sessionMinutes = 90;
        this.workDaysMask = 0;
        this.workStartMinutes = 9 * 60;
        this.workEndMinutes = 20 * 60;
    }
}