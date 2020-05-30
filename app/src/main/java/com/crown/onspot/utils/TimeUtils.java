package com.crown.onspot.utils;

import android.annotation.SuppressLint;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {
    public static String getDay(long second) {
        Date date = new Date(second * 1000);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int year = calendar.get(Calendar.YEAR);
        @SuppressLint("SimpleDateFormat") String month = new SimpleDateFormat("MMM").format(calendar.getTime());

        return String.format("%s %s %s", day, month, year);
    }

    public static String getTime(long second) {
        Date date = new Date(second * 1000);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour >= 12) hour = hour % 12;
        int min = calendar.get(Calendar.MINUTE);

        String zone;
        if (calendar.get(Calendar.AM_PM) == Calendar.AM) {
            zone = "am";
        } else {
            zone = "pm";
        }
        return String.format("%s:%s %s", hour, min, zone);
    }

    public static String getDuration(long second) {
        return String.format(Locale.ENGLISH, "%dh %02dm %02ds", (second / 3600), (second % 3600) / 60, (second % 60));
    }

    public static String getOnDuration(long second) {
        Timestamp currentTime = new Timestamp(new Date());
        long duration = currentTime.getSeconds() - second;
        return getDuration(duration);
    }
}
