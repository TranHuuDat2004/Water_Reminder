package com.example.water;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ProfileActivity extends AppCompatActivity {

  private DonutProgress waterProgress;
  private TextView txtWaterDrank, txtWaterLeft;
  private FloatingActionButton btnAddWater;
  private int currentWater = 1500; // Đơn vị: ml
  private final int goalWater = 2000; // Mục tiêu: 2L (2000ml)

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Ánh xạ các thành phần
    waterProgress = findViewById(R.id.waterProgress);
    txtWaterDrank = findViewById(R.id.txtWaterDrank);
    txtWaterLeft = findViewById(R.id.txtWaterLeft);
    btnAddWater = findViewById(R.id.btnAddWater);

    // Cập nhật giao diện ban đầu
    updateUI();

    // Xử lý khi nhấn nút thêm nước
    btnAddWater.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        addWater(250); // Mỗi lần thêm 250ml
      }
    });
  }

  private void addWater(int amount) {
    if (currentWater + amount <= goalWater) {
      currentWater += amount;
      updateUI();
    } else {
      Toast.makeText(this, "Bạn đã đạt mục tiêu nước uống!", Toast.LENGTH_SHORT).show();
    }
  }

  private void updateUI() {
    int progress = (currentWater * 100) / goalWater;
    waterProgress.setProgress(progress);
    waterProgress.setText((currentWater / 1000.0) + "L");
    txtWaterDrank.setText("Đã uống: " + (currentWater / 1000.0) + "L");
    txtWaterLeft.setText("Còn thiếu: " + ((goalWater - currentWater) / 1000.0) + "L");
  }
}