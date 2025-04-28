// File: StatusActivity.java
package com.example.canvas; // <-- Giữ nguyên package của bạn

// --- THÊM IMPORT NÀY ---
import androidx.annotation.NonNull;
// --- KẾT THÚC THÊM IMPORT ---

import androidx.appcompat.app.AlertDialog;
// import androidx.appcompat.app.AppCompatActivity; // Bạn đang dùng NavigationActivity
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StatusActivity extends NavigationActivity {

  private static final String TAG = "StatusActivity";
  private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1001; // Giữ nguyên giá trị bạn đang dùng
  private static final String PREFS_NAME = "WaterTrackerPrefs";
  private static final String KEY_LAST_GOAL_CHECK_DATE = "lastGoalCheckDate";
  private static final String KEY_GOAL_STATUS_SHOWN = "goalStatusShownToday";

  // Khai báo các View
  private TextView tvWelcomeMessage;
  private TextView tvHydrationProgress;
  private TextView tvWaterPercent;
  private TextView tvNextReminderDisplay;
  private TextView tvLastWaterAmount;
  private ProgressBar progressBarWater;
  private MaterialButton fabAddWater;
  private ProgressBar loadingProgressBar;

  // Khai báo Firebase
  private FirebaseAuth mAuth;
  private FirebaseFirestore db;
  private FirebaseUser currentUser;
  private String userId;
  private String userDisplayName = "User";

  // Biến lưu trữ dữ liệu
  private int currentWaterIntake = 0;
  private int waterGoal = 2100;
  private long nextReminderTimestamp = 0;

  // Biến kiểm tra trạng thái hoàn thành mục tiêu
  private boolean goalStatusShownToday = false;
  private String lastGoalCheckDate = "";

  // Định dạng ngày giờ
  private final SimpleDateFormat firestoreDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
  private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
  private final SimpleDateFormat dateCheckFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.status_activity);

    setupBottomNavigation();

    mAuth = FirebaseAuth.getInstance();
    db = FirebaseFirestore.getInstance();
    currentUser = mAuth.getCurrentUser();

    // Ánh xạ Views
    tvWelcomeMessage = findViewById(R.id.welcomeText);
    tvHydrationProgress = findViewById(R.id.amountText);
    tvWaterPercent = findViewById(R.id.a);
    tvNextReminderDisplay = findViewById(R.id.reminderTimeText);
    tvLastWaterAmount = findViewById(R.id.waterAmountText);
    progressBarWater = findViewById(R.id.progressCircle);
    fabAddWater = findViewById(R.id.addButton);
    loadingProgressBar = findViewById(R.id.loading_progress_bar);

    loadGoalCheckStatus();

    if (currentUser == null) {
      showLoading(false);
      handleUserNotLoggedIn();
    } else {
      userId = currentUser.getUid();
      fabAddWater.setEnabled(true);
      showLoading(true);
      loadUserProfile();
    }

    fabAddWater.setOnClickListener(v -> {
      if (userId == null || userId.isEmpty()) {
        Toast.makeText(this, "Please log in to add water.", Toast.LENGTH_SHORT).show();
        return;
      }
      showAddWaterDialog();
    });

    checkAndRequestNotificationPermission();
  }

  @Override
  protected int getCurrentBottomNavigationItemId() {
    return R.id.navHomeButton;
  }

  private void showLoading(boolean show) {
    if (loadingProgressBar != null) {
      loadingProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    if (fabAddWater != null) {
      fabAddWater.setEnabled(!show);
    }
  }

  private void handleUserNotLoggedIn() {
    userId = null;
    userDisplayName = "User";
    waterGoal = 0;
    currentWaterIntake = 0;
    nextReminderTimestamp = 0;
    updateWelcomeMessage();
    updateHydrationUI();
    updateReminderUI();
    tvLastWaterAmount.setText("0ml");
    fabAddWater.setEnabled(false);
    Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
    Log.e(TAG, "User is not logged in.");
  }

  private void loadUserProfile() {
    if (userId == null || userId.isEmpty()) {
      showLoading(false);
      handleUserNotLoggedIn();
      return;
    }
    Log.d(TAG, "Loading user profile for userId: " + userId);
    DocumentReference userProfileRef = db.collection("users").document(userId);
    userProfileRef.get().addOnSuccessListener(profileSnapshot -> {
      String tempDisplayName = "User"; // Start with default
      int tempWaterGoal = 2100; // Default goal

      if (profileSnapshot.exists()) {
        Log.d(TAG, "User profile loaded successfully.");
        // Lấy waterGoal (sửa lại field name nếu cần, ví dụ intakeGoalM1)
        Number goal = profileSnapshot.get("intakeGoalM1", Number.class); // *** KIỂM TRA FIELD NAME NÀY ***
        if (goal != null && goal.intValue() > 0) {
          tempWaterGoal = goal.intValue();
        } else {
          Log.w(TAG, "'intakeGoalM1' field missing, null, or zero. Using default: " + tempWaterGoal);
        }

        // Lấy tên (Ưu tiên username -> firstName -> Auth displayName -> email)
        String firestoreUsername = profileSnapshot.getString("username");
        String firestoreFirstName = profileSnapshot.getString("firstName"); // *** KIỂM TRA FIELD NAME NÀY ***

        if (firestoreUsername != null && !firestoreUsername.trim().isEmpty()) {
          tempDisplayName = firestoreUsername.trim();
        } else if (firestoreFirstName != null && !firestoreFirstName.trim().isEmpty()) {
          tempDisplayName = firestoreFirstName.trim();
        } else if (currentUser != null && currentUser.getDisplayName() != null && !currentUser.getDisplayName().trim().isEmpty()) {
          tempDisplayName = currentUser.getDisplayName().trim();
        } else if (currentUser != null && currentUser.getEmail() != null) {
          tempDisplayName = currentUser.getEmail();
        }
        // Không cần fallback "User" ở đây nữa vì đã khởi tạo

      } else {
        Log.w(TAG, "User profile document does not exist for userId: " + userId);
        // Fallback tên khi profile không tồn tại
        if (currentUser != null && currentUser.getDisplayName() != null && !currentUser.getDisplayName().trim().isEmpty()) {
          tempDisplayName = currentUser.getDisplayName().trim();
        } else if (currentUser != null && currentUser.getEmail() != null) {
          tempDisplayName = currentUser.getEmail();
        }
      }

      // Gán giá trị cuối cùng
      userDisplayName = tempDisplayName;
      waterGoal = tempWaterGoal;
      Log.d(TAG, "Final userDisplayName: " + userDisplayName + ", Final waterGoal: " + waterGoal);

      updateWelcomeMessage();
      loadDailyWaterData(); // Load dữ liệu nước sau khi có profile

    }).addOnFailureListener(e -> {
      Log.e(TAG, "Error getting user profile for userId: " + userId, e);
      // Sử dụng giá trị mặc định khi lỗi
      waterGoal = 2100;
      userDisplayName = (currentUser != null && currentUser.getDisplayName() != null) ? currentUser.getDisplayName() : "User";
      updateWelcomeMessage();
      loadDailyWaterData(); // Vẫn thử load dữ liệu nước
      Toast.makeText(this, "Failed to load profile. Using defaults.", Toast.LENGTH_SHORT).show();
    });
  }


  private void loadDailyWaterData() {
    if (userId == null || userId.isEmpty()) {
      showLoading(false);
      return;
    }
    String todayDate = firestoreDateFormat.format(new Date());
    Log.d(TAG, "Loading daily water data for " + todayDate);
    DocumentReference dailyLogRef = db.collection("water_tracker").document(userId)
            .collection("daily_logs").document(todayDate);

    dailyLogRef.get().addOnSuccessListener(documentSnapshot -> {
      if (!isFinishing() && !isDestroyed()) {
        if (documentSnapshot.exists()) {
          currentWaterIntake = documentSnapshot.getLong("totalWater") != null ? documentSnapshot.getLong("totalWater").intValue() : 0;
          long lastAmount = documentSnapshot.getLong("lastAddedAmount") != null ? documentSnapshot.getLong("lastAddedAmount") : 0L;
          nextReminderTimestamp = documentSnapshot.getLong("nextReminderTimestamp") != null ? documentSnapshot.getLong("nextReminderTimestamp") : 0L;
          tvLastWaterAmount.setText(lastAmount + "ml");
          Log.d(TAG, "Daily log found. Water: " + currentWaterIntake + "ml");
        } else {
          Log.d(TAG, "No daily log found for today.");
          currentWaterIntake = 0;
          nextReminderTimestamp = 0;
          tvLastWaterAmount.setText("0ml");
        }
        updateHydrationUI();
        updateReminderUI();
        checkGoalCompletionAndNavigate(); // Kiểm tra mục tiêu sau khi load xong
        showLoading(false);
      }
    }).addOnFailureListener(e -> {
      Log.w(TAG, "Error getting daily water log for " + todayDate, e);
      if (!isFinishing() && !isDestroyed()) {
        currentWaterIntake = 0;
        nextReminderTimestamp = 0;
        tvLastWaterAmount.setText("0ml");
        updateHydrationUI();
        updateReminderUI();
        showLoading(false);
        Toast.makeText(this, "Failed to load daily data.", Toast.LENGTH_SHORT).show();
      }
    });
  }

  private void showAddWaterDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    LayoutInflater inflater = this.getLayoutInflater();
    View dialogView = inflater.inflate(R.layout.dialog_add_reminder, null);
    builder.setView(dialogView);

    EditText etWaterAmount = dialogView.findViewById(R.id.waterInput);
    TextView tvSelectedTime = dialogView.findViewById(R.id.timePicker);
    final int[] selectedHour = {-1};
    final int[] selectedMinute = {-1};

    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.HOUR_OF_DAY, 1);
    selectedHour[0] = calendar.get(Calendar.HOUR_OF_DAY);
    selectedMinute[0] = calendar.get(Calendar.MINUTE);
    tvSelectedTime.setText(String.format(Locale.getDefault(), "%02d:%02d", selectedHour[0], selectedMinute[0]));

    tvSelectedTime.setOnClickListener(v -> {
      Calendar now = Calendar.getInstance();
      int initialHour = (selectedHour[0] != -1) ? selectedHour[0] : now.get(Calendar.HOUR_OF_DAY);
      int initialMinute = (selectedMinute[0] != -1) ? selectedMinute[0] : now.get(Calendar.MINUTE);

      TimePickerDialog timePickerDialog = new TimePickerDialog(this,
              (view, hourOfDay, minute) -> {
                selectedHour[0] = hourOfDay;
                selectedMinute[0] = minute;
                tvSelectedTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
              }, initialHour, initialMinute, true);
      timePickerDialog.show();
    });

    builder.setTitle("Add Water & Set Reminder");
    builder.setPositiveButton("Add", (dialog, which) -> {
      String amountStr = etWaterAmount.getText().toString();
      if (!amountStr.isEmpty()) {
        try {
          int amountToAdd = Integer.parseInt(amountStr);
          if (amountToAdd <= 0) {
            Toast.makeText(this, "Please enter a positive amount.", Toast.LENGTH_SHORT).show();
            return;
          }
          if (selectedHour[0] != -1) { // Chỉ cần giờ hợp lệ vì phút luôn có
            showLoading(true);
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
    builder.create().show();
  }


  private void saveWaterRecordAndScheduleReminder(int waterAmount, int reminderHour, int reminderMinute) {
    if (userId == null || userId.isEmpty()) {
      showLoading(false);
      Toast.makeText(this, "Cannot save data. User not identified.", Toast.LENGTH_SHORT).show();
      return;
    }

    int newTotalWater = currentWaterIntake + waterAmount;
    long currentTimeMillis = System.currentTimeMillis();
    long reminderTimestamp = calculateReminderTimestamp(reminderHour, reminderMinute);

    String todayDate = firestoreDateFormat.format(new Date());
    DocumentReference dailyLogRef = db.collection("water_tracker").document(userId)
            .collection("daily_logs").document(todayDate);

    Map<String, Object> waterData = new HashMap<>();
    waterData.put("totalWater", newTotalWater);
    waterData.put("lastAddedAmount", waterAmount);
    waterData.put("lastAddedTimestamp", currentTimeMillis);
    waterData.put("nextReminderTimestamp", reminderTimestamp);
    waterData.put("dailyGoal", waterGoal);

    dailyLogRef.set(waterData, SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
              Log.d(TAG, "Daily water log successfully written/merged for " + todayDate);
              if (!isFinishing() && !isDestroyed()) {
                currentWaterIntake = newTotalWater;
                nextReminderTimestamp = reminderTimestamp;
                tvLastWaterAmount.setText(waterAmount + "ml");
                updateHydrationUI();
                updateReminderUI();
                Toast.makeText(StatusActivity.this, waterAmount + "ml added!", Toast.LENGTH_SHORT).show();

                scheduleNotification(reminderTimestamp); // Đặt lịch sau khi lưu thành công
                checkGoalCompletionAndNavigate(); // Kiểm tra mục tiêu sau khi cập nhật
                showLoading(false);
              }
            })
            .addOnFailureListener(e -> {
              Log.w(TAG, "Error writing/merging daily water log for " + todayDate, e);
              if (!isFinishing() && !isDestroyed()) {
                showLoading(false);
                Toast.makeText(StatusActivity.this, "Failed to save data.", Toast.LENGTH_SHORT).show();
              }
            });
  }

  private long calculateReminderTimestamp(int hour, int minute) {
    Calendar reminderCalendar = Calendar.getInstance();
    reminderCalendar.set(Calendar.HOUR_OF_DAY, hour);
    reminderCalendar.set(Calendar.MINUTE, minute);
    reminderCalendar.set(Calendar.SECOND, 0);
    reminderCalendar.set(Calendar.MILLISECOND, 0);
    if (reminderCalendar.getTimeInMillis() <= System.currentTimeMillis()) {
      reminderCalendar.add(Calendar.DAY_OF_YEAR, 1); // Schedule for tomorrow if time has passed today
    }
    return reminderCalendar.getTimeInMillis();
  }

  private void updateWelcomeMessage() {
    if (tvWelcomeMessage != null) {
      tvWelcomeMessage.setText("Welcome, " + userDisplayName + "! 👋");
    }
  }

  private void updateHydrationUI() {
    if (tvHydrationProgress == null || tvWaterPercent == null || progressBarWater == null) return;

    if (waterGoal <= 0) {
      tvHydrationProgress.setText(String.format(Locale.getDefault(), "%d/--ml", currentWaterIntake));
      tvWaterPercent.setText("0%");
      progressBarWater.setMax(100);
      progressBarWater.setProgress(0);
      return;
    }

    int progressPercent = (int) (((float) currentWaterIntake / waterGoal) * 100);
    progressPercent = Math.min(progressPercent, 100);
    progressPercent = Math.max(progressPercent, 0);

    int progressValue = Math.min(currentWaterIntake, waterGoal);
    progressValue = Math.max(progressValue, 0);

    tvHydrationProgress.setText(String.format(Locale.getDefault(), "%d/%dml", currentWaterIntake, waterGoal));
    tvWaterPercent.setText(String.format(Locale.getDefault(), "%d%%", progressPercent));
    progressBarWater.setMax(waterGoal);
    progressBarWater.setProgress(progressValue);
  }

  private void updateReminderUI() {
    if (tvNextReminderDisplay == null) return;

    if (nextReminderTimestamp > 0 && nextReminderTimestamp > System.currentTimeMillis()) {
      tvNextReminderDisplay.setText(timeFormat.format(new Date(nextReminderTimestamp)));
    } else {
      tvNextReminderDisplay.setText("--:--");
    }
  }

  private void checkGoalCompletionAndNavigate() {
    String todayDate = dateCheckFormat.format(new Date());

    if (!todayDate.equals(lastGoalCheckDate)) {
      Log.i(TAG, "New day (" + todayDate + "). Resetting goal completion status.");
      goalStatusShownToday = false;
      lastGoalCheckDate = todayDate;
      saveGoalCheckStatus();
    }

    Log.d(TAG, "Checking goal: current=" + currentWaterIntake + ", goal=" + waterGoal + ", shownToday=" + goalStatusShownToday);

    if (!goalStatusShownToday && waterGoal > 0 && currentWaterIntake >= waterGoal) {
      Log.i(TAG, "Goal achieved! Navigating to GoalStatusActivity (Complete).");
      navigateToGoalStatus(true);
      goalStatusShownToday = true;
      saveGoalCheckStatus();
    }
  }

  private void navigateToGoalStatus(boolean achieved) {
    if (isFinishing() || isDestroyed()) return;
    Log.d(TAG, "Navigating to GoalStatusActivity, achieved: " + achieved);
    Intent intent = new Intent(StatusActivity.this, GoalStatusActivity.class);
    intent.putExtra(GoalStatusActivity.EXTRA_GOAL_ACHIEVED, achieved);
    intent.putExtra(GoalStatusActivity.EXTRA_USERNAME, userDisplayName);
    startActivity(intent);
  }

  private void saveGoalCheckStatus() {
    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString(KEY_LAST_GOAL_CHECK_DATE, lastGoalCheckDate);
    editor.putBoolean(KEY_GOAL_STATUS_SHOWN, goalStatusShownToday);
    editor.apply();
    Log.d(TAG, "Saved goal check status: date=" + lastGoalCheckDate + ", shown=" + goalStatusShownToday);
  }

  private void loadGoalCheckStatus() {
    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    lastGoalCheckDate = prefs.getString(KEY_LAST_GOAL_CHECK_DATE, "");
    goalStatusShownToday = prefs.getBoolean(KEY_GOAL_STATUS_SHOWN, false);
    Log.d(TAG, "Loaded goal check status: date=" + lastGoalCheckDate + ", shown=" + goalStatusShownToday);

    String todayDate = dateCheckFormat.format(new Date());
    if (!todayDate.equals(lastGoalCheckDate)) {
      if (!lastGoalCheckDate.isEmpty()) {
        Log.i(TAG, "Loaded status is for a previous day ("+lastGoalCheckDate+"). Resetting 'shown' flag for today ("+todayDate+").");
      }
      goalStatusShownToday = false;
      lastGoalCheckDate = todayDate;
      saveGoalCheckStatus();
    }
  }

  private void scheduleNotification(long triggerTimestamp) {
    if (triggerTimestamp <= System.currentTimeMillis()) {
      Log.w(TAG, "Attempted to schedule notification for a past time. Skipping.");
      return;
    }

    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    Intent intent = new Intent(this, ReminderBroadcastReceiver.class);
    int requestCode = 101; // Consistent request code for water reminder
    PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );

    if (alarmManager == null) {
      Log.e(TAG, "AlarmManager is null.");
      Toast.makeText(this, "Cannot access Alarm service.", Toast.LENGTH_SHORT).show();
      return;
    }

    // Check exact alarm permission for Android 12+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      if (!alarmManager.canScheduleExactAlarms()) {
        Log.w(TAG, "Missing SCHEDULE_EXACT_ALARM permission.");
        showExactAlarmPermissionDialog(); // Show dialog to request permission
        return; // Stop here, wait for user interaction
      }
    }

    // Schedule the exact alarm
    try {
      alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimestamp, pendingIntent);
      Log.i(TAG, "Exact alarm scheduled successfully for: " + new Date(triggerTimestamp));
    } catch (SecurityException se) {
      Log.e(TAG, "SecurityException scheduling exact alarm.", se);
      Toast.makeText(this, "Could not schedule reminder due to security restrictions.", Toast.LENGTH_LONG).show();
    } catch (Exception e) {
      Log.e(TAG, "Error scheduling exact alarm.", e);
      Toast.makeText(this, "An error occurred while scheduling the reminder.", Toast.LENGTH_SHORT).show();
    }
  }

  // Helper method to show dialog for exact alarm permission
  private void showExactAlarmPermissionDialog() {
    new AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("To set precise reminders, please allow the app to schedule exact alarms in the system settings.")
            .setPositiveButton("Go to Settings", (dialog, which) -> {
              Intent settingsIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
              try {
                startActivity(settingsIntent);
              } catch (Exception e) {
                Log.e(TAG, "Could not open ACTION_REQUEST_SCHEDULE_EXACT_ALARM settings", e);
                Toast.makeText(StatusActivity.this, "Could not open permission settings.", Toast.LENGTH_SHORT).show();
              }
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
              Toast.makeText(StatusActivity.this, "Reminders may not be exact without permission.", Toast.LENGTH_LONG).show();
              dialog.dismiss();
            })
            .show();
  }


  private void checkAndRequestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
          // Show rationale dialog
          new AlertDialog.Builder(this)
                  .setTitle("Notification Permission Needed")
                  .setMessage("This permission is required to show water reminders.")
                  .setPositiveButton("OK", (dialog, which) -> requestNotificationPermission())
                  .setNegativeButton("Cancel", (dialog, which) -> Toast.makeText(this, "Notifications disabled.", Toast.LENGTH_SHORT).show())
                  .show();
        } else {
          // Request directly
          requestNotificationPermission();
        }
      } else {
        Log.d(TAG,"POST_NOTIFICATIONS permission already granted.");
      }
    }
  }

  private void requestNotificationPermission() {
    ActivityCompat.requestPermissions(this,
            new String[]{Manifest.permission.POST_NOTIFICATIONS},
            REQUEST_CODE_POST_NOTIFICATIONS);
  }

  // --- SỬA LỖI: Thêm @NonNull annotations ---
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Log.i(TAG,"POST_NOTIFICATIONS permission granted.");
        Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show();
      } else {
        Log.w(TAG,"POST_NOTIFICATIONS permission denied.");
        Toast.makeText(this, "Notifications may not work without permission.", Toast.LENGTH_LONG).show();
      }
    }
  }

  private void cancelAlarm() {
    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    Intent intent = new Intent(this, ReminderBroadcastReceiver.class);
    int requestCode = 101; // Use the same request code as when setting the alarm
    PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );

    if (alarmManager != null) {
      try {
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
        Log.i(TAG, "Alarm with requestCode " + requestCode + " cancelled.");
        if (!isFinishing() && !isDestroyed()) {
          nextReminderTimestamp = 0;
          updateReminderUI();
        }
      } catch (Exception e) {
        Log.e(TAG,"Error cancelling alarm", e);
      }
    }
  }
}