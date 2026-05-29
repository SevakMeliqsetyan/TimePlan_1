package sevak.meliqsetyan.samsung_project_2.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class TimeUtils {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd MMM, EEE", Locale.getDefault());
    private static final SimpleDateFormat DATE_LONG_FMT = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm", Locale.getDefault());

    static {
        DATE_FMT.setTimeZone(UTC);
        DATE_LONG_FMT.setTimeZone(UTC);
    }

    private TimeUtils() {}

    public static long todayEpochDay() {
        return toEpochDay(Calendar.getInstance());
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
        Calendar c = Calendar.getInstance(UTC);
        c.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis() / (24 * 60 * 60 * 1000L);
    }

    /**
     * Возвращает абсолютное время (millis) для указанного дня и минут,
     * учитывая текущую временную зону пользователя.
     */
    public static long getLocalMillis(long epochDay, int timeMinutes) {
        Calendar utcCal = Calendar.getInstance(UTC);
        utcCal.setTimeInMillis(epochDay * 24 * 60 * 60 * 1000L);

        Calendar localCal = Calendar.getInstance();
        localCal.set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        localCal.set(Calendar.MILLISECOND, 0);
        localCal.add(Calendar.MINUTE, timeMinutes);
        return localCal.getTimeInMillis();
    }
}
