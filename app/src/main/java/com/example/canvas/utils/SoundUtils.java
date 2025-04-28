package com.example.canvas.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.canvas.R; // Import R của ứng dụng bạn

import java.util.LinkedHashMap;
import java.util.Map;

public class SoundUtils {

    private static final String TAG = "SoundUtils";
    public static final String DEFAULT_REMINDER_SOUND_NAME = "Default";

    // *** NGUỒN DUY NHẤT CHO MAP ÂM THANH ***
    private static final Map<String, Integer> soundMap = new LinkedHashMap<>();
    static {
        // Đảm bảo các R.raw.xyz tồn tại và đúng tên file
        soundMap.put("Default", R.raw.default_sound);
        soundMap.put("Chime", R.raw.chime_sound);
        soundMap.put("Alert", R.raw.alert); // Sửa lại ID nếu tên file khác
        soundMap.put("None", 0); // 0 cho không có âm thanh
    }

    // Hàm tĩnh để lấy Map (SettingsActivity có thể dùng)
    public static Map<String, Integer> getSoundMap() {
        return soundMap;
    }

    // Hàm tĩnh để lấy Uri (Cả SettingsActivity và Receiver đều có thể dùng)
    public static Uri getSoundUri(Context context, String soundName) {
        if (context == null || soundName == null) {
            return null;
        }
        Integer soundResourceId = soundMap.get(soundName);
        Log.d(TAG, "getSoundUri for '" + soundName + "': Resource ID = " + soundResourceId);
        if (soundResourceId != null && soundResourceId != 0) {
            try {
                // Dùng ContentResolver.SCHEME_ANDROID_RESOURCE
                return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + soundResourceId);
            } catch (Exception e) {
                Log.e(TAG, "Error creating sound URI for " + soundName + ", Res ID: " + soundResourceId, e);
                return null; // Trả về null nếu lỗi
            }
        }
        return null; // Trả về null nếu là "None" hoặc không tìm thấy
    }
}