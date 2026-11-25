package com.flexmeet;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;
import timber.log.Timber;

/**
 * FlexMeetApplication - Main Application Class
 * Initializes Firebase, Timber logging, and other global configurations
 */
public class FlexMeetApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase
        initializeFirebase();

        // Initialize Timber for logging
        initializeLogging();

        // Create Notification Channels
        createNotificationChannels();
    }

    /**
     * Initialize Firebase services
     */
    private void initializeFirebase() {
        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this);

            // Enable Firebase persistence
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);

            Timber.d("Firebase initialized successfully");
        } catch (Exception e) {
            Timber.e(e, "Error initializing Firebase");
        }
    }

    /**
     * Initialize Timber logging
     */
    private void initializeLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    /**
     * Create NotificationChannels for Android O and above
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);

            if (notificationManager != null) {
                // Call notification channel
                NotificationChannel callChannel = new NotificationChannel(
                        "call_notification",
                        "Call Notifications",
                        NotificationManager.IMPORTANCE_HIGH
                );
                callChannel.setDescription("Notifications for incoming calls");
                notificationManager.createNotificationChannel(callChannel);

                // Message notification channel
                NotificationChannel messageChannel = new NotificationChannel(
                        "message_notification",
                        "Message Notifications",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                messageChannel.setDescription("Notifications for new messages");
                notificationManager.createNotificationChannel(messageChannel);

                // Meeting reminder channel
                NotificationChannel reminderChannel = new NotificationChannel(
                        "meeting_reminder",
                        "Meeting Reminders",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                reminderChannel.setDescription("Notifications for scheduled meetings");
                notificationManager.createNotificationChannel(reminderChannel);

                // Location channel
                NotificationChannel locationChannel = new NotificationChannel(
                        "location_channel",
                        "Location Services",
                        NotificationManager.IMPORTANCE_LOW
                );
                locationChannel.setDescription("Notification for location sharing");
                notificationManager.createNotificationChannel(locationChannel);
            }
        }
    }
}
