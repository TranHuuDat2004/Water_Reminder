package com.example.canvas;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View; // Import View
import android.widget.Button;
import android.widget.ProgressBar; // Import ProgressBar
import android.widget.TextView;
import android.widget.Toast;

import com.example.canvas.models.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException; // Import cho lỗi cụ thể hơn

public class StatusActivity extends AppCompatActivity {

  private TextView walkTextView, caloTextView, sleepTextView, heartTextView;

  private Button navSettingsButton, navHomeButton;
  private ProgressBar loadingProgressBar; // Thêm ProgressBar
  private static final String TAG = "StatusActivity"; // Tag cho logging

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.status_activity); // Đảm bảo layout có ProgressBar với id loading_progress_bar

    String userId = getIntent().getStringExtra("userId");
    if (userId == null || userId.isEmpty()) { // Kiểm tra cả rỗng
      Log.e(TAG, "Error: User ID is null or empty!");
      Toast.makeText(this, "Error: User ID is missing!", Toast.LENGTH_SHORT).show();
      finish(); // Kết thúc Activity nếu không có userId
      return;
    }

    walkTextView = findViewById(R.id.walkTextView);
    caloTextView = findViewById(R.id.caloTextView);
    sleepTextView = findViewById(R.id.sleepTextView);
    heartTextView = findViewById(R.id.heartTextView);

    // ---> THÊM DÒNG NÀY VÀO ĐÂY <---
    loadingProgressBar = findViewById(R.id.loading_progress_bar); // Tìm ProgressBar

    // Kiểm tra lại cho chắc chắn (không bắt buộc nhưng nên làm khi debug)
    if (loadingProgressBar == null) {
      Log.e(TAG, "FATAL ERROR: loading_progress_bar not found in layout!");
      // Có thể hiển thị Toast hoặc dừng ứng dụng ở đây nếu muốn
      Toast.makeText(this, "Layout error: Loading indicator missing!", Toast.LENGTH_LONG).show();
      finish(); // Hoặc xử lý khác
      return;
    }


    // Bây giờ mới gọi showLoading
    showLoading(true);

    fetchUserData(userId);

    //Example
    navSettingsButton = findViewById(R.id.navSettingsButton);

    // You can now use the userId in this activity
    // Example: Log the user ID
    //Log.d("Started1Activity", "User ID: " + userId);

    navSettingsButton.setOnClickListener(v -> {
      Intent intent = new Intent(StatusActivity.this, SettingsActivity.class);
      intent.putExtra("userId", userId);
      startActivity(intent);
      finish();
    });

    //Example
    navHomeButton = findViewById(R.id.navHomeButton);

    // You can now use the userId in this activity
    // Example: Log the user ID
    //Log.d("Started1Activity", "User ID: " + userId);

    navHomeButton.setOnClickListener(v -> {
      Intent intent = new Intent(StatusActivity.this, StatusActivity.class);
      intent.putExtra("userId", userId);
      startActivity(intent);
      finish();
    });

  }


  private void fetchUserData(String userId) {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    DocumentReference userRef = db.collection("users").document(userId);

    userRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
      @Override
      public void onSuccess(DocumentSnapshot documentSnapshot) {
        showLoading(false); // Ẩn ProgressBar khi thành công (hoặc không tìm thấy)

        if (documentSnapshot != null && documentSnapshot.exists()) {
          User user = documentSnapshot.toObject(User.class);

          if (user != null) {
            // Sử dụng getters từ lớp User với kiểm tra null (như bạn đã làm)
            int walk = (user.getWalk() != null) ? user.getWalk() : 0;
            int calories = (user.getCalories() != null) ? user.getCalories() : 0;
            int sleep = (user.getSleep() != null) ? user.getSleep() : 0;
            int heart = (user.getHeart() != null) ? user.getHeart() : 0;

            // Cập nhật UI - Listener này thường chạy trên Main Thread rồi
            // nên không cần runOnUiThread, nhưng dùng cũng không sao.
            walkTextView.setText(String.valueOf(walk) + " steps");
            caloTextView.setText(String.valueOf(calories) + " kcol" );
            sleepTextView.setText(String.valueOf(sleep) + " hours");
            heartTextView.setText(String.valueOf(heart) + " bpm");
            Log.d(TAG, "User data loaded successfully for user: " + userId);

          } else {
            // Lỗi khi chuyển đổi DocumentSnapshot sang User POJO
            Log.w(TAG, "User object is null after deserialization for user: " + userId);
            Toast.makeText(StatusActivity.this, "Failed to parse user data", Toast.LENGTH_SHORT).show();
            // Có thể hiện giá trị mặc định hoặc thông báo lỗi trên UI
            showDefaultValues();
          }
        } else {
          // Document không tồn tại
          Log.w(TAG, "User document does not exist for user: " + userId);
          Toast.makeText(StatusActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
          // Hiện giá trị mặc định hoặc thông báo lỗi
          showDefaultValues();
        }
      }
    }).addOnFailureListener(new OnFailureListener() {
      @Override
      public void onFailure(@NonNull Exception e) {
        showLoading(false); // Ẩn ProgressBar khi có lỗi
        Log.e(TAG, "Error getting user data for user: " + userId, e); // Log lỗi đầy đủ

        // Phân tích lỗi cụ thể nếu cần
        if (e instanceof FirebaseFirestoreException) {
          FirebaseFirestoreException firestoreEx = (FirebaseFirestoreException) e;
          Log.e(TAG, "Firestore Error Code: " + firestoreEx.getCode());
          // Ví dụ: Xử lý lỗi không có mạng, quyền...
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
        // Hiện giá trị mặc định hoặc thông báo lỗi
        showDefaultValues();
      }
    });

    Log.d(TAG, "Firestore get() request initiated for user: " + userId);
    // Code ở đây sẽ chạy ngay lập tức, KHÔNG đợi kết quả từ Firestore
  }

  // Helper method để hiển thị/ẩn loading state
  private void showLoading(boolean isLoading) {
    if (isLoading) {
      loadingProgressBar.setVisibility(View.VISIBLE);
      walkTextView.setVisibility(View.INVISIBLE);
      caloTextView.setVisibility(View.INVISIBLE);
      sleepTextView.setVisibility(View.INVISIBLE);
      heartTextView.setVisibility(View.INVISIBLE);
    } else {
      loadingProgressBar.setVisibility(View.GONE);
      walkTextView.setVisibility(View.VISIBLE);
      caloTextView.setVisibility(View.VISIBLE);
      sleepTextView.setVisibility(View.VISIBLE);
      heartTextView.setVisibility(View.VISIBLE);
    }
  }

  // Helper method để hiển thị giá trị mặc định khi có lỗi hoặc không tìm thấy data
  private void showDefaultValues() {
    walkTextView.setText("0");
    caloTextView.setText("0");
    sleepTextView.setText("0");
    heartTextView.setText("0");
  }
}