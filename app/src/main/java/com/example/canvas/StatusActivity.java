package com.example.canvas;

import androidx.annotation.NonNull;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.canvas.models.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// Kế thừa từ lớp điều hướng cơ sở
public class StatusActivity extends NavigationActivity {

  // Views
  private TextView walkTextView, caloTextView, sleepTextView, heartTextView;
  private ProgressBar loadingProgressBar;
  private MaterialButton addButton;

  // Biến lưu User ID cho toàn Activity
  private String activityUserId; // <<< Sửa ở đây: Đổi tên biến rõ ràng hơn

  private static final String TAG = "StatusActivity";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.status_activity);
    setupBottomNavigation(); // Gọi setup điều hướng

    // --- Lấy và Lưu User ID NGAY ĐẦU onCreate ---
    String retrievedUserId = getIntent().getStringExtra("userId"); // Lấy từ Intent
    if (retrievedUserId == null || retrievedUserId.isEmpty()) {
      Log.e(TAG, "CRITICAL: User ID is null or empty in Intent!");
      Toast.makeText(this, "Lỗi phiên đăng nhập!", Toast.LENGTH_LONG).show();
      // Cân nhắc chuyển về màn hình Login hoặc xử lý khác
      finish(); // Kết thúc Activity nếu không có ID
      return;
    }
    // Gán vào biến thành viên ngay lập tức
    this.activityUserId = retrievedUserId;
    Log.d(TAG, "User ID set for Activity: " + this.activityUserId);
    // --- Hoàn thành lấy và lưu User ID ---


    // Tìm các Views hiển thị dữ liệu
    walkTextView = findViewById(R.id.walkTextView);
    caloTextView = findViewById(R.id.caloTextView);
    sleepTextView = findViewById(R.id.sleepTextView);
    heartTextView = findViewById(R.id.heartTextView);
    loadingProgressBar = findViewById(R.id.loading_progress_bar);
    addButton = findViewById(R.id.addButton); // Tìm nút Add

    // Kiểm tra các Views
    if (loadingProgressBar == null) {
      Log.e(TAG, "FATAL ERROR: loading_progress_bar not found!");
      // Xử lý lỗi layout nghiêm trọng
      finish();
      return;
    }
    if (addButton == null) {
      Log.e(TAG, "ERROR: addButton not found! The add functionality will be disabled.");
      // Nút không hoạt động, nhưng không nhất thiết phải dừng Activity
    } else {
      // Thiết lập listener cho nút Add (nếu tìm thấy)
      addButton.setOnClickListener(v -> {
        // Chỉ cần gọi hàm hiển thị dialog, không cần gán userId ở đây nữa
        showAddReminderDialog();
      });
    }

    // Hiển thị loading và tải dữ liệu user (sử dụng biến thành viên activityUserId)
    showLoading(true);
    fetchUserData(this.activityUserId); // Truyền ID đã lưu
  }

  // --- Phương thức hiển thị dialog (Không thay đổi logic bên trong) ---
  private void showAddReminderDialog() {
    // ... (Toàn bộ nội dung hàm showAddReminderDialog giữ nguyên như trước) ...
    // Nó sẽ sử dụng biến thành viên 'this.activityUserId' khi lưu vào Firestore
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    LayoutInflater inflater = this.getLayoutInflater();
    View dialogView = inflater.inflate(R.layout.dialog_add_reminder, null);

    ImageView closeButton = dialogView.findViewById(R.id.back_button);
    TimePicker timePicker = dialogView.findViewById(R.id.timePicker);
    EditText editTextAmount = dialogView.findViewById(R.id.edit_lastName); // Nhớ đổi ID này trong XML
    Button saveButton = dialogView.findViewById(R.id.save_button);

    if (closeButton == null || timePicker == null || editTextAmount == null || saveButton == null) {
      Log.e(TAG, "Error finding views inside dialog_add_reminder.xml. Check IDs.");
      Toast.makeText(this, "Dialog layout error!", Toast.LENGTH_SHORT).show();
      return;
    }

    builder.setView(dialogView);
    final AlertDialog dialog = builder.create();

    closeButton.setOnClickListener(v -> dialog.dismiss());

    saveButton.setOnClickListener(v -> {
      int hour, minute;
      // ... (lấy giờ phút) ...
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        hour = timePicker.getHour();
        minute = timePicker.getMinute();
      } else {
        hour = timePicker.getCurrentHour();
        minute = timePicker.getCurrentMinute();
      }

      String amountString = editTextAmount.getText().toString().trim();
      int amountMl = 0;

      // ... (validate amountString) ...
      if (amountString.isEmpty()) {
        editTextAmount.setError("Vui lòng nhập lượng nước");
        editTextAmount.requestFocus();
        return;
      }
      try {
        amountMl = Integer.parseInt(amountString);
        if (amountMl <= 0) {
          editTextAmount.setError("Lượng nước phải lớn hơn 0");
          editTextAmount.requestFocus();
          return;
        }
        editTextAmount.setError(null);
      } catch (NumberFormatException e) {
        editTextAmount.setError("Vui lòng nhập số hợp lệ");
        editTextAmount.requestFocus();
        return;
      }

      // (Trong saveButton.setOnClickListener(v -> { ... sau khi validate dữ liệu ...)

      // --- Dữ liệu hợp lệ, tiến hành LƯU VÀO FIRESTORE ---

      if (this.activityUserId == null || this.activityUserId.isEmpty()) {
        Log.e(TAG, "Cannot save reminder: activityUserId is missing.");
        Toast.makeText(StatusActivity.this, "Lỗi: Không tìm thấy ID người dùng.", Toast.LENGTH_SHORT).show();
        return;
      }

      // 1. Tạo đối tượng Calendar để lấy thời gian hiện tại HOẶC kết hợp với TimePicker
      Calendar calendar = Calendar.getInstance();
      // Tùy chọn 1: Lưu thời điểm chính xác lúc nhấn Save
      // Để calendar như hiện tại

      // Tùy chọn 2: Kết hợp ngày hiện tại với giờ/phút từ TimePicker
      // (Hữu ích nếu bạn muốn người dùng có thể nhập thời gian khác)
        /*
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0); // Đặt giây/ms về 0 nếu muốn
        calendar.set(Calendar.MILLISECOND, 0);
        */
      // Lấy đối tượng Date từ Calendar
      Date entryTime = calendar.getTime();

      // 2. Tạo Map để lưu dữ liệu
      Map<String, Object> reminderData = new HashMap<>();
      reminderData.put("hour", hour);                 // Giữ lại nếu vẫn cần (hoặc có thể bỏ)
      reminderData.put("minute", minute);             // Giữ lại nếu vẫn cần (hoặc có thể bỏ)
      reminderData.put("amountMl", amountMl);         // Kiểu Number (quan trọng)
      reminderData.put("userId", this.activityUserId); // Kiểu String
      // ---->>>> THÊM TRƯỜNG TIMESTAMP <<<<----
      reminderData.put("timestamp", entryTime);       // Kiểu Timestamp (Firestore tự chuyển đổi Date)

      // 3. Lấy tham chiếu và lưu
      FirebaseFirestore db = FirebaseFirestore.getInstance();
      CollectionReference remindersRef = db.collection("Reminders");
      remindersRef.add(reminderData)
              .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Reminder added with timestamp: " + entryTime.toString());
                Toast.makeText(StatusActivity.this, "Đã lưu thành công!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
              })
              .addOnFailureListener(e -> {
                Log.e(TAG, "Error adding reminder", e);
                Toast.makeText(StatusActivity.this, "Lỗi khi lưu.", Toast.LENGTH_SHORT).show();
              });
// (Kết thúc của saveButton.setOnClickListener)
    });

    dialog.show();
  }


  // Implement phương thức trừu tượng từ BaseNavigationActivity (giữ nguyên)
  @Override
  protected int getCurrentBottomNavigationItemId() {
    return R.id.navHomeButton;
  }

  // --- Các phương thức fetchUserData, showLoading, showDefaultValues giữ nguyên ---
  // Đảm bảo fetchUserData sử dụng userId được truyền vào (mà chúng ta đã lấy từ this.activityUserId trong onCreate)
  private void fetchUserData(String userId) {
    // ... (Mã fetchUserData không đổi) ...
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    DocumentReference userRef = db.collection("users").document(userId); // Sử dụng userId truyền vào

    Log.d(TAG, "Firestore get() request initiated for user: " + userId);

    userRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
      @Override
      public void onSuccess(DocumentSnapshot documentSnapshot) {
        showLoading(false);

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
        showLoading(false);
        Log.e(TAG, "Error getting user data for user: " + userId, e);

        // ... (Xử lý lỗi Firestore không đổi) ...
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
    // ... (Mã showLoading không đổi) ...
    if (loadingProgressBar == null) return;

    if (isLoading) {
      loadingProgressBar.setVisibility(View.VISIBLE);
      if (walkTextView != null) walkTextView.setVisibility(View.INVISIBLE);
      if (caloTextView != null) caloTextView.setVisibility(View.INVISIBLE);
      if (sleepTextView != null) sleepTextView.setVisibility(View.INVISIBLE);
      if (heartTextView != null) heartTextView.setVisibility(View.INVISIBLE);
    } else {
      loadingProgressBar.setVisibility(View.GONE);
      if (walkTextView != null) walkTextView.setVisibility(View.VISIBLE);
      if (caloTextView != null) caloTextView.setVisibility(View.VISIBLE);
      if (sleepTextView != null) sleepTextView.setVisibility(View.VISIBLE);
      if (heartTextView != null) heartTextView.setVisibility(View.VISIBLE);
    }
  }

  private void showDefaultValues() {
    // ... (Mã showDefaultValues không đổi) ...
    if (walkTextView != null) walkTextView.setText("0 steps");
    if (caloTextView != null) caloTextView.setText("0 kcol");
    if (sleepTextView != null) sleepTextView.setText("0 hours");
    if (heartTextView != null) heartTextView.setText("0 bpm");
  }
}