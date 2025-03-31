package com.example.canvas;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class Started2Activity extends AppCompatActivity {

  private String userId;
  private Button nextButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.started_2); // Replace with your layout

    // Get the user ID from the Intent
    userId = getIntent().getStringExtra("userId");

    //Example
    nextButton = findViewById(R.id.btnNext);

    // You can now use the userId in this activity
    // Example: Log the user ID
    //Log.d("Started1Activity", "User ID: " + userId);

    nextButton.setOnClickListener(v -> {
      Intent intent = new Intent(Started2Activity.this, Started3Activity.class);
      intent.putExtra("userId", userId);
      startActivity(intent);
      finish();
    });
  }

}