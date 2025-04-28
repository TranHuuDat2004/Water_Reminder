package com.example.canvas;

import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout; // Import FrameLayout
import android.widget.Toast;
import androidx.fragment.app.Fragment; // Import Fragment
import androidx.fragment.app.FragmentManager; // Import FragmentManager
import androidx.fragment.app.FragmentTransaction; // Import FragmentTransaction

// Import lớp cơ sở điều hướng của bạn
import com.example.canvas.NavigationActivity; // Hoặc tên lớp cơ sở của bạn

public class ProgressActivity extends NavigationActivity {

    private static final String TAG = "ProgressActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Đặt layout cho Activity - Layout này CHỈ cần chứa BottomNavigationView
        //    và một Container để chứa Fragment (ví dụ: FrameLayout).
        //    Ví dụ: tạo file res/layout/activity_progress.xml
        try {
            setContentView(R.layout.activity_progress); // <<< SỬ DỤNG LAYOUT MỚI CHO ACTIVITY
        } catch (Exception e) {
            Log.e(TAG, "Error setting activity layout. Check R.layout.activity_progress exists.", e);
            Toast.makeText(this, "Activity Layout Error", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 2. Setup bottom navigation (nó nằm trong activity_progress.xml)
        try {
            setupBottomNavigation();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up bottom navigation.", e);
            Toast.makeText(this, "Navigation Error", Toast.LENGTH_LONG).show();
        }

        // 3. Tải ProgressFragment vào Container
        if (savedInstanceState == null) { // Chỉ tải fragment lần đầu, tránh tạo lại khi xoay màn hình
            loadFragment(new ProgressFragment()); // Gọi hàm để tải fragment
        }

        Log.d(TAG, "ProgressActivity created, loading ProgressFragment.");
    }

    // Hàm để tải Fragment vào container trong layout của Activity
    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        // R.id.fragment_container là ID của FrameLayout trong activity_progress.xml
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        // fragmentTransaction.addToBackStack(null); // Bỏ comment nếu muốn thêm vào back stack
        fragmentTransaction.commit();
    }

    @Override
    protected int getCurrentBottomNavigationItemId() {
        // ID của mục "Progress" trong menu.xml
        return R.id.navProgressButton; // <<< Đảm bảo ID đúng
    }

    // --- Không cần các hàm setupViews, setupListeners, loadData, updateUI ở đây ---
}