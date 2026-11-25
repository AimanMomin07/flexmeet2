package com.flexmeet.activities;

import android.content.Context;
import com.flexmeet.firebase.FirebaseManager;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.concurrent.TimeUnit;

public final class MeetingsApi {
    public static final String PREFS = "flexmeet_settings";
    public static final String API_BASE = "api_base";
    private static final OkHttpClient HTTP = new OkHttpClient.Builder().callTimeout(25, TimeUnit.SECONDS).build();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private MeetingsApi() {}

    public static JSONObject post(Context context, String path, JSONObject body, boolean authenticated) throws Exception {
        String base = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(API_BASE, "");
        if (base == null || base.trim().isEmpty()) throw new IllegalStateException("Set your FlexMeet API URL before creating or joining meetings.");
        base = base.trim().replaceAll("/+$", "");
        if (!base.endsWith("/api")) base += "/api";
        Request.Builder request = new Request.Builder().url(base + path)
                .post(RequestBody.create(body == null ? "{}" : body.toString(), JSON));
        if (authenticated) {
            FirebaseUser user = FirebaseManager.getInstance().getCurrentUser();
            if (user == null) throw new IllegalStateException("Sign in to host and manage meetings.");
            String token = Tasks.await(user.getIdToken(false)).getToken();
            request.header("Authorization", "Bearer " + token);
        }
        try (Response response = HTTP.newCall(request.build()).execute()) {
            String text = response.body() == null ? "" : response.body().string();
            JSONObject result = text.isEmpty() ? new JSONObject() : new JSONObject(text);
            if (!response.isSuccessful()) throw new IllegalStateException(result.optString("message", "Meeting service returned " + response.code()));
            return result;
        }
    }

    public static JSONArray getArray(Context context, String path, boolean authenticated) throws Exception {
        String base = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(API_BASE, "");
        if (base == null || base.trim().isEmpty()) throw new IllegalStateException("Set your FlexMeet API URL first.");
        base = base.trim().replaceAll("/+$", "");
        if (!base.endsWith("/api")) base += "/api";
        Request.Builder request = new Request.Builder().url(base + path).get();
        if (authenticated) {
            FirebaseUser user = FirebaseManager.getInstance().getCurrentUser();
            if (user == null) throw new IllegalStateException("Sign in to manage meetings.");
            request.header("Authorization", "Bearer " + Tasks.await(user.getIdToken(false)).getToken());
        }
        try (Response response = HTTP.newCall(request.build()).execute()) {
            String text = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) throw new IllegalStateException("Meeting service returned " + response.code());
            return text.isEmpty() ? new JSONArray() : new JSONArray(text);
        }
    }
}