package com.flexmeet.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.flexmeet.databinding.ActivityLocationSharingBinding;

public class LocationSharingActivity extends AppCompatActivity {

    private ActivityLocationSharingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLocationSharingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}
