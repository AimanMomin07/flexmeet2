package com.flexmeet.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.flexmeet.models.Meeting;
import com.flexmeet.services.AlarmReceiver;
import java.util.Calendar;

public class NotificationHelper {

    public static void scheduleMeetingReminder(Context context, Meeting meeting) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("meeting_id", meeting.getMeetingId());
        intent.putExtra("meeting_title", meeting.getTitle());

        // Use meetingId hashcode as a unique request code
        int requestCode = meeting.getMeetingId().hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode,
                intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Set alarm 30 minutes before the meeting
        long triggerAtMillis = meeting.getScheduledTime() - (30 * 60 * 1000);

        // If time has already passed, don't set alarm
        if (triggerAtMillis < System.currentTimeMillis()) return;

        try {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } catch (SecurityException e) {
            // Handle cases where exact alarm permission is not granted
            e.printStackTrace();
        }
    }

    public static void cancelMeetingReminder(Context context, String meetingId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        int requestCode = meetingId.hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode,
                intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}
