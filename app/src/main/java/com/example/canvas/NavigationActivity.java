package com.example.canvas; // Thay thế bằng tên gói của bạn

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // Thêm Log để debug (tùy chọn)
import android.view.MenuItem;
// Các import khác nếu cần (View, ViewGroup, CoordinatorLayout...)
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
// Import Firebase Auth
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public abstract class NavigationActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    protected BottomNavigationView bottomNavigationView;
    protected FirebaseAuth mAuth; // Thêm biến FirebaseAuth
    protected String currentUserId; // Biến để lưu trữ userId hiện tại

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance(); // Khởi tạo FirebaseAuth
    }

    /**
     * Các Activity con phải gọi phương thức này TRONG onCreate SAU setContentView
     * để thiết lập BottomNavigationView.
     */
    protected void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottomNavView);
        if (bottomNavigationView != null) {
            bottomNavigationView.setOnItemSelectedListener(this);
        } else {
            Log.e("BaseNavigationActivity", "BottomNavigationView with ID R.id.bottomNavView not found.");
        }
    }

    /**
     * Trả về ID của mục menu tương ứng với Activity hiện tại.
     * Activity con PHẢI override phương thức này.
     */
    protected abstract int getCurrentBottomNavigationItemId();

    @Override
    protected void onStart() {
        super.onStart();

        // --- Lấy User ID hiện tại khi Activity bắt đầu hoặc quay lại ---
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
            Log.d("BaseNavigationActivity", "Current User ID set in onStart: " + currentUserId);
        } else {
            currentUserId = null; // Đặt là null nếu không có ai đăng nhập
            Log.w("BaseNavigationActivity", "No user logged in during onStart.");
            // Bạn có thể thêm logic ở đây để chuyển hướng đến LoginActivity nếu cần
            // if (isLoginRequiredForThisActivity()) { // Tạo một phương thức kiểm tra nếu cần
            //     Intent loginIntent = new Intent(this, LoginActivity.class);
            //     loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            //     startActivity(loginIntent);
            //     finish();
            //     return; // Ngăn không cho phần còn lại của onStart chạy
            // }
        }

        // Cập nhật trạng thái selected cho mục menu của Activity hiện tại
        if (bottomNavigationView != null) {
            MenuItem currentItem = bottomNavigationView.getMenu().findItem(getCurrentBottomNavigationItemId());
            if (currentItem != null) {
                // Chỉ đặt checked nếu nó chưa được checked để tránh gọi lại onNavigationItemSelected không cần thiết
                if (!currentItem.isChecked()) {
                    currentItem.setChecked(true);
                }
            }
        }
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        // Nếu người dùng nhấn vào mục của Activity hiện tại, không làm gì cả
        if (itemId == getCurrentBottomNavigationItemId()) {
            return false; // Không làm gì, không coi là đã xử lý chuyển trang
        }

        Intent intent = null;

        // Sử dụng if-else if cho ID tài nguyên
        if (itemId == R.id.navHomeButton) {
            // Giả sử StatusActivity là màn hình Home
            intent = new Intent(this, StatusActivity.class);
        } else if (itemId == R.id.navProgressButton) {
            intent = new Intent(this, ProgressActivity.class);
        } else if (itemId == R.id.navTypeButton) {
            intent = new Intent(this, TypeActivity.class);
        } else if (itemId == R.id.navRewardsButton) {
            intent = new Intent(this, RewardsActivity.class);
        } else if (itemId == R.id.navSettingsButton) {
            intent = new Intent(this, SettingsActivity.class);
        }

        if (intent != null) {
            // *** THÊM userId VÀO INTENT Ở ĐÂY ***
            if (currentUserId != null && !currentUserId.isEmpty()) {
                intent.putExtra("userId", currentUserId); // Truyền userId đã lấy được trong onStart
                Log.d("BaseNavigationActivity", "Navigating to " + intent.getComponent().getClassName() + " with userId: " + currentUserId);
            } else {
                // Xử lý trường hợp không có userId (ví dụ: người dùng chưa đăng nhập)
                // Có thể hiển thị thông báo, không cho điều hướng, hoặc vẫn điều hướng
                // nhưng Activity đích phải xử lý việc không có userId.
                Log.w("BaseNavigationActivity", "Navigating to " + intent.getComponent().getClassName() + " WITHOUT userId (user not logged in or ID missing).");
                // Nếu Activity đích BẮT BUỘC phải có userId, bạn nên ngăn chặn ở đây
                // Toast.makeText(this, "Please log in to access this section.", Toast.LENGTH_SHORT).show();
                // return false; // Ngăn không cho điều hướng
            }

            // Giữ nguyên các cờ Intent quan trọng
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            // overridePendingTransition(0, 0); // Tùy chọn: Loại bỏ animation
            return true; // Đã xử lý
        }

        return false; // Không xử lý
    }

    // (Tùy chọn) Thêm phương thức để kiểm tra xem login có bắt buộc không
    // protected boolean isLoginRequiredForThisActivity() {
    //    // Mặc định là không bắt buộc, các Activity con có thể override nếu cần
    //    return false;
    // }
}