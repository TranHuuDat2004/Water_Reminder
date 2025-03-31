package com.example.canvas;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Started2Activity extends AppCompatActivity {

  private Button btnNext;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.started_2);

    btnNext = findViewById(R.id.btnNext); // Thay "btnNext" bằng id thực tế của nút Next trong started_2.xml

    btnNext.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(Started2Activity.this, Started3Activity.class);
        startActivity(intent);
        finish();
      }
    });
  }
}