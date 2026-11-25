package com.flexmeet.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.flexmeet.databinding.ActivityRegisterBinding;
import com.flexmeet.firebase.FirebaseManager;
import com.flexmeet.models.User;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.registerButton.setOnClickListener(v -> registerUser());
        binding.signInText.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String name = binding.nameInput.getText().toString().trim();
        String email = binding.emailInput.getText().toString().trim();
        String phone = binding.phoneInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (phone.length() < 10) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseManager.getInstance().registerUser(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String uid = task.getResult().getUser().getUid();
                    User user = new User(uid, email.trim().toLowerCase(java.util.Locale.ROOT), name);
                    user.setPhoneNumber("+91" + phone);
                    FirebaseManager.getInstance().updateUserProfile(name, null)
                        .addOnCompleteListener(profileTask -> FirebaseManager.getInstance().saveUser(uid, user)
                            .continueWithTask(saveUserTask -> {
                                if (!saveUserTask.isSuccessful()) throw saveUserTask.getException();
                                return FirebaseManager.getInstance().saveUserRealtime(uid, user);
                            })
                            .addOnCompleteListener(saveTask -> {
                                if (saveTask.isSuccessful()) {
                                    startActivity(new Intent(RegisterActivity.this, DashboardActivity.class));
                                    finishAffinity();
                                } else {
                                    Toast.makeText(RegisterActivity.this, "Profile setup failed: " + saveTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }));
                } else {
                    Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }
}
