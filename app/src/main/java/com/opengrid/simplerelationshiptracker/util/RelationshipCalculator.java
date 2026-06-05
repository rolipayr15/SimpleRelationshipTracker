package com.opengrid.simplerelationshiptracker.util;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class RelationshipCalculator {

    public static class Duration {
        public long years, months, days, hours, minutes, seconds;

        @Override
        public String toString() {
            return String.format("%dy %dm %dd %02d:%02d:%02d", years, months, days, hours, minutes, seconds);
        }
    }

    public static Duration calculateDuration(long startDateMillis, long currentTimeMillis, String timeZoneId) {
        TimeZone tz = TimeZone.getTimeZone(timeZoneId);
        Calendar start = Calendar.getInstance(tz);
        start.setTimeInMillis(startDateMillis);

        Calendar end = Calendar.getInstance(tz);
        end.setTimeInMillis(currentTimeMillis);

        if (end.before(start)) {
            return new Duration();
        }

        Duration duration = new Duration();

        duration.years = end.get(Calendar.YEAR) - start.get(Calendar.YEAR);
        duration.months = end.get(Calendar.MONTH) - start.get(Calendar.MONTH);
        duration.days = end.get(Calendar.DAY_OF_MONTH) - start.get(Calendar.DAY_OF_MONTH);

        if (duration.days < 0) {
            duration.months--;
            Calendar temp = (Calendar) end.clone();
            temp.add(Calendar.MONTH, -1);
            duration.days = temp.getActualMaximum(Calendar.DAY_OF_MONTH) + duration.days;
        }

        if (duration.months < 0) {
            duration.years--;
            duration.months = 12 + duration.months;
        }

        long diffMillis = end.getTimeInMillis() - start.getTimeInMillis();
        long remainingMillis = diffMillis % (24 * 60 * 60 * 1000);

        duration.hours = TimeUnit.MILLISECONDS.toHours(remainingMillis);
        duration.minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60;
        duration.seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60;

        return duration;
    }
}