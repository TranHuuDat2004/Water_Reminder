package com.example.canvas;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class Started1Activity extends AppCompatActivity {

  private String userId;
  private Button nextButton, skip;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.started_1); // Replace with your layout


    //Example
    nextButton = findViewById(R.id.btnNext);
    skip = findViewById(R.id.btnSkip);

    // You can now use the userId in this activity
    // Example: Log the user ID
    //Log.d("Started1Activity", "User ID: " + userId);

    nextButton.setOnClickListener(v -> {
      Intent intent = new Intent(Started1Activity.this, Started2Activity.class);
      startActivity(intent);
      finish();
    });

    skip.setOnClickListener(v -> {
      Intent intent = new Intent(Started1Activity.this, LoginActivity.class);
      startActivity(intent);
      finish();
    });


  }

}