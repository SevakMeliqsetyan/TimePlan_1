package sevak.meliqsetyan.samsung_project_2.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

public final class TimeUtils {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM, EEE");
    private static final DateTimeFormatter DATE_LONG_FMT = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private TimeUtils() {}

    public static long todayEpochDay() {
        return LocalDate.now(ZoneId.systemDefault()).toEpochDay();
    }

    public static String formatTimeMinutes(int minutesFromMidnight) {
        int h = Math.max(0, minutesFromMidnight) / 60;
        int m = Math.max(0, minutesFromMidnight) % 60;
        return String.format("%02d:%02d", h, m);
    }

    /**
     * @return minutes from midnight, or -1 if value can't be parsed.
     */
    public static int parseTimeToMinutes(String hhmm) {
        if (hhmm == null) return -1;
        String v = hhmm.trim();
        if (v.isEmpty()) return -1;
        try {
            LocalTime t = LocalTime.parse(v, TIME_FMT);
            return t.getHour() * 60 + t.getMinute();
        } catch (Exception e) {
            return -1;
        }
    }

    public static String formatEpochDayShort(long epochDay) {
        return LocalDate.ofEpochDay(epochDay).format(DATE_FMT);
    }

    public static String formatEpochDayLong(long epochDay) {
        return LocalDate.ofEpochDay(epochDay).format(DATE_LONG_FMT);
    }

    public static long plusMonthsFromTodayEpochDay(int months) {
        return LocalDate.now(ZoneId.systemDefault()).plusMonths(months).toEpochDay();
    }

    public static long epochDayToMillis(long epochDay) {
        return LocalDate.ofEpochDay(epochDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}