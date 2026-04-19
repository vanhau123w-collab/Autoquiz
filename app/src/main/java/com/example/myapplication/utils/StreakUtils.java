package com.example.myapplication.utils;

import com.example.myapplication.data.QuizResult;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StreakUtils {

    /**
     * Calculates the current streak of consecutive days with at least one quiz completion.
     * A streak is maintained if a quiz was completed today or yesterday.
     */
    public static int calculateCurrentStreak(List<QuizResult> results) {
        if (results == null || results.isEmpty()) return 0;

        Set<String> completedDates = new HashSet<>();
        for (QuizResult result : results) {
            completedDates.add(getYearDayString(result.getTimestamp()));
        }

        Calendar cal = Calendar.getInstance();
        String today = getYearDayString(cal.getTimeInMillis());
        
        cal.add(Calendar.DAY_OF_YEAR, -1);
        String yesterday = getYearDayString(cal.getTimeInMillis());

        // Does the streak end today?
        boolean hasToday = completedDates.contains(today);
        boolean hasYesterday = completedDates.contains(yesterday);

        if (!hasToday && !hasYesterday) return 0;

        int streak = 0;
        cal = Calendar.getInstance(); 
        if (!hasToday) cal.add(Calendar.DAY_OF_YEAR, -1); // Start from yesterday if today is empty

        while (completedDates.contains(getYearDayString(cal.getTimeInMillis()))) {
            streak++;
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        return streak;
    }

    /**
     * Returns an array of 7 booleans indicating if a quiz was completed on each day
     * of the current week (Monday to Sunday).
     */
    public static boolean[] getWeeklyStatus(List<QuizResult> results) {
        boolean[] status = new boolean[7];
        if (results == null || results.isEmpty()) return status;

        Set<String> completedDates = new HashSet<>();
        for (QuizResult result : results) {
            completedDates.add(getYearDayString(result.getTimestamp()));
        }

        Calendar cal = Calendar.getInstance();
        // Set to Monday of the current week
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        for (int i = 0; i < 7; i++) {
            status[i] = completedDates.contains(getYearDayString(cal.getTimeInMillis()));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        return status;
    }

    private static String getYearDayString(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        return cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.DAY_OF_YEAR);
    }
}
