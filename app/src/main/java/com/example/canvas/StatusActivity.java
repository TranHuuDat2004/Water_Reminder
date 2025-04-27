// File: StatusActivity.java
package com.example.canvas; // <-- Giữ nguyên package của bạn

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar; // Đã import
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

// Thêm các import cần thiết
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
// import com.google.android.material.floatingactionbutton.FloatingActionButton; // Bạn đang dùng MaterialButton
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
// import java.util.TimeZone; // Có thể không cần nếu không xử lý timezone phức tạp

public class StatusActivity extends NavigationActivity {

  private static final String TAG = "StatusActivity";
  private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1001;


  // Khai báo các View (Sử dụng ID từ code bạn cung cấp)
  private TextView tvWelcomeMessage;
  private TextView tvHydrationProgress;
  private TextView tvWaterPercent;
  private TextView tvNextReminderDisplay;
  private TextView tvLastWaterAmount;
  private ProgressBar progressBarWater; // ProgressBar hình tròn
  private MaterialButton fabAddWater;    // Nút thêm nước

  // Khai báo Firebase
  private FirebaseAuth mAuth;
  private FirebaseFirestore db;
  private FirebaseUser currentUser;
  private String userId;
  private String userDisplayName = "User"; // Tên mặc định

  // Biến lưu trữ dữ liệu
  private int currentWaterIntake = 0;
  private int waterGoal = 2100; // Giá trị mặc định, sẽ được ghi đè từ Firestore
  private long nextReminderTimestamp = 0;

  // Định dạng ngày giờ
  private final SimpleDateFormat firestoreDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
  private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Đảm bảo tên layout này đúng: "status_activity.xml"
    setContentView(R.layout.status_activity);

    // *** THÊM DÒNG NÀY ***
    setupBottomNavigation(); // Gọi phương thức từ lớp cha để thiết lập BottomNavigationView


    // Khởi tạo Firebase Auth và Firestore
    mAuth = FirebaseAuth.getInstance();
    db = FirebaseFirestore.getInstance();
    currentUser = mAuth.getCurrentUser();

    // Ánh xạ Views từ layout (Sử dụng ID bạn đã cung cấp)
    tvWelcomeMessage = findViewById(R.id.welcomeText);
    tvHydrationProgress = findViewById(R.id.amountText);
    tvWaterPercent = findViewById(R.id.a); // <-- ID 'a' có thể nên đổi tên cho dễ hiểu hơn trong XML
    tvNextReminderDisplay = findViewById(R.id.reminderTimeText);
    tvLastWaterAmount = findViewById(R.id.waterAmountText);
    progressBarWater = findViewById(R.id.progressCircle);
    fabAddWater = findViewById(R.id.addButton); // Nút thêm nước


    if (currentUser == null) {
      // Người dùng chưa đăng nhập
      Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
      Log.e(TAG, "User is not logged in. Cannot proceed.");
      // Gán userId giả để test (CẦN XÓA KHI CÓ LOGIN THẬT)
      // userId = "testUserId";
      // Hiển thị trạng thái mặc định/chưa đăng nhập
      updateWelcomeMessage(); // Sẽ hiển thị "Welcome, User! 👋"
      waterGoal = 0; // Đặt goal về 0 hoặc giá trị mặc định khác
      currentWaterIntake = 0;
      updateHydrationUI();
      updateReminderUI();
      tvLastWaterAmount.setText("0ml");
      fabAddWater.setEnabled(false); // Vô hiệu hóa nút thêm nước
    } else {
      // Người dùng đã đăng nhập
      userId = currentUser.getUid();
      fabAddWater.setEnabled(true); // Bật nút thêm nước
      // Bắt đầu chuỗi load dữ liệu
      loadUserProfile();
    }

    // Xử lý sự kiện click nút "+"
    fabAddWater.setOnClickListener(v -> {
      if (userId == null || userId.isEmpty()) { // Kiểm tra lại userId hợp lệ
        Toast.makeText(this, "Please log in to add water.", Toast.LENGTH_SHORT).show();
        return;
      }
      showAddWaterDialog();
    });

    // Yêu cầu quyền Notification (Android 13+)
    checkAndRequestNotificationPermission();
  }

  // Implement phương thức trừu tượng từ NavigationActivity (giữ nguyên)
  @Override
  protected int getCurrentBottomNavigationItemId() {
    return R.id.navHomeButton;
  }

  // 1. Load thông tin Profile người dùng (lấy goal và tên)
  private void loadUserProfile() {
    if (userId == null || userId.isEmpty()) return;

    DocumentReference userProfileRef = db.collection("users").document(userId);
    userProfileRef.get().addOnSuccessListener(profileSnapshot -> {
      String tempDisplayName = null; // Biến tạm lưu tên

      if (profileSnapshot.exists()) {
        Log.d(TAG, "User profile loaded successfully for userId: " + userId);
        // Lấy waterGoal
        if (profileSnapshot.contains("waterGoal")) {
          // Sử dụng get() an toàn hơn getLong() trực tiếp
          Number goal = profileSnapshot.get("waterGoal", Number.class);
          if (goal != null) {
            waterGoal = goal.intValue();
            Log.d(TAG, "Water Goal set to: " + waterGoal);
          } else {
            waterGoal = 2100; // Fallback nếu field tồn tại nhưng là null
            Log.w(TAG, "waterGoal field is null, using default: " + waterGoal);
          }

        } else {
          waterGoal = 2100; // Fallback nếu không có field
          Log.w(TAG, "waterGoal field not found, using default: " + waterGoal);
        }

        // --- Logic lấy tên được cải thiện ---
        // 1. Ưu tiên lấy 'username' từ Firestore
        if (profileSnapshot.contains("username")) {
          String firestoreUsername = profileSnapshot.getString("username");
          if (firestoreUsername != null && !firestoreUsername.trim().isEmpty()) {
            tempDisplayName = firestoreUsername.trim();
            Log.d(TAG, "Using 'username' from Firestore: " + tempDisplayName);
          } else {
            Log.w(TAG, "'username' field found in Firestore but is null or empty.");
          }
        } else {
          Log.w(TAG, "'username' field not found in Firestore profile.");
        }

        // 2. Nếu không có 'username' từ Firestore, thử lấy displayName từ Firebase Auth profile
        if (tempDisplayName == null && currentUser != null && currentUser.getDisplayName() != null && !currentUser.getDisplayName().trim().isEmpty()) {
          tempDisplayName = currentUser.getDisplayName().trim();
          Log.d(TAG, "Using 'displayName' from Firebase Auth profile as fallback: " + tempDisplayName);
        }
        // --- Kết thúc cải thiện logic tên ---

      } else {
        // Hồ sơ người dùng không tồn tại trong Firestore
        Log.w(TAG, "User profile document does not exist for userId: " + userId);
        waterGoal = 2100; // Dùng goal mặc định

        // Fallback tên: Auth displayName -> email -> "User"
        if (currentUser != null && currentUser.getDisplayName() != null && !currentUser.getDisplayName().trim().isEmpty()) {
          tempDisplayName = currentUser.getDisplayName().trim();
        }
        // (Không cần fallback email ở đây nếu ưu tiên username/displayName)
      }

      // 3. Nếu vẫn không có tên, fallback dùng email (chỉ khi cần)
      if (tempDisplayName == null && currentUser != null && currentUser.getEmail() != null) {
        tempDisplayName = currentUser.getEmail();
        Log.d(TAG, "Falling back to email address.");
      }

      // 4. Gán giá trị cuối cùng (nếu vẫn null thì gán "User")
      userDisplayName = (tempDisplayName != null) ? tempDisplayName : "User";
      Log.d(TAG, "Final userDisplayName set to: " + userDisplayName);


      // Cập nhật lời chào
      updateWelcomeMessage();
      // Load dữ liệu nước hàng ngày SAU KHI đã có waterGoal và tên
      loadDailyWaterData();

    }).addOnFailureListener(e -> {
      Log.e(TAG, "Error getting user profile for userId: " + userId, e);
      Toast.makeText(this, "Failed to load user profile.", Toast.LENGTH_SHORT).show();
      // Sử dụng giá trị mặc định khi lỗi
      waterGoal = 2100;
      userDisplayName = "User"; // Tên mặc định khi lỗi
      updateWelcomeMessage();
      loadDailyWaterData(); // Vẫn thử load dữ liệu nước
    });
  }

  // 2. Load dữ liệu nước uống của ngày hôm nay từ water_tracker
  private void loadDailyWaterData() {
    if (userId == null || userId.isEmpty()) return;

    String todayDate = firestoreDateFormat.format(new Date());
    DocumentReference dailyLogRef = db.collection("water_tracker").document(userId)
            .collection("daily_logs").document(todayDate);

    dailyLogRef.get().addOnSuccessListener(documentSnapshot -> {
      if (documentSnapshot.exists()) {
        Log.d(TAG, "Daily water log loaded for date: " + todayDate);
        Long totalWLong = documentSnapshot.getLong("totalWater");
        // Gán vào biến int, kiểm tra null trước
        currentWaterIntake = (totalWLong != null) ? totalWLong.intValue() : 0;

        Long lastALong = documentSnapshot.getLong("lastAddedAmount");
        // Gán vào biến long, kiểm tra null trước
        long lastAmount = (lastALong != null) ? lastALong : 0L; // 0L là giá trị long mặc định

        Long nextRLong = documentSnapshot.getLong("nextReminderTimestamp");
        // Gán vào biến long, kiểm tra null trước
        nextReminderTimestamp = (nextRLong != null) ? nextRLong : 0L;


        tvLastWaterAmount.setText(lastAmount + "ml");
      } else {
        // Chưa có record cho ngày hôm nay
        Log.d(TAG, "No daily water log found for date: " + todayDate + ". Setting defaults.");
        currentWaterIntake = 0;
        nextReminderTimestamp = 0;
        tvLastWaterAmount.setText("0ml");
      }
      // Cập nhật UI sau khi có dữ liệu (hoặc biết là chưa có)
      // Chỉ cập nhật nếu Activity còn tồn tại
      if (!isFinishing() && !isDestroyed()) {
        updateHydrationUI();
        updateReminderUI();
      }
    }).addOnFailureListener(e -> {
      Log.w(TAG, "Error getting daily water log for date: " + todayDate, e);
      // Xử lý lỗi (ví dụ: hiển thị giá trị mặc định)
      if (!isFinishing() && !isDestroyed()) {
        currentWaterIntake = 0;
        nextReminderTimestamp = 0;
        tvLastWaterAmount.setText("0ml");
        updateHydrationUI();
        updateReminderUI();
        Toast.makeText(this, "Failed to load daily water data.", Toast.LENGTH_SHORT).show();
      }
    });
  }


  // Hiển thị Dialog thêm nước
  private void showAddWaterDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    LayoutInflater inflater = this.getLayoutInflater();
    // *** KIỂM TRA LẠI TÊN FILE LAYOUT NÀY ***
    View dialogView = inflater.inflate(R.layout.dialog_add_reminder, null);
    builder.setView(dialogView);

    // Đảm bảo các ID này tồn tại trong dialog_add_reminder.xml
    EditText etWaterAmount = dialogView.findViewById(R.id.etWaterAmount);
    TextView tvSelectedTime = dialogView.findViewById(R.id.tvSelectedTime);

    // --- Phần xử lý dialog giữ nguyên ---
    final int[] selectedHour = {-1};
    final int[] selectedMinute = {-1};
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.HOUR_OF_DAY, 1);
    selectedHour[0] = calendar.get(Calendar.HOUR_OF_DAY);
    selectedMinute[0] = calendar.get(Calendar.MINUTE);
    tvSelectedTime.setText(String.format(Locale.getDefault(), "%02d:%02d", selectedHour[0], selectedMinute[0]));
    tvSelectedTime.setOnClickListener(v -> {
      Calendar now = Calendar.getInstance();
      int currentHour = now.get(Calendar.HOUR_OF_DAY);
      int currentMinute = now.get(Calendar.MINUTE);
      TimePickerDialog timePickerDialog = new TimePickerDialog(this,
              (view, hourOfDay, minute) -> {
                selectedHour[0] = hourOfDay;
                selectedMinute[0] = minute;
                tvSelectedTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
              },
              selectedHour[0] != -1 ? selectedHour[0] : currentHour,
              selectedMinute[0] != -1 ? selectedMinute[0] : currentMinute,
              true);
      timePickerDialog.show();
    });
    builder.setPositiveButton("Add", (dialog, which) -> {
      String amountStr = etWaterAmount.getText().toString();
      if (!amountStr.isEmpty()) {
        try {
          int amountToAdd = Integer.parseInt(amountStr);
          if (amountToAdd <= 0) {
            Toast.makeText(this, "Please enter a positive amount.", Toast.LENGTH_SHORT).show();
            return;
          }
          if (selectedHour[0] != -1 && selectedMinute[0] != -1) {
            saveWaterRecordAndScheduleReminder(amountToAdd, selectedHour[0], selectedMinute[0]);
          } else {
            Toast.makeText(this, "Please select a reminder time.", Toast.LENGTH_SHORT).show();
          }
        } catch (NumberFormatException e) {
          Toast.makeText(this, "Invalid number format.", Toast.LENGTH_SHORT).show();
        }
      } else {
        Toast.makeText(this, "Please enter the amount.", Toast.LENGTH_SHORT).show();
      }
    });
    builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
    AlertDialog dialog = builder.create();
    dialog.show();
    // --- Kết thúc phần xử lý dialog ---
  }

  // Lưu dữ liệu vào water_tracker và đặt lịch thông báo
  private void saveWaterRecordAndScheduleReminder(int waterAmount, int reminderHour, int reminderMinute) {
    if (userId == null || userId.isEmpty()) {
      Toast.makeText(this, "Cannot save data. User not identified.", Toast.LENGTH_SHORT).show();
      return;
    }

    // --- Phần tính toán và lưu Firestore giữ nguyên ---
    int newTotalWater = currentWaterIntake + waterAmount;
    Calendar reminderCalendar = Calendar.getInstance();
    reminderCalendar.set(Calendar.HOUR_OF_DAY, reminderHour);
    reminderCalendar.set(Calendar.MINUTE, reminderMinute);
    reminderCalendar.set(Calendar.SECOND, 0);
    reminderCalendar.set(Calendar.MILLISECOND, 0);
    if (reminderCalendar.getTimeInMillis() <= System.currentTimeMillis()) {
      reminderCalendar.add(Calendar.DAY_OF_YEAR, 1);
      Log.d(TAG,"Reminder time is in the past for today, scheduling for tomorrow.");
    }
    long reminderTimestamp = reminderCalendar.getTimeInMillis();
    String todayDate = firestoreDateFormat.format(new Date());
    DocumentReference dailyLogRef = db.collection("water_tracker").document(userId)
            .collection("daily_logs").document(todayDate);
    Map<String, Object> waterData = new HashMap<>();
    waterData.put("totalWater", newTotalWater);
    waterData.put("lastAddedAmount", waterAmount);
    waterData.put("lastAddedTimestamp", System.currentTimeMillis());
    waterData.put("nextReminderTimestamp", reminderTimestamp);
    waterData.put("dailyGoal", waterGoal); // Lưu goal của ngày đó
    dailyLogRef.set(waterData, SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
              Log.d(TAG, "Daily water log successfully written for date: " + todayDate);
              Toast.makeText(StatusActivity.this, "Water added!", Toast.LENGTH_SHORT).show();
              // Cập nhật biến cục bộ và UI (chỉ nếu activity còn tồn tại)
              if (!isFinishing() && !isDestroyed()) {
                currentWaterIntake = newTotalWater;
                nextReminderTimestamp = reminderTimestamp;
                tvLastWaterAmount.setText(waterAmount + "ml");
                updateHydrationUI();
                updateReminderUI();
                // Đặt lịch thông báo
                scheduleNotification(reminderTimestamp);
              }
            })
            .addOnFailureListener(e -> {
              Log.w(TAG, "Error writing daily water log for date: " + todayDate, e);
              if (!isFinishing() && !isDestroyed()) {
                Toast.makeText(StatusActivity.this, "Failed to save data.", Toast.LENGTH_SHORT).show();
              }
            });
    // --- Kết thúc phần lưu Firestore ---
  }

  // --- Cập nhật các thành phần UI ---

  private void updateWelcomeMessage() {
    // Đảm bảo tvWelcomeMessage không null trước khi setText
    if (tvWelcomeMessage != null) {
      tvWelcomeMessage.setText("Welcome, " + userDisplayName + "! 👋");
    }
  }

  private void updateHydrationUI() {
    // Đảm bảo các view không null
    if (tvHydrationProgress == null || tvWaterPercent == null || progressBarWater == null) {
      Log.e(TAG, "One or more hydration UI elements are null!");
      return;
    }

    tvHydrationProgress.setText(String.format(Locale.getDefault(), "%d/%dml", currentWaterIntake, waterGoal));

    int progressPercent = 0;
    int progressValue = 0;

    if (waterGoal > 0) {
      progressPercent = (int) (((float) currentWaterIntake / waterGoal) * 100);
      progressPercent = Math.min(progressPercent, 100); // Không vượt quá 100%
      progressValue = Math.min(currentWaterIntake, waterGoal); // Giá trị cho progress bar
    } else {
      // Xử lý trường hợp goal = 0 (ví dụ: chưa load xong hoặc user đặt = 0)
      progressPercent = 0;
      progressValue = 0;
      Log.w(TAG, "Water goal is 0, setting progress to 0.");
    }


    tvWaterPercent.setText(String.format(Locale.getDefault(), "%d%%", progressPercent));

    // Cập nhật ProgressBar (bỏ comment và sửa lại)
    progressBarWater.setMax(waterGoal > 0 ? waterGoal : 100); // Đặt max, tránh max=0
    progressBarWater.setProgress(progressValue);

  }

  private void updateReminderUI() {
    // Đảm bảo view không null
    if (tvNextReminderDisplay == null) return;

    if (nextReminderTimestamp > 0 && nextReminderTimestamp > System.currentTimeMillis()) {
      Date reminderDate = new Date(nextReminderTimestamp);
      tvNextReminderDisplay.setText(timeFormat.format(reminderDate));
    } else {
      tvNextReminderDisplay.setText("--:--");
    }
  }


  // --- Xử lý thông báo và quyền (Giữ nguyên phần logic) ---

  private void scheduleNotification(long triggerTimestamp) {
    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    Intent intent = new Intent(this, ReminderBroadcastReceiver.class);
    PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    if (alarmManager == null) {
      Log.e(TAG, "AlarmManager is null.");
      Toast.makeText(this, "Cannot access Alarm service.", Toast.LENGTH_SHORT).show();
      return;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      if (!alarmManager.canScheduleExactAlarms()) {
        Log.w(TAG, "SCHEDULE_EXACT_ALARM permission needed.");
        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("To ensure timely reminders, this app needs permission to schedule exact alarms. Please grant this permission in the next screen.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                  Intent settingsIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                  if (settingsIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(settingsIntent);
                  } else {
                    Log.e(TAG,"Could not resolve Intent: Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM");
                    Toast.makeText(StatusActivity.this, "Could not open permission settings.", Toast.LENGTH_SHORT).show();
                  }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                  Toast.makeText(StatusActivity.this, "Reminder cannot be set without permission.", Toast.LENGTH_SHORT).show();
                  dialog.dismiss();
                })
                .show();
        return; // Dừng lại
      }
    }

    try {
      alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimestamp, pendingIntent);
      Log.d(TAG, "Alarm scheduled for: " + new Date(triggerTimestamp));
    } catch (SecurityException se) {
      Log.e(TAG, "SecurityException scheduling alarm.", se);
      Toast.makeText(this, "Could not schedule reminder due to permissions.", Toast.LENGTH_LONG).show();
    }
  }

  private void checkAndRequestNotificationPermission() {
    // Giữ nguyên logic kiểm tra và yêu cầu quyền POST_NOTIFICATIONS
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        Log.d(TAG, "Requesting POST_NOTIFICATIONS permission.");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_POST_NOTIFICATIONS);
      } else {
        Log.d(TAG,"POST_NOTIFICATIONS permission already granted.");
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    // Giữ nguyên logic xử lý kết quả quyền
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Log.d(TAG,"POST_NOTIFICATIONS permission granted by user.");
        Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show();
      } else {
        Log.w(TAG,"POST_NOTIFICATIONS permission denied by user.");
        Toast.makeText(this, "Notifications disabled. Reminders might not work.", Toast.LENGTH_LONG).show();
      }
    }
  }

  // --- (Optional) Hủy bỏ báo thức (Giữ nguyên) ---
  private void cancelAlarm() {
    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    Intent intent = new Intent(this, ReminderBroadcastReceiver.class);
    PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    if (alarmManager != null) {
      alarmManager.cancel(pendingIntent);
      pendingIntent.cancel();
      Log.d(TAG, "Alarm cancelled.");
      // Cập nhật UI nếu cần
      if (!isFinishing() && !isDestroyed()) {
        nextReminderTimestamp = 0;
        updateReminderUI();
      }
    }
  }
}