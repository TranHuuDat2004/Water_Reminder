package com.example.canvas;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

  private FirebaseAuth mAuth;

  private ImageView btnClose;
  private TextView tvTitle, tvSubtitle, tvForgotPassword, tvSignUp;
  private EditText etEmail, etPassword;
  private ImageView btnTogglePassword;
  private CheckBox cbRememberMe;
  private Button btnLogin, btnGoogleLogin, btnFacebookLogin;

  @Override
  public void onStart() {
    super.onStart();
    FirebaseUser currentUser = mAuth.getCurrentUser();
    if (currentUser != null) {
      // User is already signed in
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.login);

    mAuth = FirebaseAuth.getInstance();

    btnClose = findViewById(R.id.btnClose);
    tvTitle = findViewById(R.id.tvTitle);
    tvSubtitle = findViewById(R.id.tvSubtitle);
    tvForgotPassword = findViewById(R.id.tvForgotPassword);
    tvSignUp = findViewById(R.id.tvSignUp);
    etEmail = findViewById(R.id.etEmail);
    etPassword = findViewById(R.id.etPassword);
    btnTogglePassword = findViewById(R.id.btnTogglePassword);
    cbRememberMe = findViewById(R.id.cbRememberMe);
    btnLogin = findViewById(R.id.btnLogin);
    btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
    btnFacebookLogin = findViewById(R.id.btnFacebookLogin);

    btnLogin.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        loginUser();
      }
    });

    tvSignUp.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        // Start SignupActivity
        Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
        startActivity(intent);
      }
    });
  }

  private void loginUser() {
    String email = etEmail.getText().toString().trim();
    String password = etPassword.getText().toString().trim();

    if (email.isEmpty() || password.isEmpty()) {
      Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
      return;
    }

    mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
              @Override
              public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                  // Login successful
                  FirebaseUser user = mAuth.getCurrentUser();
                  String userId = user.getUid(); // Get the user ID here
                  Toast.makeText(LoginActivity.this, "Login successful. User ID: " + userId,
                          Toast.LENGTH_SHORT).show();

                  // Navigate to Started1Activity after successful login
                  Intent intent = new Intent(LoginActivity.this, Started1Activity.class);
                  intent.putExtra("userId", userId); // Pass the user ID to Started1Activity
                  startActivity(intent);
                  finish();

                } else {
                  // Login failed
                  Toast.makeText(LoginActivity.this, "Authentication failed.",
                          Toast.LENGTH_SHORT).show();
                }
              }
            });
  }

}