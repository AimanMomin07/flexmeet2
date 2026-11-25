package com.flexmeet.constants;

/**
 * AppConstants
 * Contains all application constants
 */
public class AppConstants {

    // Firebase Collections
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_MEETINGS = "meetings";
    public static final String COLLECTION_MESSAGES = "messages";
    public static final String COLLECTION_NOTES = "notes";
    public static final String COLLECTION_FILES = "files";
    public static final String COLLECTION_LOCATIONS = "locations";
    public static final String COLLECTION_PARTICIPANTS = "participants";

    // Shared Preferences Keys
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_USER_EMAIL = "user_email";
    public static final String PREF_USER_NAME = "user_name";
    public static final String PREF_USER_PHOTO_URL = "user_photo_url";
    public static final String PREF_IS_LOGGED_IN = "is_logged_in";
    public static final String PREF_FCM_TOKEN = "fcm_token";

    // Meeting Status
    public static final String STATUS_SCHEDULED = "scheduled";
    public static final String STATUS_ONGOING = "ongoing";
    public static final String STATUS_ENDED = "ended";
    public static final String STATUS_CANCELLED = "cancelled";

    // User Status
    public static final String USER_STATUS_ONLINE = "online";
    public static final String USER_STATUS_OFFLINE = "offline";
    public static final String USER_STATUS_BUSY = "busy";
    public static final String USER_STATUS_AWAY = "away";

    // Message Types
    public static final String MESSAGE_TYPE_TEXT = "text";
    public static final String MESSAGE_TYPE_IMAGE = "image";
    public static final String MESSAGE_TYPE_FILE = "file";
    public static final String MESSAGE_TYPE_LOCATION = "location";

    // File Types
    public static final String FILE_TYPE_PDF = "pdf";
    public static final String FILE_TYPE_IMAGE = "image";
    public static final String FILE_TYPE_DOCUMENT = "document";
    public static final String FILE_TYPE_VIDEO = "video";
    public static final String FILE_TYPE_AUDIO = "audio";
    public static final String FILE_TYPE_OTHER = "other";

    // Request Codes
    public static final int REQUEST_CODE_CAMERA = 101;
    public static final int REQUEST_CODE_PICK_IMAGE = 102;
    public static final int REQUEST_CODE_PICK_FILE = 103;
    public static final int REQUEST_CODE_LOCATION = 104;

    // WebRTC Configuration
    public static final String WEBRTC_SIGNALING_CHANNEL = "webrtc_signaling";
    public static final int MAX_VIDEO_WIDTH = 720;
    public static final int MAX_VIDEO_HEIGHT = 1280;
    public static final int VIDEO_FPS = 30;

    // Notification IDs
    public static final int NOTIFICATION_ID_CALL = 1001;
    public static final int NOTIFICATION_ID_MESSAGE = 1002;
    public static final int NOTIFICATION_ID_MEETING_REMINDER = 1003;
    public static final int NOTIFICATION_ID_LOCATION = 1004;

    // Time Constants
    public static final long LOCATION_UPDATE_INTERVAL = 10000; // 10 seconds
    public static final long FASTEST_LOCATION_UPDATE_INTERVAL = 5000; // 5 seconds
    public static final float LOCATION_MIN_ACCURACY = 50f; // meters

    // UI Constants
    public static final long ANIMATION_DURATION = 300; // milliseconds
    public static final long DOUBLE_CLICK_TIME = 500; // milliseconds

    // Empty States
    public static final String EMPTY_STRING = "";
    public static final String SPACE = " ";
    public static final String ZERO_VALUE = "0";

    // Validation
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_FILE_SIZE = 100 * 1024 * 1024; // 100 MB

    // Network
    public static final int CONNECTION_TIMEOUT = 30; // seconds
    public static final int READ_TIMEOUT = 30; // seconds
}
