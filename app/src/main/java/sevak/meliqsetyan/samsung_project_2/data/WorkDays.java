package sevak.meliqsetyan.samsung_project_2.data;

import java.util.Calendar;

public final class WorkDays {
    private WorkDays() {}

    public static final int MON = 1 << 0;
    public static final int TUE = 1 << 1;
    public static final int WED = 1 << 2;
    public static final int THU = 1 << 3;
    public static final int FRI = 1 << 4;
    public static final int SAT = 1 << 5;
    public static final int SUN = 1 << 6;

    public static int defaultMonToFri() {
        return MON | TUE | WED | THU | FRI;
    }

    public static boolean isEnabled(int mask, int dayFlag) {
        return (mask & dayFlag) != 0;
    }

    public static int setEnabled(int mask, int dayFlag, boolean enabled) {
        if (enabled) return mask | dayFlag;
        return mask & ~dayFlag;
    }

    public static int flagFromCalendarDayOfWeek(int calendarDayOfWeek) {
        switch (calendarDayOfWeek) {
            case Calendar.MONDAY:
                return MON;
            case Calendar.TUESDAY:
                return TUE;
            case Calendar.WEDNESDAY:
                return WED;
            case Calendar.THURSDAY:
                return THU;
            case Calendar.FRIDAY:
                return FRI;
            case Calendar.SATURDAY:
                return SAT;
            case Calendar.SUNDAY:
            default:
                return SUN;
        }
    }
}

