package com.example.canvas;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Started3Activity extends AppCompatActivity {

  private Button btnNext;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.started_3);

    btnNext = findViewById(R.id.btnNext); // Thay "btnNext" bằng id thực tế của nút Next trong started_3.xml

    btnNext.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(Started3Activity.this, StatusActivity.class);
        startActivity(intent);
        finish();
      }
    });
  }
}