package com.example.canvas;

import com.example.canvas.models.User;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class MainActivity extends AppCompatActivity {

  private EditText etEmail, etPassword,etUsername,etPhone;
  private Button btnLogin, btnSignUp;
  private TextView tvSignUp, tvTitle;
  private FirebaseAuth mAuth;
  private ProgressBar progressBar;
  private String activityType; // "login" or "signup"
  private DatabaseReference mDatabase;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Xác định xem activity là đăng nhập hay đăng ký
    if (getIntent().hasExtra("activity_type")) {
      activityType = getIntent().getStringExtra("activity_type");
    } else {
      activityType = "login"; // Default to login
    }
    if (activityType.equals("signup")) {
      setContentView(R.layout.signup);
      etUsername = findViewById(R.id.etUsername);
      etPhone = findViewById(R.id.etPhone);
      btnLogin = findViewById(R.id.btnLogin);
      tvTitle = findViewById(R.id.tvTitle);
    } else {
      setContentView(R.layout.login);
      tvSignUp = findViewById(R.id.tvSignUp);
    }
    etEmail = findViewById(R.id.etEmail);
    etPassword = findViewById(R.id.etPassword);
    btnLogin = findViewById(R.id.btnLogin);
    mAuth = FirebaseAuth.getInstance();

    // Initialize Firebase Realtime Database
    mDatabase = FirebaseDatabase.getInstance().getReference();

    if (activityType.equals("login")) {
      tvSignUp.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          // Start the SignupActivity
          Intent intent = new Intent(MainActivity.this, MainActivity.class);
          intent.putExtra("activity_type", "signup");
          startActivity(intent);
          finish();
        }
      });
    }else{
      btnLogin.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          registerNewUser();
        }
      });
    }
    btnLogin.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (activityType.equals("login")) {
          loginUserAccount();
        } else {
          registerNewUser();
        }
      }
    });



  }

  private void registerNewUser() {
    progressBar.setVisibility(View.VISIBLE);

    String email, password,username,phone;
    email = etEmail.getText().toString();
    password = etPassword.getText().toString();
    username = etUsername.getText().toString();
    phone = etPhone.getText().toString();


    if (TextUtils.isEmpty(email)) {
      Toast.makeText(getApplicationContext(), "Please enter email...", Toast.LENGTH_LONG).show();
      progressBar.setVisibility(View.GONE);
      return;
    }
    if (TextUtils.isEmpty(username)) {
      Toast.makeText(getApplicationContext(), "Please enter username...", Toast.LENGTH_LONG).show();
      progressBar.setVisibility(View.GONE);
      return;
    }
    if (TextUtils.isEmpty(phone)) {
      Toast.makeText(getApplicationContext(), "Please enter phone...", Toast.LENGTH_LONG).show();
      progressBar.setVisibility(View.GONE);
      return;
    }
    if (TextUtils.isEmpty(password)) {
      Toast.makeText(getApplicationContext(), "Please enter password...", Toast.LENGTH_LONG).show();
      progressBar.setVisibility(View.GONE);
      return;
    }

    mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
              @Override
              public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                  Toast.makeText(getApplicationContext(), "Registration successful!", Toast.LENGTH_LONG).show();
                  progressBar.setVisibility(View.GONE);
                  FirebaseUser user = mAuth.getCurrentUser();
                  if (user != null) {
                    writeNewUser(user.getUid(), username, email, phone);
                  }
                  Intent intent = new Intent(MainActivity.this,
                          LoginActivity.class);
                  startActivity(intent);
                  finish();
                } else {
                  Toast.makeText(getApplicationContext(), "Registration failed!!" + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                  progressBar.setVisibility(View.GONE);
                }
              }
            });
  }
  private void loginUserAccount() {
    progressBar.setVisibility(View.VISIBLE);

    String email, password;
    email = etEmail.getText().toString();
    password = etPassword.getText().toString();

    if (TextUtils.isEmpty(email)) {
      Toast.makeText(getApplicationContext(), "Please enter email...", Toast.LENGTH_LONG).show();
      progressBar.setVisibility(View.GONE);
      return;
    }
    if (TextUtils.isEmpty(password)) {
      Toast.makeText(getApplicationContext(), "Please enter password...", Toast.LENGTH_LONG).show();
      progressBar.setVisibility(View.GONE);
      return;
    }

    mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
              @Override
              public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                  Toast.makeText(getApplicationContext(), "Login successful!", Toast.LENGTH_LONG).show();
                  progressBar.setVisibility(View.GONE);

                  Intent intent = new Intent(MainActivity.this,
                          HomeActivity.class);
                  startActivity(intent);
                  finish();
                } else {
                  Toast.makeText(getApplicationContext(), "Login failed!!", Toast.LENGTH_LONG).show();
                  progressBar.setVisibility(View.GONE);
                }
              }
            });
  }
  private void writeNewUser(String userId, String name, String email, String phone) {
    User user = new User(name, email, phone);

    mDatabase.child("users").child(userId).setValue(user)
            .addOnCompleteListener(new OnCompleteListener<Void>() {
              @Override
              public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                  Log.d("Firebase", "User data saved successfully.");
                } else {
                  Log.e("Firebase", "Failed to save user data.", task.getException());
                }
              }
            });
  }


}