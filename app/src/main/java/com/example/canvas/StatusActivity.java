package com.example.canvas;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.canvas.models.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class StatusActivity extends AppCompatActivity {

  private TextView walkTextView, caloTextView, sleepTextView, heartTextView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.status_activity);

    String userId = getIntent().getStringExtra("userId");
    if (userId == null) {
      Log.e("StatusActivity", "Error: User ID is null!");
      Toast.makeText(this, "Error: User ID is missing!", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    walkTextView = findViewById(R.id.walkTextView);
    caloTextView = findViewById(R.id.caloTextView);
    sleepTextView = findViewById(R.id.sleepTextView);
    heartTextView = findViewById(R.id.heartTextView);

    fetchUserData(userId);
  }

  private void fetchUserData(String userId) {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    DocumentReference userRef = db.collection("users").document(userId);

    Future<Void> future = Executors.newSingleThreadExecutor().submit(() -> {
      try {
        Task<DocumentSnapshot> task = userRef.get();
        DocumentSnapshot documentSnapshot = Tasks.await(task);

        if (documentSnapshot.exists()) {
          User user = documentSnapshot.toObject(User.class);

          if (user != null) {
            // Use the getters from User class with null check to avoid crash
            final int walk = (user.getWalk() != null) ? user.getWalk() : 0;
            final int calories = (user.getCalories() != null) ? user.getCalories() : 0;
            final int sleep = (user.getSleep() != null) ? user.getSleep() : 0;
            final int heart = (user.getHeart() != null) ? user.getHeart() : 0;

            runOnUiThread(() -> {
              walkTextView.setText(String.valueOf(walk));
              caloTextView.setText(String.valueOf(calories));
              sleepTextView.setText(String.valueOf(sleep));
              heartTextView.setText(String.valueOf(heart));
            });
          } else {
            Log.w("StatusActivity", "User object is null (likely deserialization issue).");
            runOnUiThread(() -> Toast.makeText(StatusActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show());
          }

        } else {
          Log.w("StatusActivity", "User document does not exist");
          runOnUiThread(() -> Toast.makeText(StatusActivity.this, "User not found", Toast.LENGTH_SHORT).show());
        }
      } catch (Exception e) {
        Log.e("StatusActivity", "Error getting user data in background thread: " + e.getMessage());
        runOnUiThread(() -> Toast.makeText(StatusActivity.this, "Error loading data", Toast.LENGTH_SHORT).show());
      }
      return null;
    });

    try {
      future.get(); // Wait for the background task to complete
    } catch (Exception e) {
      Log.e("StatusActivity", "Error fetching user data: " + e.getMessage());
      runOnUiThread(() -> Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show());
    }
  }
}