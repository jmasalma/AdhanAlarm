package islam.adhanalarm.handler;

import java.util.GregorianCalendar;
import fi.joensuu.joyds1.calendar.Calendar;

public class ScheduleData {
    public final GregorianCalendar[] schedule;
    public final boolean[] extremes;
    public final Calendar hijriDate;
    public final short nextTimeIndex;

    public ScheduleData(GregorianCalendar[] schedule, boolean[] extremes, Calendar hijriDate, short nextTimeIndex) {
        this.schedule = schedule;
        this.extremes = extremes;
        this.hijriDate = hijriDate;
        this.nextTimeIndex = nextTimeIndex;
    }
}