package com.flexmeet.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import timber.log.Timber;

/**
 * PermissionManager
 * Handles all runtime permissions for the application
 */
public class PermissionManager {

    public static final int CAMERA_PERMISSION_CODE = 101;
    public static final int MICROPHONE_PERMISSION_CODE = 102;
    public static final int LOCATION_PERMISSION_CODE = 103;
    public static final int STORAGE_PERMISSION_CODE = 104;
    public static final int ALL_PERMISSIONS_CODE = 105;

    /**
     * Check if camera permission is granted
     */
    public static boolean hasCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request camera permission
     */
    public static void requestCameraPermission(Activity activity) {
        if (!hasCameraPermission(activity)) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);
        }
    }

    /**
     * Check if microphone permission is granted
     */
    public static boolean hasMicrophonePermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request microphone permission
     */
    public static void requestMicrophonePermission(Activity activity) {
        if (!hasMicrophonePermission(activity)) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MICROPHONE_PERMISSION_CODE);
        }
    }

    /**
     * Check if location permission is granted
     */
    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request location permission
     */
    public static void requestLocationPermission(Activity activity) {
        if (!hasLocationPermission(activity)) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_CODE);
        }
    }

    /**
     * Check if storage permission is granted
     */
    public static boolean hasStoragePermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request storage permission
     */
    public static void requestStoragePermission(Activity activity) {
        if (!hasStoragePermission(activity)) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        }
    }

    /**
     * Check if all video call permissions are granted
     */
    public static boolean hasVideoCallPermissions(Context context) {
        return hasCameraPermission(context) && hasMicrophonePermission(context);
    }

    /**
     * Request all video call permissions
     */
    public static void requestVideoCallPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.MODIFY_AUDIO_SETTINGS
                },
                ALL_PERMISSIONS_CODE);
    }

    /**
     * Request all necessary permissions for the app
     */
    public static void requestAllPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.MODIFY_AUDIO_SETTINGS,
                        Manifest.permission.POST_NOTIFICATIONS
                },
                ALL_PERMISSIONS_CODE);
    }

    /**
     * Check if a specific permission is granted
     */
    public static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Handle permission result
     */
    public static boolean handlePermissionResult(int requestCode, int[] grantResults) {
        if (grantResults.length == 0) {
            return false;
        }

        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Log permission status
     */
    public static void logPermissionStatus(Context context) {
        Timber.d("Camera: " + hasCameraPermission(context));
        Timber.d("Microphone: " + hasMicrophonePermission(context));
        Timber.d("Location: " + hasLocationPermission(context));
        Timber.d("Storage: " + hasStoragePermission(context));
    }
}
