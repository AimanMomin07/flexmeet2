package com.flexmeet.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.flexmeet.R;
import com.flexmeet.activities.VideoCallActivity;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String meetingId = intent.getStringExtra("meeting_id");
        String meetingTitle = intent.getStringExtra("meeting_title");

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "meeting_reminder_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Meeting Reminders", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Intent joinIntent = new Intent(context, VideoCallActivity.class);
        joinIntent.putExtra("meeting_id", meetingId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, meetingId.hashCode(), joinIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Meeting Reminder")
                .setContentText("Your meeting '" + meetingTitle + "' starts in 30 minutes.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(meetingId.hashCode(), builder.build());
    }
}
