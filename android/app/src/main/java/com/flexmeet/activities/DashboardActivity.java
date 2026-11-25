package com.flexmeet.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.flexmeet.R;
import com.flexmeet.databinding.ActivityDashboardBinding;
import com.flexmeet.fragments.ChatListFragment;
import com.flexmeet.fragments.ContactsFragment;
import com.flexmeet.fragments.ExploreFragment;
import com.flexmeet.fragments.HomeFragment;
import com.flexmeet.fragments.MeetingsFragment;
import com.flexmeet.firebase.FirebaseManager;
import com.google.firebase.database.ServerValue;
import com.google.firebase.messaging.FirebaseMessaging;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardActivity extends AppCompatActivity {
    private ActivityDashboardBinding binding;
    private final ExecutorService network = Executors.newSingleThreadExecutor();
    private int selectedNavId = R.id.nav_home;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(247, 248, 252));
        getWindow().setNavigationBarColor(Color.rgb(247, 248, 252));
        getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> { if (task.isSuccessful()) android.util.Log.d("FCM_TOKEN", task.getResult()); });
        if (state != null) selectedNavId = state.getInt("selected_nav_id", R.id.nav_home);
        setupBottomNavigation();
        if (state == null) loadFragment(new HomeFragment());
        binding.profileBtn.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        handleDeepLink();
    }

    private void setupBottomNavigation() {
        binding.navHome.setOnClickListener(v -> navigateTo(R.id.nav_home));
        binding.navHistory.setOnClickListener(v -> navigateTo(R.id.nav_history));
        binding.navChat.setOnClickListener(v -> navigateTo(R.id.nav_chat));
        binding.navContacts.setOnClickListener(v -> navigateTo(R.id.nav_contacts));
        binding.navExplore.setOnClickListener(v -> navigateTo(R.id.nav_explore));
        renderNavigationSelection();
    }

    private void navigateTo(int id) {
        if (id == selectedNavId) return;
        Fragment fragment;
        if (id == R.id.nav_home) fragment = new HomeFragment();
        else if (id == R.id.nav_history) fragment = new MeetingsFragment();
        else if (id == R.id.nav_chat) fragment = new ChatListFragment();
        else if (id == R.id.nav_contacts) fragment = new ContactsFragment();
        else if (id == R.id.nav_explore) fragment = new ExploreFragment();
        else return;
        selectedNavId = id;
        renderNavigationSelection();
        loadFragment(fragment);
    }

    private void renderNavigationSelection() {
        styleNavigationItem(binding.navHome, binding.navHomeIcon, binding.navHomeLabel, R.id.nav_home);
        styleNavigationItem(binding.navHistory, binding.navHistoryIcon, binding.navHistoryLabel, R.id.nav_history);
        styleNavigationItem(binding.navChat, binding.navChatIcon, binding.navChatLabel, R.id.nav_chat);
        styleNavigationItem(binding.navContacts, binding.navContactsIcon, binding.navContactsLabel, R.id.nav_contacts);
        styleNavigationItem(binding.navExplore, binding.navExploreIcon, binding.navExploreLabel, R.id.nav_explore);
    }

    private void styleNavigationItem(View item, ImageView icon, TextView label, int itemId) {
        boolean selected = selectedNavId == itemId;
        item.setSelected(selected);
        item.animate().cancel();
        item.setScaleX(selected ? 0.92f : 1f);
        item.setScaleY(selected ? 0.92f : 1f);
        item.animate().scaleX(1f).scaleY(1f).alpha(selected ? 1f : 0.74f).setDuration(190).start();
        icon.setColorFilter(selected ? Color.rgb(83, 104, 216) : Color.rgb(120, 134, 154));
        label.setTextColor(selected ? Color.rgb(83, 104, 216) : Color.rgb(120, 134, 154));
        label.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.nav_host_fragment, fragment)
                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                        android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .commit();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("selected_nav_id", selectedNavId);
        super.onSaveInstanceState(outState);
    }

    public void startNewMeeting() {
        if (FirebaseManager.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Sign in to host a meeting.", Toast.LENGTH_LONG).show();
            return;
        }
        ensureApiConfigured(this::showCreateMeetingDialog);
    }

    private void ensureApiConfigured(Runnable ready) {
        String configured = getSharedPreferences(MeetingsApi.PREFS, MODE_PRIVATE).getString(MeetingsApi.API_BASE, "");
        if (configured != null && !configured.trim().isEmpty()) { ready.run(); return; }
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint("https://your-api-host.example/api");
        new AlertDialog.Builder(this).setTitle("Connect FlexMeet")
                .setMessage("Enter the public URL of your FlexMeet Spring API. It must be deployed with LiveKit server credentials.")
                .setView(input).setPositiveButton("Save", (d, w) -> {
                    String value = input.getText().toString().trim().replaceAll("/+$", "");
                    if (value.isEmpty()) { Toast.makeText(this, "API URL is required", Toast.LENGTH_SHORT).show(); return; }
                    if (!value.endsWith("/api")) value += "/api";
                    getSharedPreferences(MeetingsApi.PREFS, MODE_PRIVATE).edit().putString(MeetingsApi.API_BASE, value).apply();
                    ready.run();
                }).setNegativeButton("Cancel", null).show();
    }

    private void showCreateMeetingDialog() {
        EditText title = new EditText(this);
        title.setSingleLine(true);
        title.setHint("Weekly team sync");
        CheckBox publicRoom = new CheckBox(this);
        publicRoom.setText("Show this room in Live now");
        LinearLayout panel = new LinearLayout(this);
        panel.setPadding(24, 8, 24, 0);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.addView(title);
        panel.addView(publicRoom);
        android.widget.RadioGroup modes = new android.widget.RadioGroup(this);
        android.widget.RadioButton open = new android.widget.RadioButton(this);
        open.setText("Everyone can use mic and camera");
        open.setId(View.generateViewId());
        android.widget.RadioButton hostOnly = new android.widget.RadioButton(this);
        hostOnly.setText("Only the host can publish");
        hostOnly.setId(View.generateViewId());
        modes.addView(open);
        modes.addView(hostOnly);
        open.setChecked(true);
        panel.addView(modes);
        new AlertDialog.Builder(this).setTitle("Create meeting").setView(panel)
                .setPositiveButton("Create & start", (d, w) -> createMeeting(title.getText().toString().trim(),
                        open.isChecked() ? "OPEN" : "HOST_ONLY", publicRoom.isChecked()))
                .setNegativeButton("Cancel", null).show();
    }

    private void createMeeting(String title, String mode, boolean publicRoom) {
        Toast.makeText(this, "Creating secure meeting…", Toast.LENGTH_SHORT).show();
        network.execute(() -> {
            try {
                JSONObject body = new JSONObject().put("title", title).put("mode", mode);
                JSONObject result = MeetingsApi.post(this, "/rooms", body, true);
                String room = result.getString("room");
                String roomTitle = result.optString("title", title.isEmpty() ? "FlexMeet room" : title);
                if (publicRoom) publishPublicRoom(room, roomTitle);
                runOnUiThread(() -> launchCall(room, roomTitle, true, true, true));
            } catch (Exception error) {
                runOnUiThread(() -> Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void publishPublicRoom(String room, String title) {
        String uid = FirebaseManager.getInstance().getCurrentUserId();
        String name = FirebaseManager.getInstance().getCurrentRealtimeUser().getDisplayName();
        Map<String, Object> event = new HashMap<>();
        event.put("room", room);
        event.put("title", title);
        event.put("hostName", name);
        event.put("status", "live");
        event.put("createdAt", ServerValue.TIMESTAMP);
        event.put("hostUid", uid);
        FirebaseManager.getInstance().getFirebaseDatabase().getReference("public_rooms").child(room).setValue(event);
    }

    public void showJoinDialog() {
        EditText input = new EditText(this);
        input.setHint("Paste meeting invite or room code");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        new AlertDialog.Builder(this).setTitle("Join meeting").setMessage("FlexMeet links open in the app when installed.")
                .setView(input).setPositiveButton("Join", (dialog, which) -> {
                    String id = normalizeMeetingId(input.getText().toString());
                    if (id.isEmpty()) Toast.makeText(this, "Enter a valid invite link or room code", Toast.LENGTH_SHORT).show();
                    else joinMeeting(id);
                }).setNegativeButton("Cancel", null).show();
    }

    public void openMeeting(String id) { joinMeeting(id); }

    private void joinMeeting(String id) {
        ensureApiConfigured(() -> launchCall(id, "FlexMeet meeting", false, true, true));
    }

    private void launchCall(String id, String title, boolean host, boolean audio, boolean video) {
        Intent intent = new Intent(this, LiveKitCallActivity.class);
        intent.putExtra(LiveKitCallActivity.EXTRA_ROOM, id);
        intent.putExtra(LiveKitCallActivity.EXTRA_TITLE, title);
        intent.putExtra(LiveKitCallActivity.EXTRA_HOST, host);
        startActivity(intent);
    }

    private String normalizeMeetingId(String value) {
        if (value == null) return "";
        String input = value.trim();
        if (input.isEmpty()) return "";
        if (input.startsWith("http://") || input.startsWith("https://") || input.startsWith("flexmeet://")) {
            Uri uri = Uri.parse(input);
            String path = uri.getPath();
            if (path == null) return "";
            String[] segments = path.split("/");
            return segments.length == 0 ? "" : segments[segments.length - 1].trim();
        }
        return input.replaceAll("[^A-Za-z0-9_-]", "");
    }

    public void scheduleMeeting() { startActivity(new Intent(this, ScheduleMeetingActivity.class)); }

    private void handleDeepLink() {
        Uri data = getIntent().getData();
        if (data == null || data.getPath() == null || !data.getPath().startsWith("/join/")) return;
        String id = normalizeMeetingId(data.toString());
        if (!id.isEmpty()) joinMeeting(id);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink();
    }

    @Override public void onRequestPermissionsResult(int code, @NonNull String[] permissions, @NonNull int[] grants) {
        super.onRequestPermissionsResult(code, permissions, grants);
    }

    @Override protected void onDestroy() {
        network.shutdown();
        super.onDestroy();
    }
}