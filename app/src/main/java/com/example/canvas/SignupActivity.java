package com.example.canvas;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private ImageView btnClose;
    private TextView tvTitle, tvSubtitle, tvLoginLink;
    private EditText etUsername, etEmail, etPhone, etPassword;
    private ImageView btnTogglePassword;
    private Button btnSignup, btnGoogleLogin, btnFacebookLogin;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);

        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        btnClose = findViewById(R.id.btnClose);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvLoginLink = findViewById(R.id.tvSignUp); // Corrected ID
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnSignup = findViewById(R.id.btnLogin); // Corrected ID (signup button)
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        btnFacebookLogin = findViewById(R.id.btnFacebookLogin);


        // Set onClickListener for signup button
        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        // Set onClickListener for Login TextView
        tvLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to LoginActivity
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Optional: Close SignupActivity
            }
        });
    }

    private void registerUser() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String userId = user.getUid(); // Lấy userId từ Firebase Auth

                                // Lưu thông tin lên Firestore
                                saveUserToFirestore(userId, username, email, phone);
                            }
                        } else {
                            Toast.makeText(SignupActivity.this, "Registration failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            Log.e("SignupActivity", "Registration failed", task.getException());
                        }
                    }
                });
    }

    private void saveUserToFirestore(String userId, String username, String email, String phone) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("email", email);
        userData.put("phoneNumber", phone);
        userData.put("registrationDate", Timestamp.now()); // Ngày đăng ký là hôm nay
        userData.put("walk", 0);
        userData.put("calories", 0);
        userData.put("sleep", 0);
        userData.put("heart", 0);

        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SignupActivity.this, "User registered successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SignupActivity.this, "Failed to save user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error saving user data", e);
                });
    }
}