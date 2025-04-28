package com.example.canvas.utils; // Hoặc package tiện ích của bạn

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.util.Log;

import com.example.canvas.SettingsActivity; // Import để dùng hằng số

import java.util.Locale;

public class LocaleHelper {

    private static final String TAG = "LocaleHelper";

    // Sử dụng hằng số từ SettingsActivity để đảm bảo nhất quán
    private static final String SELECTED_LANGUAGE = SettingsActivity.PREF_LANGUAGE;
    private static final String PREFS_NAME = SettingsActivity.PREFS_NAME;

    // Phương thức chính được gọi từ BaseActivity
    public static Context onAttach(Context context) {
        String lang = getPersistedData(context, SELECTED_LANGUAGE);
        // Nếu chưa lưu gì, lấy ngôn ngữ mặc định của thiết bị hoặc fallback về 'en'
        if (lang == null) {
            lang = getDefaultLanguage();
            persistData(context, SELECTED_LANGUAGE, lang); // Lưu lại ngôn ngữ mặc định đã chọn
        }
        Log.d(TAG, "Attaching context with language: " + lang);
        return setLocale(context, lang);
    }

    // Lấy ngôn ngữ đã lưu
    public static String getLanguage(Context context) {
        String lang = getPersistedData(context, SELECTED_LANGUAGE);
        return (lang != null) ? lang : getDefaultLanguage();
    }

    // Áp dụng Locale vào Context mới
    public static Context setLocale(Context context, String languageCode) {
        Log.d(TAG, "Setting locale to: " + languageCode);
        persistData(context, SELECTED_LANGUAGE, languageCode); // Lưu lại lựa chọn mới
        return updateResources(context, languageCode);
    }

    // Lấy ngôn ngữ mặc định (ưu tiên 'vi', sau đó 'en')
    private static String getDefaultLanguage() {
        String deviceLanguage = Locale.getDefault().getLanguage();
        if ("vi".equals(deviceLanguage)) {
            return "vi";
        } else {
            return "en"; // Mặc định là tiếng Anh
        }
    }

    // Lấy dữ liệu từ SharedPreferences (giống trong SettingsActivity)
    private static String getPersistedData(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(key, null);
    }

    // Lưu dữ liệu vào SharedPreferences (giống trong SettingsActivity)
    private static void persistData(Context context, String key, String value) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(key, value).apply();
    }

    // Cập nhật tài nguyên của Context với ngôn ngữ mới
    private static Context updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // API 24+
            LocaleList localeList = new LocaleList(locale);
            LocaleList.setDefault(localeList);
            config.setLocales(localeList);
            config.setLocale(locale); // Đảm bảo setLocale cũng được gọi
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // API 17 - 23
            config.setLocale(locale);
        } else {
            // API < 17
            config.locale = locale;
        }

        // API 17+ có thể tạo context mới với configuration đã cập nhật
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return context.createConfigurationContext(config);
        } else {
            // API < 17 cập nhật trực tiếp resources
            res.updateConfiguration(config, res.getDisplayMetrics());
            return context; // Trả về context gốc sau khi đã cập nhật resources
        }
    }
}