package com.example.canvas;

import androidx.annotation.NonNull;
// Xóa: import androidx.appcompat.app.AppCompatActivity; // Không cần nữa
import android.content.Intent; // Vẫn cần nếu dùng Intent ở nơi khác
import android.os.Bundle;
import android.util.Log;
import android.view.View;
// Xóa: import android.widget.Button; // Không cần tham chiếu trực tiếp Button nav nữa
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
// Import MenuItem nếu BaseNavigationActivity sử dụng (thường không cần trực tiếp trong lớp con)
// import android.view.MenuItem;

import com.example.canvas.models.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

// 1. Kế thừa từ BaseNavigationActivity
public class StatusActivity extends NavigationActivity {

  // Các View để hiển thị dữ liệu trạng thái
  private TextView walkTextView, caloTextView, sleepTextView, heartTextView;
  private ProgressBar loadingProgressBar; // ProgressBar cho trạng thái tải

  // Xóa: private Button navSettingsButton, navHomeButton; // Không cần nữa

  private static final String TAG = "StatusActivity"; // Tag cho logging

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // 2. Đặt layout cho Activity này
    setContentView(R.layout.status_activity); // Đảm bảo layout có ProgressBar với id loading_progress_bar

    // 3. Gọi setup của BaseNavigationActivity SAU setContentView
    setupBottomNavigation();

    // Lấy userId từ Intent (giữ nguyên)
    String userId = getIntent().getStringExtra("userId");
    if (userId == null || userId.isEmpty()) {
      Log.e(TAG, "Error: User ID is null or empty!");
      Toast.makeText(this, "Error: User ID is missing!", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    // Tìm các View hiển thị dữ liệu (giữ nguyên)
    walkTextView = findViewById(R.id.walkTextView);
    caloTextView = findViewById(R.id.caloTextView);
    sleepTextView = findViewById(R.id.sleepTextView);
    heartTextView = findViewById(R.id.heartTextView);
    loadingProgressBar = findViewById(R.id.loading_progress_bar);

    // Kiểm tra ProgressBar (giữ nguyên)
    if (loadingProgressBar == null) {
      Log.e(TAG, "FATAL ERROR: loading_progress_bar not found in layout!");
      Toast.makeText(this, "Layout error: Loading indicator missing!", Toast.LENGTH_LONG).show();
      finish();
      return;
    }

    // Hiển thị trạng thái đang tải và bắt đầu lấy dữ liệu (giữ nguyên)
    showLoading(true);
    fetchUserData(userId);

  }

  // 4. Implement phương thức trừu tượng từ BaseNavigationActivity
  @Override
  protected int getCurrentBottomNavigationItemId() {
    // Trả về ID của mục menu tương ứng với StatusActivity.
    // Giả sử StatusActivity là màn hình chính, tương ứng với nút Home.
    return R.id.navHomeButton; // <<=== Đảm bảo ID này đúng với mục trong menu.xml
  }


  // --- Các phương thức fetchUserData, showLoading, showDefaultValues giữ nguyên ---

  private void fetchUserData(String userId) {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    DocumentReference userRef = db.collection("users").document(userId);

    Log.d(TAG, "Firestore get() request initiated for user: " + userId); // Log khi bắt đầu request

    userRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
      @Override
      public void onSuccess(DocumentSnapshot documentSnapshot) {
        showLoading(false); // Ẩn ProgressBar

        if (documentSnapshot != null && documentSnapshot.exists()) {
          User user = documentSnapshot.toObject(User.class);

          if (user != null) {
            int walk = (user.getWalk() != null) ? user.getWalk() : 0;
            int calories = (user.getCalories() != null) ? user.getCalories() : 0;
            int sleep = (user.getSleep() != null) ? user.getSleep() : 0;
            int heart = (user.getHeart() != null) ? user.getHeart() : 0;

            walkTextView.setText(String.valueOf(walk) + " steps");
            caloTextView.setText(String.valueOf(calories) + " kcol");
            sleepTextView.setText(String.valueOf(sleep) + " hours");
            heartTextView.setText(String.valueOf(heart) + " bpm");
            Log.d(TAG, "User data loaded successfully for user: " + userId);

          } else {
            Log.w(TAG, "User object is null after deserialization for user: " + userId);
            Toast.makeText(StatusActivity.this, "Failed to parse user data", Toast.LENGTH_SHORT).show();
            showDefaultValues();
          }
        } else {
          Log.w(TAG, "User document does not exist for user: " + userId);
          Toast.makeText(StatusActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
          showDefaultValues();
        }
      }
    }).addOnFailureListener(new OnFailureListener() {
      @Override
      public void onFailure(@NonNull Exception e) {
        showLoading(false); // Ẩn ProgressBar
        Log.e(TAG, "Error getting user data for user: " + userId, e);

        if (e instanceof FirebaseFirestoreException) {
          FirebaseFirestoreException firestoreEx = (FirebaseFirestoreException) e;
          Log.e(TAG, "Firestore Error Code: " + firestoreEx.getCode());
          if (firestoreEx.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            Toast.makeText(StatusActivity.this, "Permission Denied.", Toast.LENGTH_LONG).show();
          } else if (firestoreEx.getCode() == FirebaseFirestoreException.Code.UNAVAILABLE) {
            Toast.makeText(StatusActivity.this, "Network Error. Please check connection.", Toast.LENGTH_LONG).show();
          } else {
            Toast.makeText(StatusActivity.this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
          }
        } else {
          Toast.makeText(StatusActivity.this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        showDefaultValues();
      }
    });
  }

  private void showLoading(boolean isLoading) {
    // Đảm bảo progressBar không null trước khi sử dụng
    if (loadingProgressBar == null) return;

    if (isLoading) {
      loadingProgressBar.setVisibility(View.VISIBLE);
      // Ẩn các TextView dữ liệu
      if (walkTextView != null) walkTextView.setVisibility(View.INVISIBLE);
      if (caloTextView != null) caloTextView.setVisibility(View.INVISIBLE);
      if (sleepTextView != null) sleepTextView.setVisibility(View.INVISIBLE);
      if (heartTextView != null) heartTextView.setVisibility(View.INVISIBLE);
    } else {
      loadingProgressBar.setVisibility(View.GONE);
      // Hiện các TextView dữ liệu
      if (walkTextView != null) walkTextView.setVisibility(View.VISIBLE);
      if (caloTextView != null) caloTextView.setVisibility(View.VISIBLE);
      if (sleepTextView != null) sleepTextView.setVisibility(View.VISIBLE);
      if (heartTextView != null) heartTextView.setVisibility(View.VISIBLE);
    }
  }

  private void showDefaultValues() {
    // Đảm bảo các TextView không null trước khi sử dụng
    if (walkTextView != null) walkTextView.setText("0 steps");
    if (caloTextView != null) caloTextView.setText("0 kcol");
    if (sleepTextView != null) sleepTextView.setText("0 hours");
    if (heartTextView != null) heartTextView.setText("0 bpm");
  }
}