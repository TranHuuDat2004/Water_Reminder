package com.example.canvas;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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

public class LoginActivity extends AppCompatActivity {

  private EditText etEmail, etPassword;
  private Button btnLogin;
  private TextView tvSignUp, tvForgotPassword;
  private FirebaseAuth mAuth;
  private ProgressBar progressBar;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.login); // Use the correct layout file name

    etEmail = findViewById(R.id.etEmail);
    etPassword = findViewById(R.id.etPassword);
    btnLogin = findViewById(R.id.btnLogin);
    tvSignUp = findViewById(R.id.tvSignUp);
    tvForgotPassword = findViewById(R.id.tvForgotPassword);
    mAuth = FirebaseAuth.getInstance();


    tvSignUp.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        // Start the SignupActivity
               /* Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);*/
        Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
        startActivity(intent);
        finish();
      }
    });

    btnLogin.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        loginUserAccount();
      }
    });

    tvForgotPassword.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        //TODO: Implement forgot password functionality
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

                  Intent intent = new Intent(LoginActivity.this,
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
}