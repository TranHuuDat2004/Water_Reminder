package com.example.canvas;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private TextView tvWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.started_1);

//        tvWelcome = findViewById(R.id.tvWelcome);

        // You can get the current user's information here (e.g., email, display name)
        // and display a welcome message.
        // Example:
        // FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // if (user != null) {
        //     tvWelcome.setText("Welcome, " + user.getDisplayName() + "!");
        // } else {
        //     tvWelcome.setText("Welcome!");
        // }
    }
}