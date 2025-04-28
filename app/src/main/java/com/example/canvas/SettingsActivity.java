package com.example.canvas; // Thay bằng package của bạn

// Import các lớp cần thiết
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
// import android.widget.EditText; // Không cần nữa
import android.widget.FrameLayout;
// import android.widget.RadioButton; // Không cần nữa
// import android.widget.RadioGroup;   // Không cần nữa
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// Import Firebase
import com.example.canvas.utils.SoundUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class SettingsActivity extends NavigationActivity {

  private static final String TAG = "SettingsActivity";

  // --- Views ---
  private TextView tvReminderSoundValue;
  private TextView tvLanguageValue;
  private RelativeLayout layoutReminderSound;
  private RelativeLayout layoutLanguage;

  // --- Firebase ---
  private FirebaseAuth mAuth;
  private FirebaseFirestore db;
  private FirebaseUser currentUser;
  private DocumentReference userDocRef;

  // --- Language Settings (SharedPreferences) ---
  public static final String PREFS_NAME = "SettingsPrefs";
  public static final String PREF_LANGUAGE = "selected_language";
  private final String[] languageCodes = {"en", "vi"};
  private String currentLanguageCode;

  // --- Sound Settings ---
  // Key dùng chung cho SharedPreferences và Firestore (nếu lưu cả 2)
  public static final String PREF_REMINDER_SOUND = "selected_reminder_sound"; // << Giữ 1 khai báo public
  private String currentSelectedSoundName;
  private MediaPlayer mediaPlayer;



  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    loadLocale(); // Load ngôn ngữ (SharedPreferences) trước
    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings); // Đảm bảo tên layout đúng là activity_settings.xml

    setupBottomNavigation();

    mAuth = FirebaseAuth.getInstance();
    db = FirebaseFirestore.getInstance();
    currentUser = mAuth.getCurrentUser();

    if (currentUser == null) {
      showToast("Please login to view settings.");
      finish();
      return;
    }

    String userId = currentUser.getUid();
    userDocRef = db.collection("users").document(userId);



    setupViews();
    setupListeners();
    loadUserSettings(); // Tải cài đặt sound từ Firestore
    updateLanguageValueText(); // Cập nhật text ngôn ngữ từ biến đã load

    Log.d(TAG, "SettingsActivity created. Current language code: " + currentLanguageCode);
  }

  @Override
  protected int getCurrentBottomNavigationItemId() {
    // *** KIỂM TRA LẠI ID TRONG FILE MENU BOTTOM NAVIGATION CỦA BẠN ***
    // Dựa trên file strings.xml bạn cung cấp, ID có thể là nav_settings
    return R.id.navSettingsButton; // <<< Đảm bảo ID này đúng
  }

  // Khởi tạo map âm thanh


  private void setupViews() {
    // Chỉ ánh xạ các View còn lại
    layoutReminderSound = findViewById(R.id.reminderSoundLayout); // ID của RelativeLayout cha
    layoutLanguage = findViewById(R.id.languageLayout);

    tvReminderSoundValue = findViewById(R.id.reminderSoundValue); // TextView giá trị Sound
    tvLanguageValue = findViewById(R.id.languageValue);          // TextView giá trị Language
  }

  private void setupListeners() {
    // Chỉ đặt listener cho các mục còn lại
    layoutReminderSound.setOnClickListener(v -> showSoundSelectionDialog());
    layoutLanguage.setOnClickListener(v -> showLanguageSelectionDialog());
  }

  // Load cài đặt từ Firestore (chỉ sound)
  private void loadUserSettings() {
    userDocRef.get().addOnSuccessListener(documentSnapshot -> {
      if (documentSnapshot.exists()) {
        currentSelectedSoundName = documentSnapshot.getString("reminderSound");
        // Sử dụng default từ SoundUtils
        if (currentSelectedSoundName == null || !SoundUtils.getSoundMap().containsKey(currentSelectedSoundName)) {
          currentSelectedSoundName = SoundUtils.DEFAULT_REMINDER_SOUND_NAME;
        }
        tvReminderSoundValue.setText(currentSelectedSoundName);
      } else {
        displayDefaultSettings();
      }
    }).addOnFailureListener(e -> {
      Log.e(TAG, "Error loading user settings", e);
      showToast("Error loading settings: " + e.getMessage());
      displayDefaultSettings();
    });
  }

  // Hiển thị các giá trị mặc định (sound và language)
  private void displayDefaultSettings() {
    updateLanguageValueText();
    currentSelectedSoundName = SoundUtils.DEFAULT_REMINDER_SOUND_NAME;
    tvReminderSoundValue.setText(currentSelectedSoundName);
  }


  // --- Dialog chọn Âm thanh ---
  private void showSoundSelectionDialog() {
    // Lấy tên sound từ SoundUtils
    final String[] soundNames = SoundUtils.getSoundMap().keySet().toArray(new String[0]);
    int currentSoundIndex = -1;
    // ... (Tìm index như cũ) ...
    for (int i = 0; i < soundNames.length; i++) {
      if (soundNames[i].equals(currentSelectedSoundName)) { currentSoundIndex = i; break;}
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(getString(R.string.reminder_sound));
    final int[] checkedItem = {currentSoundIndex};

    builder.setSingleChoiceItems(soundNames, currentSoundIndex, (dialog, which) -> {
      String selectedName = soundNames[which];
      checkedItem[0] = which;
      playSound(selectedName); // Phát thử
    });

    builder.setPositiveButton(getString(android.R.string.ok), (dialog, which) -> {
      int finalSelectedIndex = checkedItem[0];
      if (finalSelectedIndex != -1) {
        String finalSelectedSoundName = soundNames[finalSelectedIndex];
        if (!finalSelectedSoundName.equals(currentSelectedSoundName)) {
          saveSettingToFirestore("reminderSound", finalSelectedSoundName);
          persistData(this, PREF_REMINDER_SOUND, finalSelectedSoundName); // Lưu vào Prefs
          currentSelectedSoundName = finalSelectedSoundName;
          tvReminderSoundValue.setText(currentSelectedSoundName);
        }
      }
      stopSound();
      dialog.dismiss();
    });
    // ... (Negative button và Dismiss listener như cũ) ...
    builder.setNegativeButton(getString(android.R.string.cancel), (d, w) -> { stopSound(); d.cancel(); });
    builder.setOnDismissListener(d -> stopSound());
    builder.create().show();
  }

  // --- Hàm phát thử âm thanh (Giữ nguyên) ---
  private void playSound(String soundName) {
    stopSound();
    // Gọi hàm tiện ích từ SoundUtils
    Uri soundUri = SoundUtils.getSoundUri(this, soundName);
    Log.d(TAG, "Attempting to play sound: " + soundName + ", URI: " + soundUri);
    if (soundUri != null) { // Chỉ cần kiểm tra Uri khác null
      mediaPlayer = new MediaPlayer();
      try {
        mediaPlayer.setDataSource(this, soundUri);
        mediaPlayer.setOnPreparedListener(mp -> { /* ... */ try { mp.start(); } catch (Exception e) {/*...*/} });
        mediaPlayer.setOnCompletionListener(mp -> stopSound());
        mediaPlayer.setOnErrorListener((mp, w, e) -> { /* ... */ stopSound(); return true; });
        mediaPlayer.prepareAsync();
      } catch (Exception e) {
        Log.e(TAG, "Error setting/preparing MediaPlayer", e);
        showToast("Error loading sound");
        stopSound();
      }
    } else { Log.w(TAG,"Sound URI is null, not playing."); }
  }

  // --- Hàm dừng và giải phóng MediaPlayer (Giữ nguyên) ---
  private void stopSound() {
    if (mediaPlayer != null) {
      try {
        if (mediaPlayer.isPlaying()) { mediaPlayer.stop(); }
        mediaPlayer.release();
        Log.d(TAG,"MediaPlayer stopped and released.");
      } catch (Exception e) { Log.e(TAG,"Error stopping/releasing MP", e); }
      finally { mediaPlayer = null; }
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    stopSound(); // Dừng âm thanh khi activity dừng
  }

  // Hàm Lưu cài đặt vào Firestore (chỉ sound)
  private void saveSettingToFirestore(String fieldName, Object value) {
    Map<String, Object> settingUpdate = new HashMap<>();
    settingUpdate.put(fieldName, value);
    userDocRef.set(settingUpdate, SetOptions.merge()) // Dùng merge
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Firestore setting '" + fieldName + "' updated."))
            .addOnFailureListener(e -> {
              Log.e(TAG, "Error saving Firestore setting '" + fieldName + "'", e);
              showToast("Failed to save setting: " + e.getMessage());
              // Có thể cần reload lại giá trị cũ từ Firestore nếu lưu thất bại
              // loadUserSettings(); // Cân nhắc có nên load lại không
            });
  }

  // --- Các hàm xử lý ngôn ngữ (Giữ nguyên) ---
  private void updateLanguageValueText() {
    if (tvLanguageValue == null || currentLanguageCode == null) return;
    String displayName;
    switch (currentLanguageCode) {
      case "vi": displayName = getString(R.string.vietnamese); break;
      case "en": default: displayName = getString(R.string.english); break;
    }
    tvLanguageValue.setText(displayName);
  }
  private void showLanguageSelectionDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(getString(R.string.language));
    final String[] displayNamesToShow = {getString(R.string.english), getString(R.string.vietnamese)};
    int currentLanguageIndex = -1;
    for (int i = 0; i < languageCodes.length; i++) if (languageCodes[i].equals(currentLanguageCode)) { currentLanguageIndex = i; break; }
    builder.setSingleChoiceItems(displayNamesToShow, currentLanguageIndex, (dialog, which) -> {
      String selectedLanguageCode = languageCodes[which];
      if (!selectedLanguageCode.equals(currentLanguageCode)) {
        setLocale(selectedLanguageCode);
        dialog.dismiss();
        recreate();
        showToast("Language changed");
      } else { dialog.dismiss(); }
    });
    builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());
    builder.create().show();
  }
  private void setLocale(String langCode) {
    if (langCode == null || langCode.isEmpty()) return;
    Log.i(TAG, "Setting locale to: " + langCode);
    persistData(this, PREF_LANGUAGE, langCode);
    Locale locale = new Locale(langCode);
    Locale.setDefault(locale);
    Resources res = getResources();
    Configuration conf = res.getConfiguration();
    DisplayMetrics dm = res.getDisplayMetrics();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) conf.setLocale(locale);
    else conf.locale = locale;
    res.updateConfiguration(conf, dm);
    currentLanguageCode = langCode;
  }
  private void loadLocale() {
    String savedLanguage = getPersistedData(this, PREF_LANGUAGE);
    String targetLanguageCode = null;
    boolean needsUpdate = false;
    if (savedLanguage != null && !savedLanguage.isEmpty()) { targetLanguageCode = savedLanguage;}
    else {
      String deviceLanguage = Locale.getDefault().getLanguage();
      boolean supported = false;
      for (String code : languageCodes) if (code.equals(deviceLanguage)) { supported = true; targetLanguageCode = deviceLanguage; break; }
      if (!supported) targetLanguageCode = "en";
      persistData(this, PREF_LANGUAGE, targetLanguageCode);
    }
    Configuration currentConfig = getResources().getConfiguration();
    Locale currentLocale = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? currentConfig.getLocales().get(0) : currentConfig.locale;
    if (!targetLanguageCode.equals(currentLocale.getLanguage())) {
      needsUpdate = true;
      Log.i(TAG, "Applying locale on load: " + targetLanguageCode);
      Locale locale = new Locale(targetLanguageCode);
      Locale.setDefault(locale);
      Resources res = getResources();
      Configuration conf = new Configuration(currentConfig);
      DisplayMetrics dm = res.getDisplayMetrics();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) conf.setLocale(locale);
      else conf.locale = locale;
      res.updateConfiguration(conf, dm);
    }
    currentLanguageCode = targetLanguageCode;
    Log.d(TAG, "loadLocale finished. Current code: " + currentLanguageCode + ", Applied update: " + needsUpdate);
  }
  public static void persistData(Context context, String key, String value) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    prefs.edit().putString(key, value).apply();
  }
  public static String getPersistedData(Context context, String key) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    return prefs.getString(key, null);
  }

  // --- Tiện ích ---
  private void showToast(String message) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
  }
  // private void setLoadingState(boolean isLoading) { ... }
}