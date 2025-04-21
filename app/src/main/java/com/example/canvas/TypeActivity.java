package com.example.canvas; // Hoặc package của bạn

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

// Import lớp cơ sở điều hướng của bạn
// Đảm bảo tên lớp 'BaseNavigationActivity' là chính xác
import com.example.canvas.NavigationActivity;

// Kế thừa từ lớp Activity cơ sở chứa logic điều hướng
public class TypeActivity extends NavigationActivity {

    private static final String TAG = "TypeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Đặt layout XML tĩnh cho Activity này
        // *** QUAN TRỌNG: Bạn cần tạo file res/layout/activity_type.xml ***
        // (Hoặc thay đổi R.layout.activity_type thành tên file layout thực tế của bạn)
        try {
            setContentView(R.layout.type); // <<< SỬ DỤNG LAYOUT CHO TYPE
        } catch (Exception e) {
            Log.e(TAG, "Error setting content view. Check R.layout.activity_type exists and is valid.", e);
            Toast.makeText(this, "Layout Error (Type)", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Gọi phương thức setup của lớp cơ sở ĐIỀU HƯỚNG SAU setContentView
        try {
            setupBottomNavigation();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up bottom navigation in TypeActivity.", e);
            Toast.makeText(this, "Navigation Error (Type)", Toast.LENGTH_LONG).show();
        }

        Log.d(TAG, "TypeActivity created and static layout displayed.");
    }

    // Implement phương thức trừu tượng để cho BaseNavigationActivity biết mục nào đang active
    @Override
    protected int getCurrentBottomNavigationItemId() {
        // Trả về ID của mục "Type" trong tệp menu.xml của bạn
        return R.id.navTypeButton; // <<< Đảm bảo ID này khớp với menu item "Type"
    }

    // --- Không cần logic động hoặc findViewById cho nội dung tĩnh ---
}