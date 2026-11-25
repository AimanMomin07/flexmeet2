package com.flexmeet.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.flexmeet.databinding.ActivityFileTransferBinding;

public class FileTransferActivity extends AppCompatActivity {

    private ActivityFileTransferBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFileTransferBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}
