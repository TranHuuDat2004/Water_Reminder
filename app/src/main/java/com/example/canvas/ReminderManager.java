package com.example.canvas;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

public class ReminderManager {
    private final FirebaseFirestore db;

    public ReminderManager() {
        db = FirebaseFirestore.getInstance();
    }

    public void saveReminder(int hour, int minute, String waterAmount) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> reminder = new HashMap<>();
        reminder.put("hour", hour);
        reminder.put("minute", minute);
        reminder.put("waterAmount", waterAmount);
        reminder.put("userId", userId);

        db.collection("Reminders").add(reminder)
                .addOnSuccessListener(documentReference ->
                        Log.d("Firebase", "Nhắc nhở đã được lưu vào Firestore!")
                )
                .addOnFailureListener(e ->
                        Log.e("Firebase", "Lỗi khi lưu nhắc nhở!", e)
                );
    }

    public void getReminders() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("Reminders")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        int hour = doc.getLong("hour").intValue();
                        int minute = doc.getLong("minute").intValue();
                        String waterAmount = doc.getString("waterAmount");

                        Log.d("Firebase", "Nhắc nhở: " + hour + ":" + minute + " - " + waterAmount + "ml");
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("Firebase", "Lỗi khi lấy dữ liệu!", e)
                );
    }
}
