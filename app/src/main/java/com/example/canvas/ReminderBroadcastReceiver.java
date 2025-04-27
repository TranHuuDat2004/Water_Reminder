package com.example.canvas;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class ReminderBroadcastReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "WATER_REMINDER_CHANNEL";
    private static final int NOTIFICATION_ID = 101;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Tạo Notification Channel (cần thiết cho Android 8.0 Oreo trở lên)
        createNotificationChannel(context);

        // Tạo Intent để mở lại ứng dụng khi nhấn vào thông báo
        Intent mainIntent = new Intent(context, StatusActivity.class); // Hoặc Activity bạn muốn mở
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


        // Xây dựng thông báo
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_water_drop) // **QUAN TRỌNG:** Thay bằng icon của bạn
                .setContentTitle("Time to Hydrate!")
                .setContentText("Don't forget to drink some water, Karol!")
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Ưu tiên cao để hiển thị head-up
                .setContentIntent(pendingIntent) // Intent khi nhấn vào thông báo
                .setAutoCancel(true); // Tự động xóa thông báo khi nhấn

        // Hiển thị thông báo
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        // Kiểm tra quyền POST_NOTIFICATIONS cho Android 13+ (bạn cần yêu cầu quyền này trong Activity)
        // Ở đây chỉ là ví dụ hiển thị, việc kiểm tra quyền nên làm ở Activity trước khi đặt lịch
        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            // Xử lý trường hợp không có quyền gửi thông báo (Android 13+)
            e.printStackTrace();
            // Có thể hiển thị một thông báo lỗi cho người dùng hoặc ghi log
        }
    }

    // Phương thức tạo Notification Channel
    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Water Reminder Channel";
            String description = "Channel for water drinking reminders";
            int importance = NotificationManager.IMPORTANCE_HIGH; // Quan trọng cao
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
