package sevak.meliqsetyan.samsung_project_2.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class TimeUtils {
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd MMM, EEE", Locale.getDefault());
    private static final SimpleDateFormat DATE_LONG_FMT = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private TimeUtils() {}

    public static long todayEpochDay() {
        Calendar cal = Calendar.getInstance();
        return toEpochDay(cal);
    }

    public static String formatTimeMinutes(int minutesFromMidnight) {
        int h = Math.max(0, minutesFromMidnight) / 60;
        int m = Math.max(0, minutesFromMidnight) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", h, m);
    }

    public static int parseTimeToMinutes(String hhmm) {
        if (hhmm == null || hhmm.trim().isEmpty()) return -1;
        try {
            String[] parts = hhmm.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            return h * 60 + m;
        } catch (Exception e) {
            return -1;
        }
    }

    public static String formatEpochDayShort(long epochDay) {
        return DATE_FMT.format(new Date(epochDayToMillis(epochDay)));
    }

    public static String formatEpochDayLong(long epochDay) {
        return DATE_LONG_FMT.format(new Date(epochDayToMillis(epochDay)));
    }

    public static long epochDayToMillis(long epochDay) {
        return epochDay * 24 * 60 * 60 * 1000L;
    }

    public static long toEpochDay(Calendar cal) {
        // Очищаем время для точного расчета дня
        Calendar c = (Calendar) cal.clone();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis() / (24 * 60 * 60 * 1000L);
    }
}
