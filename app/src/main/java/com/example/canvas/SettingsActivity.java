package com.example.canvas; // Thay đổi package name nếu cần

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast; // Thêm Toast để thông báo

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

  private static final String TAG = "SettingsActivity";
  private static final String PREFS_NAME = "SettingsPrefs"; // Tên file SharedPreferences
  private static final String PREF_LANGUAGE = "selected_language"; // Key lưu ngôn ngữ

  private RelativeLayout languageLayout;
  private TextView languageValue;

  // Mảng chứa tên ngôn ngữ hiển thị trong Dialog
  private String[] languageDisplayNames;
  // Mảng chứa mã ngôn ngữ tương ứng (quan trọng để set Locale)
  private String[] languageCodes = {"en", "vi"}; // "en" cho English, "vi" cho Tiếng Việt
  private String currentLanguageCode;
  private Button navHomeButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // --- QUAN TRỌNG: Áp dụng ngôn ngữ đã lưu TRƯỚC khi setContentView ---
    // Điều này đảm bảo Activity này cũng hiển thị đúng ngôn ngữ ngay từ đầu
    loadLocale();
    // ---

    setContentView(R.layout.settings);

    // Khởi tạo mảng tên ngôn ngữ (nên lấy từ resources để hỗ trợ đa ngôn ngữ tốt hơn)
    // Ví dụ đơn giản:
    languageDisplayNames = new String[]{"English", "Tiếng Việt"};

    languageLayout = findViewById(R.id.languageLayout);
    languageValue = findViewById(R.id.languageValue);

    // Lấy ngôn ngữ hiện tại (đã được load bởi loadLocale())
    currentLanguageCode = getPersistedData(this, PREF_LANGUAGE);
    if (currentLanguageCode == null || currentLanguageCode.isEmpty()) {
      // Nếu chưa có, lấy ngôn ngữ mặc định của hệ thống
      currentLanguageCode = Locale.getDefault().getLanguage();
      // Giới hạn trong các ngôn ngữ hỗ trợ (ví dụ chỉ en/vi)
      boolean supported = false;
      for (String code : languageCodes) {
        if (code.equals(currentLanguageCode)) {
          supported = true;
          break;
        }
      }
      if (!supported) {
        currentLanguageCode = "en"; // Mặc định là English nếu ngôn ngữ hệ thống không được hỗ trợ
      }
    }


    updateLanguageValueText(); // Cập nhật TextView hiển thị

    languageLayout.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        showLanguageSelectionDialog();
      }
    });

    // (Thêm các findViewById và listener khác cho các mục cài đặt còn lại nếu cần)

    // 1. Lấy Intent đã khởi động Activity này
    Intent intent = getIntent();

    // 2. Truy xuất String extra bằng ĐÚNG key "userId"
    String userId = intent.getStringExtra("userId"); // Key phải khớp với key khi gửi
    
    //Example
    navHomeButton = findViewById(R.id.navHomeButton);

    // You can now use the userId in this activity
    // Example: Log the user ID
    //Log.d("Started1Activity", "User ID: " + userId);

    navHomeButton.setOnClickListener(v -> {
      Intent intentA = new Intent(SettingsActivity.this, StatusActivity.class);
      intentA.putExtra("userId", userId);
      startActivity(intentA);
      finish();
    });
  }

  private void updateLanguageValueText() {
    // Tìm tên hiển thị tương ứng với mã ngôn ngữ hiện tại
    String displayName = "Unknown"; // Giá trị mặc định nếu không tìm thấy
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
    builder.setTitle(getString(R.string.language)); // Lấy tiêu đề từ resources

    // Tìm index của ngôn ngữ hiện tại để check radio button
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
        // Lấy mã ngôn ngữ mới được chọn
        String selectedLanguageCode = languageCodes[which];

        // Chỉ thực hiện nếu ngôn ngữ thay đổi
        if (!selectedLanguageCode.equals(currentLanguageCode)) {
          setLocale(selectedLanguageCode);
          // Đóng dialog
          dialog.dismiss();
          // Khởi động lại Activity để áp dụng thay đổi ngôn ngữ
          recreate();
          Toast.makeText(SettingsActivity.this, "Language changed", Toast.LENGTH_SHORT).show(); // Thông báo ngắn
        } else {
          // Nếu chọn lại ngôn ngữ hiện tại thì thôi
          dialog.dismiss();
        }
      }
    });

    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
      }
    });

    AlertDialog dialog = builder.create();
    dialog.show();
  }

  // Hàm áp dụng Locale mới
  private void setLocale(String langCode) {
    if (langCode == null || langCode.isEmpty()) {
      Log.e(TAG, "Language code is null or empty. Cannot set locale.");
      return;
    }
    Log.d(TAG, "Setting locale to: " + langCode);
    // Lưu ngôn ngữ mới vào SharedPreferences
    persistData(this, PREF_LANGUAGE, langCode);

    // Tạo đối tượng Locale
    Locale locale = new Locale(langCode);
    Locale.setDefault(locale); // Set locale mặc định cho JVM

    // Cập nhật configuration cho application context
    Resources res = getResources();
    DisplayMetrics dm = res.getDisplayMetrics();
    Configuration conf = res.getConfiguration();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      conf.setLocale(locale);
    } else {
      conf.locale = locale; // API < 24
    }
    // Cần cập nhật lại configuration của Resources
    res.updateConfiguration(conf, dm);

    // Cập nhật biến global
    currentLanguageCode = langCode;

    Log.d(TAG, "Locale updated successfully.");

    // Lưu ý: Việc gọi recreate() trong showLanguageSelectionDialog sẽ load lại UI
    // với configuration mới này.
  }

  // Hàm load Locale khi Activity khởi tạo
  private void loadLocale() {
    String language = getPersistedData(this, PREF_LANGUAGE);
    // Chỉ set nếu đã có ngôn ngữ được lưu và khác với ngôn ngữ hiện tại của configuration
    // (Tránh việc set lại không cần thiết)
    if (language != null && !language.isEmpty()) {
      // Lấy config hiện tại để so sánh
      Configuration currentConfig = getResources().getConfiguration();
      Locale currentLocale;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        currentLocale = currentConfig.getLocales().get(0);
      } else {
        currentLocale = currentConfig.locale;
      }

      // Chỉ set lại nếu khác
      if (!language.equals(currentLocale.getLanguage())) {
        Log.d(TAG, "Loading saved locale: " + language);
        setLocale(language); // Gọi setLocale để áp dụng cho Context này trước khi UI được tạo
      } else {
        Log.d(TAG, "Saved locale (" + language + ") matches current config locale. No change needed on load.");
        // Cập nhật biến global phòng trường hợp chạy lần đầu
        currentLanguageCode = language;
      }
    } else {
      Log.d(TAG, "No saved language found. Using default.");
      // Nếu không có gì được lưu, dùng locale mặc định, không cần làm gì thêm ở đây
      // vì hệ thống đã làm rồi. Chỉ cần cập nhật biến currentLanguageCode
      currentLanguageCode = Locale.getDefault().getLanguage();
      // Giới hạn trong các ngôn ngữ hỗ trợ (ví dụ chỉ en/vi)
      boolean supported = false;
      for (String code : languageCodes) {
        if (code.equals(currentLanguageCode)) {
          supported = true;
          break;
        }
      }
      if (!supported) {
        currentLanguageCode = "en"; // Mặc định là English
      }
    }

  }

  // ---- Helper functions for SharedPreferences ----
  public static void persistData(Context context, String key, String value) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString(key, value);
    editor.apply(); // Dùng apply() thay vì commit() để chạy bất đồng bộ
  }


  public static String getPersistedData(Context context, String key) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    // Trả về null nếu không tìm thấy key
    return prefs.getString(key, null);
  }
  // ---------------------------------------------

  // ---- QUAN TRỌNG: Để đảm bảo ngôn ngữ áp dụng cho toàn bộ app ----
  // ---- ngay cả trước khi Activity được tạo, bạn nên override ----
  // ---- attachBaseContext trong một BaseActivity hoặc Application class ----

     /* Ví dụ trong một BaseActivity mà tất cả các Activity khác kế thừa:

     @Override
     protected void attachBaseContext(Context newBase) {
         String langCode = getPersistedData(newBase, PREF_LANGUAGE);
         if (langCode == null || langCode.isEmpty()) {
              // Lấy default hoặc set một ngôn ngữ mặc định ("en")
              langCode = Locale.getDefault().getLanguage();
              // Optionally validate against supported codes like above
         }
         super.attachBaseContext(LocaleHelper.setLocale(newBase, langCode)); // LocaleHelper là class tự tạo
     }

     // LocaleHelper.java (ví dụ)
     public class LocaleHelper {
         public static Context setLocale(Context context, String language) {
             Locale locale = new Locale(language);
             Locale.setDefault(locale);

             Resources resources = context.getResources();
             Configuration config = resources.getConfiguration();

             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                 config.setLocale(locale);
                 return context.createConfigurationContext(config);
             } else {
                 config.locale = locale;
                 resources.updateConfiguration(config, resources.getDisplayMetrics());
                 return context; // Trên API cũ, context tự cập nhật
             }
         }
     }
     */
  // ---- Nếu không dùng BaseActivity, cách trên là đủ để ----
  // ---- SettingsActivity tự cập nhật ----

}