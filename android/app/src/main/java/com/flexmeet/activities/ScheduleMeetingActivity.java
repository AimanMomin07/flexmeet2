package com.flexmeet.activities;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.flexmeet.databinding.ActivityScheduleMeetingBinding;
import com.flexmeet.firebase.FirebaseManager;
import com.flexmeet.models.Meeting;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class ScheduleMeetingActivity extends AppCompatActivity {

    private ActivityScheduleMeetingBinding binding;
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormatter = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScheduleMeetingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.backBtn.setOnClickListener(v -> finish());

        binding.datePickerLayout.setOnClickListener(v -> showMaterialDatePicker());
        binding.timePickerLayout.setOnClickListener(v -> showMaterialTimePicker());

        binding.scheduleBtn.setOnClickListener(v -> {
            String title = binding.meetingTitleInput.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (binding.selectedDateText.getText().equals("Not selected")) {
                Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = FirebaseManager.getInstance().getCurrentUserId();
            // Important: Use saveMeeting instead of createMeeting to ensure document ID matches meetingId
            Meeting meeting = new Meeting(UUID.randomUUID().toString(), title, userId, calendar.getTimeInMillis());
            
            FirebaseManager.getInstance().saveMeeting(meeting)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Meeting scheduled successfully", Toast.LENGTH_SHORT).show();
                        // This Toast was already implemented, ensured it works with the latest saveMeeting method.
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to schedule meeting", Toast.LENGTH_SHORT).show();
                    }
                });
        });
    }

    private void showMaterialDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Meeting Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            calendar.setTimeInMillis(selection);
            binding.selectedDateText.setText(dateFormatter.format(calendar.getTime()));
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void showMaterialTimePicker() {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .setTitleText("Select Meeting Time")
                .build();

        timePicker.addOnPositiveButtonClickListener(v -> {
            calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
            calendar.set(Calendar.MINUTE, timePicker.getMinute());
            binding.selectedTimeText.setText(timeFormatter.format(calendar.getTime()));
        });

        timePicker.show(getSupportFragmentManager(), "TIME_PICKER");
    }
}
