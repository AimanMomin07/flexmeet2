package com.flexmeet.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.flexmeet.databinding.ActivityLoginBinding;
import com.flexmeet.firebase.FirebaseManager;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(247, 248, 252));
        getWindow().setNavigationBarColor(Color.rgb(247, 248, 252));
        getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.loginButton.setOnClickListener(v -> loginUser());
        binding.signUpText.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
        
        binding.forgotPasswordText.setOnClickListener(v -> {
            String email = binding.emailInput.getText().toString().trim();
            if (!TextUtils.isEmpty(email)) {
                FirebaseManager.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Reset email sent to " + email, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
            } else {
                Toast.makeText(this, "Enter your email first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginUser() {
        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseManager.getInstance().loginUser(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    com.google.firebase.auth.FirebaseUser user = FirebaseManager.getInstance().getCurrentUser();
                    if (user != null) {
                        String name = user.getDisplayName();
                        if (name == null || name.trim().isEmpty()) name = email.split("@")[0];
                        FirebaseManager.getInstance().saveUserRealtime(user.getUid(), new com.flexmeet.models.User(user.getUid(), email, name));
                    }
                    startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }
}
