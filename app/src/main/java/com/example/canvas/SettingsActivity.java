package com.example.canvas; // Hoặc package của bạn

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent; // Giữ lại nếu cần cho các Intent khác (không phải nav)
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
// Xóa: import android.widget.Button; // Không cần nút nav nữa
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable; // Thêm cái này cho savedInstanceState
import androidx.appcompat.app.AlertDialog;
// Import lớp cơ sở điều hướng của bạn
// Đảm bảo tên lớp này là chính xác (NavigationActivity hoặc BaseNavigationActivity)
import com.example.canvas.NavigationActivity; // <<< SỬ DỤNG TÊN LỚP CƠ SỞ ĐÚNG

import java.util.Locale;

// 1. Kế thừa từ lớp Activity cơ sở chứa logic điều hướng
public class SettingsActivity extends NavigationActivity { // <<< SỬ DỤNG TÊN LỚP CƠ SỞ ĐÚNG

  // --- Fields từ logic ngôn ngữ ---
  private static final String TAG = "SettingsActivity";
  private static final String PREFS_NAME = "SettingsPrefs";
  private static final String PREF_LANGUAGE = "selected_language";

  private RelativeLayout languageLayout;
  private TextView languageValue;

  private String[] languageDisplayNames;
  private String[] languageCodes = {"en", "vi"}; // Mã ngôn ngữ
  private String currentLanguageCode; // Mã ngôn ngữ đang được sử dụng

  // Xóa: private Button navHomeButton; // Không cần nữa

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    // --- QUAN TRỌNG: Áp dụng ngôn ngữ đã lưu TRƯỚC khi setContentView ---
    loadLocale();
    // ---

    super.onCreate(savedInstanceState); // Gọi super sau loadLocale nhưng trước setContentView

    // --- Đặt layout XML tĩnh cho Activity này ---
    try {
      setContentView(R.layout.settings); // <<< SỬ DỤNG LAYOUT CHO SETTINGS
    } catch (Exception e) {
      Log.e(TAG, "Error setting content view. Check R.layout.settings exists and is valid.", e);
      Toast.makeText(this, "Layout Error (Settings)", Toast.LENGTH_LONG).show();
      finish();
      return;
    }

    // --- Gọi phương thức setup của lớp cơ sở ĐIỀU HƯỚNG SAU setContentView ---
    try {
      setupBottomNavigation();
    } catch (Exception e) {
      Log.e(TAG, "Error setting up bottom navigation in SettingsActivity.", e);
      Toast.makeText(this, "Navigation Error (Settings)", Toast.LENGTH_LONG).show();
    }

    // --- Khởi tạo và xử lý logic cài đặt ngôn ngữ ---
    languageDisplayNames = new String[]{"English", "Tiếng Việt"}; // Có thể lấy từ R.string

    languageLayout = findViewById(R.id.languageLayout);
    languageValue = findViewById(R.id.languageValue);

    // Kiểm tra null cho View trước khi sử dụng
    if (languageLayout == null || languageValue == null) {
      Log.e(TAG, "Error finding language layout views. Check IDs in R.layout.settings.");
      Toast.makeText(this, "UI Error (Settings)", Toast.LENGTH_SHORT).show();
      // Có thể không cần finish(), nhưng chức năng ngôn ngữ sẽ không hoạt động
    } else {
      // currentLanguageCode đã được thiết lập trong loadLocale()
      updateLanguageValueText(); // Cập nhật TextView hiển thị

      languageLayout.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          showLanguageSelectionDialog();
        }
      });
    }

    // --- XÓA LOGIC getIntent và setOnClickListener cho navHomeButton ---
        /*
        Intent intent = getIntent();
        String userId = intent.getStringExtra("userId");
        navHomeButton = findViewById(R.id.navHomeButton);
        navHomeButton.setOnClickListener(v -> { ... });
        */
    // Nếu cần userId, truy cập biến `currentUserId` được kế thừa từ lớp cơ sở

    Log.d(TAG, "SettingsActivity created. Current language code: " + currentLanguageCode);
  }

  // 2. Implement phương thức trừu tượng để BaseNavigationActivity biết mục nào đang active
  @Override
  protected int getCurrentBottomNavigationItemId() {
    // Trả về ID của mục "Settings" trong tệp menu.xml của bạn
    return R.id.navSettingsButton; // <<< Đảm bảo ID này khớp với menu item "Settings"
  }

  // --- Các phương thức xử lý ngôn ngữ (giữ nguyên từ phiên bản đầu) ---

  private void updateLanguageValueText() {
    if (languageValue == null || languageCodes == null || languageDisplayNames == null) return;
    String displayName = "Unknown";
    for (int i = 0; i < languageCodes.length; i++) {
      if (languageCodes[i].equals(currentLanguageCode)) {
        displayName = languageDisplayNames[i];
        break;
      }
    }
    languageValue.setText(displayName);
  }

  private void showLanguageSelectionDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(getString(R.string.language)); // Nên dùng R.string.language

    int currentLanguageIndex = -1;
    for (int i = 0; i < languageCodes.length; i++) {
      if (languageCodes[i].equals(currentLanguageCode)) {
        currentLanguageIndex = i;
        break;
      }
    }

    builder.setSingleChoiceItems(languageDisplayNames, currentLanguageIndex, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        String selectedLanguageCode = languageCodes[which];
        if (!selectedLanguageCode.equals(currentLanguageCode)) {
          setLocale(selectedLanguageCode);
          dialog.dismiss();
          // Khởi động lại Activity để áp dụng ngôn ngữ mới cho UI của nó
          recreate(); // This re-runs onCreate with the new configuration
          Toast.makeText(SettingsActivity.this, "Language changed", Toast.LENGTH_SHORT).show();
        } else {
          dialog.dismiss();
        }
      }
    });

    builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());
    AlertDialog dialog = builder.create();
    dialog.show();
  }

  // Áp dụng Locale mới và lưu vào SharedPreferences
  private void setLocale(String langCode) {
    if (langCode == null || langCode.isEmpty()) {
      Log.w(TAG, "Attempted to set null or empty language code.");
      return;
    }
    Log.i(TAG, "Setting locale to: " + langCode);
    persistData(this, PREF_LANGUAGE, langCode);

    Locale locale = new Locale(langCode);
    Locale.setDefault(locale);

    Resources res = getResources();
    Configuration conf = res.getConfiguration();

    // Nên dùng context của Application để cập nhật nếu có thể,
    // nhưng cập nhật context của Activity cũng hoạt động cho recreate()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      conf.setLocale(locale);
      // createConfigurationContext(conf); // Cập nhật context cho Activity này
    } else {
      conf.locale = locale;
    }
    res.updateConfiguration(conf, res.getDisplayMetrics());

    // Cập nhật biến global sau khi đã áp dụng thành công
    currentLanguageCode = langCode;
  }

  // Load Locale đã lưu khi Activity khởi tạo (gọi trước setContentView)
  private void loadLocale() {
    String language = getPersistedData(this, PREF_LANGUAGE);
    String targetLanguageCode = null;

    if (language != null && !language.isEmpty()) {
      targetLanguageCode = language;
      Log.d(TAG, "Found saved language: " + targetLanguageCode);
    } else {
      // Không có ngôn ngữ đã lưu, dùng default hệ thống (và kiểm tra hỗ trợ)
      String deviceLanguage = Locale.getDefault().getLanguage();
      Log.d(TAG, "No saved language, device default: " + deviceLanguage);
      boolean supported = false;
      for (String code : languageCodes) {
        if (code.equals(deviceLanguage)) {
          supported = true;
          targetLanguageCode = deviceLanguage;
          break;
        }
      }
      if (!supported) {
        targetLanguageCode = "en"; // Fallback về English nếu default không hỗ trợ
        Log.d(TAG, "Device language not supported, falling back to: " + targetLanguageCode);
      }
    }

    // Chỉ gọi setLocale nếu ngôn ngữ mục tiêu khác với ngôn ngữ hiện tại của config
    Configuration currentConfig = getResources().getConfiguration();
    Locale currentLocale;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      currentLocale = currentConfig.getLocales().get(0);
    } else {
      currentLocale = currentConfig.locale;
    }

    if (targetLanguageCode != null && !targetLanguageCode.equals(currentLocale.getLanguage())) {
      Log.i(TAG, "Applying locale on load: " + targetLanguageCode);
      setLocale(targetLanguageCode); // Áp dụng locale
    } else {
      Log.d(TAG, "Locale already set or no change needed on load. Current code: " + targetLanguageCode);
      // Đảm bảo biến global được cập nhật đúng ngay cả khi không gọi setLocale
      currentLanguageCode = targetLanguageCode;
    }

  }

  // ---- Helper functions for SharedPreferences (giữ nguyên) ----
  public static void persistData(Context context, String key, String value) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString(key, value);
    editor.apply();
  }

  public static String getPersistedData(Context context, String key) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    return prefs.getString(key, null); // Trả về null nếu không tìm thấy
  }

  // ---- Ghi chú về attachBaseContext ----
  // Để áp dụng ngôn ngữ ngay lập tức cho TOÀN BỘ ứng dụng và TRƯỚC KHI
  // bất kỳ Activity nào được tạo, cách tốt nhất là override attachBaseContext
  // trong một BaseActivity chung (mà tất cả các Activity khác kế thừa)
  // hoặc trong lớp Application của bạn. Xem comment trong mã gốc của bạn.
  // Phương pháp loadLocale() trong onCreate chỉ đảm bảo Activity *này*
  // được cập nhật đúng khi nó khởi động hoặc được recreate().
}