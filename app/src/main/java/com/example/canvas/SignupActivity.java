package com.example.canvas; // Make sure this package name is correct

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
// Import ProgressBar if you intend to use it
// import android.widget.ProgressBar;

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
    private TextView tvTitle, tvSubtitle, tvLoginLink; // Changed name from tvSignUp to tvLoginLink for clarity
    private EditText etUsername, etEmail, etPhone, etPassword;
    private ImageView btnTogglePassword;
    private Button btnSignup; // Renamed from btnLogin
    // Commented out unused social login buttons
    // private Button btnGoogleLogin, btnFacebookLogin;
    // Commented out unused ProgressBar
    // private ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure the layout name here matches your XML file name
        setContentView(R.layout.signup); // Assuming your XML file is named signup.xml

        mAuth = FirebaseAuth.getInstance();

        // Initialize views - IDs now match the XML
        btnClose = findViewById(R.id.btnClose);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvLoginLink = findViewById(R.id.tvLogIn); // CORRECTED ID to match XML
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnSignup = findViewById(R.id.btnSignUp); // CORRECTED ID to match XML
        // progressBar = findViewById(R.id.progressBar); // Uncomment if you use the ProgressBar

        // Social login buttons are commented out in XML, so comment out initialization here too
        // btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        // btnFacebookLogin = findViewById(R.id.btnFacebookLogin);


        // Set onClickListener for signup button
        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        // Set onClickListener for Login TextView ("Log In")
        tvLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to LoginActivity
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class); // Assuming LoginActivity exists
                startActivity(intent);
                finish(); // Optional: Close SignupActivity
            }
        });

        // --- Add listeners for btnClose, btnTogglePassword etc. if needed ---
        btnClose.setOnClickListener(v -> finish()); // Example: Close activity on btnClose click

        // Example: Toggle Password Visibility (You'll need logic for this)
        btnTogglePassword.setOnClickListener(v -> {
            // Add logic to toggle password input type between textPassword and visible text
            Toast.makeText(SignupActivity.this, "Toggle Password Clicked", Toast.LENGTH_SHORT).show();
        });

        // --- Comment out listeners for social buttons if they are commented out ---
        /*
        btnGoogleLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle Google Login
            }
        });

        btnFacebookLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle Facebook Login
            }
        });
        */
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

        // --- Optional: Show ProgressBar ---
        // if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        // if (btnSignup != null) btnSignup.setEnabled(false); // Disable button during registration

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // --- Optional: Hide ProgressBar ---
                        // if (progressBar != null) progressBar.setVisibility(View.GONE);
                        // if (btnSignup != null) btnSignup.setEnabled(true); // Re-enable button

                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String userId = user.getUid(); // Get userId from Firebase Auth

                                // Save info to Firestore
                                saveUserToFirestore(userId, username, email, phone);
                            } else {
                                // Should not happen if task is successful, but good practice to handle
                                Toast.makeText(SignupActivity.this, "Registration succeeded but failed to get user.", Toast.LENGTH_LONG).show();
                                Log.e("SignupActivity", "Registration succeeded but user is null");
                            }
                        } else {
                            Toast.makeText(SignupActivity.this, "Registration failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show(); // Use LONG duration for errors
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
        userData.put("phoneNumber", phone); // Consistent key name
        userData.put("registrationDate", Timestamp.now()); // Registration date
        // Default values for other fields
        userData.put("walk", 0);
        userData.put("calories", 0);
        userData.put("sleep", 0);
        userData.put("heart", 0);

        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SignupActivity.this, "User registered successfully!", Toast.LENGTH_SHORT).show();
                    // Redirect to LoginActivity after successful registration AND Firestore save
                    startActivity(new Intent(SignupActivity.this, LoginActivity.class)); // Assuming LoginActivity exists
                    finish(); // Close SignupActivity
                })
                .addOnFailureListener(e -> {
                    // Inform user saving failed, but registration might have succeeded.
                    // Consider how to handle this - maybe allow retry or manual login.
                    Toast.makeText(SignupActivity.this, "Registration succeeded, but failed to save user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("Firestore", "Error saving user data", e);
                    // Don't automatically redirect if saving fails, let user try logging in later.
                    // startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                    // finish();
                });
    }
}