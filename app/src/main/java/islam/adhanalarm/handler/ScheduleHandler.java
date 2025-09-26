package islam.adhanalarm.handler;

import android.text.format.DateFormat;

import net.sourceforge.jitl.Jitl;
import net.sourceforge.jitl.Method;
import net.sourceforge.jitl.Prayer;
import net.sourceforge.jitl.astro.Location;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import islam.adhanalarm.CONSTANT;

public class ScheduleHandler {

    public static ScheduleData calculate(Location location, String calculationMethodIndex, String roundingTypeIndex, int offsetMinutes) {
        Method method = CONSTANT.CALCULATION_METHODS[Integer.parseInt(calculationMethodIndex)].copy();
        method.setRound(CONSTANT.ROUNDING_TYPES[Integer.parseInt(roundingTypeIndex)]);

        GregorianCalendar day = new GregorianCalendar();
        Jitl itl = new Jitl(location, method);
        Prayer[] dayPrayers = itl.getPrayerTimes(day).getPrayers();
        Prayer[] allTimes = new Prayer[]{dayPrayers[0], dayPrayers[1], dayPrayers[2], dayPrayers[3], dayPrayers[4], dayPrayers[5], itl.getNextDayFajr(day)};

        GregorianCalendar[] schedule = new GregorianCalendar[7];
        boolean[] extremes = new boolean[7];
        for (short i = CONSTANT.FAJR; i <= CONSTANT.NEXT_FAJR; i++) {
            schedule[i] = new GregorianCalendar(day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH), allTimes[i].getHour(), allTimes[i].getMinute(), allTimes[i].getSecond());
            schedule[i].add(Calendar.MINUTE, offsetMinutes);
            extremes[i] = allTimes[i].isExtreme();
        }
        schedule[CONSTANT.NEXT_FAJR].add(Calendar.DAY_OF_MONTH, 1); // Next fajr is tomorrow

        fi.joensuu.joyds1.calendar.IslamicCalendar hijriDate = new fi.joensuu.joyds1.calendar.IslamicCalendar();

        return new ScheduleData(schedule, extremes, hijriDate, getNextTimeIndex(schedule));
    }

    public static String getFormattedTime(GregorianCalendar[] schedule, boolean[] extremes, short i, String timeFormatIndex) {
        boolean isAMPM = Integer.parseInt(timeFormatIndex) == CONSTANT.DEFAULT_TIME_FORMAT;
        if (schedule[i] == null) {
            return "";
        }
        Date time = schedule[i].getTime();
        if (time == null) {
            return "";
        }
        String formattedTime = DateFormat.format(isAMPM ? "hh:mm a" : "HH:mm", time).toString();
        if (isAMPM && (formattedTime.startsWith("0") || formattedTime.startsWith("٠"))) {
            formattedTime = " " + formattedTime.substring(1);
        }
        if (extremes[i]) {
            formattedTime += " *";
        }
        return formattedTime;
    }

    public static short getNextTimeIndex(GregorianCalendar[] schedule) {
        Calendar now = new GregorianCalendar();
        if (now.before(schedule[CONSTANT.FAJR])) return CONSTANT.FAJR;
        for (short i = CONSTANT.FAJR; i < CONSTANT.NEXT_FAJR; i++) {
            if (now.after(schedule[i]) && now.before(schedule[i + 1])) {
                return ++i;
            }
        }
        return CONSTANT.NEXT_FAJR;
    }

    public static String getHijriDateString(fi.joensuu.joyds1.calendar.Calendar hijriDate, GregorianCalendar[] schedule, String[] hijriMonths, String anooHegirae) {
        boolean addedDay = false;
        if (isAfterSunset(schedule)) {
            addedDay = true;
            hijriDate.addDays(1);
        }
        String day = String.valueOf(hijriDate.getDay());
        String month = hijriMonths[hijriDate.getMonth() - 1];
        String year = String.valueOf(hijriDate.getYear());
        if (addedDay) {
            hijriDate.addDays(-1); // Revert to the day independent of sunset
        }
        return day + " " + month + ", " + year + " " + anooHegirae;
    }

    private static boolean isAfterSunset(GregorianCalendar[] schedule) {
        Calendar now = new GregorianCalendar();
        return now.after(schedule[CONSTANT.MAGHRIB]);
    }

    public static Location getLocation(String latitude, String longitude, String altitude, String pressure, String temperature) {
        Location location = new Location(
                Float.parseFloat(latitude),
                Float.parseFloat(longitude),
                getGMTOffset(),
                0
        );
        location.setSeaLevel(Float.parseFloat(altitude) < 0 ? 0 : Float.parseFloat(altitude));
        location.setPressure(Float.parseFloat(pressure));
        location.setTemperature(Float.parseFloat(temperature));
        return location;
    }

    private static double getGMTOffset() {
        Calendar now = new GregorianCalendar();
        int gmtOffset = now.getTimeZone().getOffset(now.getTimeInMillis());
        return gmtOffset / 3600000.0;
    }
}