package com.flexmeet.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.flexmeet.R;
import com.flexmeet.databinding.ActivityProfileBinding;
import com.flexmeet.firebase.FirebaseManager;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private final ActivityResultLauncher<String> getContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadProfileImage(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(247, 248, 252));
        getWindow().setNavigationBarColor(Color.rgb(247, 248, 252));
        getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        configureSetting(binding.settingsNotifications.getRoot(), "Notifications", "Meeting and message alerts", android.R.drawable.ic_popup_reminder, Color.rgb(139, 225, 255));
        configureSetting(binding.settingsPrivacy.getRoot(), "Privacy & security", "Control your account security", android.R.drawable.ic_lock_lock, Color.rgb(174, 156, 255));
        configureSetting(binding.settingsAbout.getRoot(), "About FlexMeet", "Version, support and information", android.R.drawable.ic_menu_info_details, Color.rgb(139, 225, 255));
        configureSetting(binding.logoutBtn.getRoot(), "Sign out", "Sign out of this account", android.R.drawable.ic_lock_power_off, Color.rgb(255, 143, 164));

        loadUserData();

        binding.backBtn.setOnClickListener(v -> finish());

        binding.changePhotoBtn.setOnClickListener(v -> getContent.launch("image/*"));

        binding.editProfileBtn.setOnClickListener(v -> showEditNameDialog());

        binding.settingsNotifications.getRoot().setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, NotificationsSettingsActivity.class);
            startActivity(intent);
        });

        binding.settingsPrivacy.getRoot().setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, SecuritySettingsActivity.class);
            startActivity(intent);
        });

        binding.logoutBtn.getRoot().setOnClickListener(v -> {
            FirebaseManager.getInstance().logoutUser();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        binding.settingsAbout.getRoot().setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, AboutAppActivity.class)));
    }

    private void configureSetting(View row, String title, String subtitle, int icon, int tint) {
        android.widget.TextView titleView = row.findViewById(R.id.setting_title);
        android.widget.TextView subtitleView = row.findViewById(R.id.setting_subtitle);
        android.widget.ImageView iconView = row.findViewById(R.id.setting_icon);
        titleView.setText(title);
        subtitleView.setText(subtitle);
        iconView.setImageResource(icon);
        iconView.setColorFilter(tint);
        if (title.equals("Sign out")) titleView.setTextColor(tint);
    }

    private void loadUserData() {
        FirebaseUser user = FirebaseManager.getInstance().getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            String name = user.getDisplayName();
            Uri photoUrl = user.getPhotoUrl();

            binding.userEmailText.setText(email);
            binding.userNameText.setText(name != null && !name.isEmpty() ? name : email.split("@")[0]);

            if (photoUrl != null) {
                Glide.with(this)
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_person_profile)
                        .error(R.drawable.ic_person_profile)
                        .into(binding.profileImage);
            }
        }
    }

    private void showEditNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Name");

        final EditText input = new EditText(this);
        input.setPadding(50, 20, 50, 20);
        String currentName = binding.userNameText.getText().toString();
        input.setText(currentName);
        input.setSelection(currentName.length());
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                updateProfileName(newName);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateProfileName(String newName) {
        binding.progressBar.setVisibility(View.VISIBLE);
        FirebaseManager.getInstance().updateUserProfile(newName, null)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = FirebaseManager.getInstance().getCurrentUserId();
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("displayName", newName);
                        FirebaseFirestore.getInstance().collection("users").document(uid)
                                .update(updates)
                                .addOnCompleteListener(firestoreTask -> {
                                    binding.progressBar.setVisibility(View.GONE);
                                    if (firestoreTask.isSuccessful()) {
                                        binding.userNameText.setText(newName);
                                        Toast.makeText(ProfileActivity.this, "Name updated", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(ProfileActivity.this, "Update failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadProfileImage(Uri imageUri) {
        binding.progressBar.setVisibility(View.VISIBLE);
        String uid = FirebaseManager.getInstance().getCurrentUserId();
        if (uid == null) return;

        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference photoRef = storageRef.child("profile_images/" + uid + ".jpg");

        UploadTask uploadTask = photoRef.putFile(imageUri);

        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return photoRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                updateProfilePhoto(downloadUri);
            } else {
                binding.progressBar.setVisibility(View.GONE);
                String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                Toast.makeText(ProfileActivity.this, "Upload failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateProfilePhoto(Uri photoUri) {
        FirebaseManager.getInstance().updateUserProfile(null, photoUri)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = FirebaseManager.getInstance().getCurrentUserId();
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("photoUrl", photoUri.toString());
                        FirebaseFirestore.getInstance().collection("users").document(uid)
                                .update(updates)
                                .addOnCompleteListener(firestoreTask -> {
                                    binding.progressBar.setVisibility(View.GONE);
                                    if (firestoreTask.isSuccessful()) {
                                        Glide.with(this)
                                                .load(photoUri)
                                                .placeholder(R.drawable.ic_person_profile)
                                                .into(binding.profileImage);
                                        Toast.makeText(ProfileActivity.this, "Photo updated", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(ProfileActivity.this, "Update failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
