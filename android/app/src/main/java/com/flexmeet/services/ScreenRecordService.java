package com.flexmeet.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.io.IOException;

public class ScreenRecordService extends Service {

    private static final String TAG = "ScreenRecordService";
    private static final String CHANNEL_ID = "ScreenRecordChannel";
    private static final int NOTIFICATION_ID = 1337;

    public static final String ACTION_START = "START_RECORDING";
    public static final String ACTION_STOP = "STOP_RECORDING";
    public static final String EXTRA_RESULT_CODE = "RESULT_CODE";
    public static final String EXTRA_DATA = "DATA";

    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaRecorder mediaRecorder;
    private Uri videoUri;
    private ParcelFileDescriptor pfd;

    @Override
    public void onCreate() {
        super.onCreate();
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1);
            Intent data = intent.getParcelableExtra(EXTRA_DATA);
            if (resultCode != -1 && data != null) {
                startForegroundWithNotification();
                startRecording(resultCode, data);
            }
        } else if (ACTION_STOP.equals(action)) {
            stopRecording();
            stopForeground(true);
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    private void startForegroundWithNotification() {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("FlexMeet Recording")
                .setContentText("Meeting is being recorded and will be saved to Gallery")
                .setSmallIcon(android.R.drawable.presence_video_busy)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void startRecording(int resultCode, Intent data) {
        mediaProjection = projectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) return;

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        int screenDensity = metrics.densityDpi;

        // Force standard 720p for maximum compatibility if screen is larger
        if (screenWidth > 1280) {
            float aspect = (float) screenHeight / screenWidth;
            screenWidth = 720;
            screenHeight = (int) (720 * aspect);
        }

        // Ensure even dimensions
        if (screenWidth % 2 != 0) screenWidth--;
        if (screenHeight % 2 != 0) screenHeight--;

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, "FlexMeet_" + System.currentTimeMillis() + ".mp4");
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/FlexMeet");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Video.Media.IS_PENDING, 1);
        }

        videoUri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

        try {
            if (videoUri != null) {
                pfd = getContentResolver().openFileDescriptor(videoUri, "rw");
                if (pfd != null) {
                    mediaRecorder.setOutputFile(pfd.getFileDescriptor());
                }
            }
            
            mediaRecorder.setVideoSize(screenWidth, screenHeight);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setVideoEncodingBitRate(3 * 1024 * 1024); // 3Mbps for stability
            mediaRecorder.setVideoFrameRate(30);
            
            mediaRecorder.prepare();

            virtualDisplay = mediaProjection.createVirtualDisplay("FlexMeetRec",
                    screenWidth, screenHeight, screenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mediaRecorder.getSurface(), null, null);
            
            mediaRecorder.start();
        } catch (Exception e) {
            Log.e(TAG, "Start recording failed", e);
        }
    }

    private void stopRecording() {
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
            }
        } catch (Exception e) {
            Log.e(TAG, "Stop recorder failed", e);
        }
        
        try {
            if (pfd != null) pfd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        if (virtualDisplay != null) virtualDisplay.release();
        if (mediaProjection != null) mediaProjection.stop();
        
        if (videoUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Video.Media.IS_PENDING, 0);
            getContentResolver().update(videoUri, values, null, null);
        }

        mediaRecorder = null;
        virtualDisplay = null;
        mediaProjection = null;
        pfd = null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Screen Recording", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
