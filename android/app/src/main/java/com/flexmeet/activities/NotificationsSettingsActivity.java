package com.flexmeet.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.flexmeet.databinding.ActivityNotificationsSettingsBinding;

public class NotificationsSettingsActivity extends AppCompatActivity {
    private ActivityNotificationsSettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationsSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.backBtn.setOnClickListener(v -> finish());
        
        // Load saved preferences here from SharedPreferences if needed
    }
}
