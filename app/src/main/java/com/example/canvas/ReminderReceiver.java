package com.example.canvas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationHelper.showNotification(context, "Nhắc nhở uống nước!", "Đã đến giờ uống nước, đừng quên nhé!");
    }
}
