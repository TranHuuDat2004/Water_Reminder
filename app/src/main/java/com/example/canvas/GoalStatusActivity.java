package com.example.canvas;




import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GoalStatusActivity extends AppCompatActivity {

    private static final String TAG = "GoalStatusActivity";
    public static final String EXTRA_GOAL_ACHIEVED = "GOAL_ACHIEVED";
    public static final String EXTRA_USERNAME = "USERNAME";

    private String currentUsername = "You"; // Tên mặc định

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        boolean goalAchieved = intent.getBooleanExtra(EXTRA_GOAL_ACHIEVED, false);
        String usernameFromIntent = intent.getStringExtra(EXTRA_USERNAME);
        if (usernameFromIntent != null && !usernameFromIntent.trim().isEmpty()) {
            currentUsername = usernameFromIntent.trim();
        }

        Log.d(TAG, "Received goalAchieved: " + goalAchieved + ", username: " + currentUsername);

        // Định dạng ngày thân thiện hơn
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault());
        String currentDate = sdf.format(new Date());
        String todayText = "Today - " + currentDate; // Đã sửa format

        if (goalAchieved) {
            // --- HIỂN THỊ LAYOUT COMPLETE ---
            setContentView(R.layout.complete); // Sử dụng layout complete.xml

            // Ánh xạ View từ complete.xml
            TextView tvTodayDate = findViewById(R.id.today_date);
            TextView tvGreeting = findViewById(R.id.greeting);
            TextView tvCongratsMessage = findViewById(R.id.congratulations_message);
            Button btnShare = findViewById(R.id.buttonShare);
            ImageView emoji = findViewById(R.id.emoji);

            // Cập nhật nội dung
            tvTodayDate.setText(todayText);
            tvGreeting.setText("Hi, " + currentUsername + "!");
            // Sửa lại text message để phù hợp với cấu trúc câu
            tvCongratsMessage.setText(currentUsername + " has achieved\nyour goal today");
            // Đảm bảo bạn có drawable tên là 'ic_happy'
            emoji.setImageResource(R.drawable.ic_happy);

            // Xử lý nút Share
            btnShare.setOnClickListener(v -> {
                Log.d(TAG, "Share button clicked.");
                shareAchievement();
            });

        } else {
            // --- HIỂN THỊ LAYOUT NOT_COMPLETE ---
            setContentView(R.layout.not_complete); // Sử dụng layout not_complete.xml

            // Ánh xạ View từ not_complete.xml
            TextView tvTodayDate = findViewById(R.id.today_date);
            TextView tvGreeting = findViewById(R.id.greeting);
            TextView tvOopsMessage = findViewById(R.id.congratulations_message);
            Button btnGoHome = findViewById(R.id.buttonGoToHome);
            ImageView emoji = findViewById(R.id.emoji);

            // Cập nhật nội dung
            tvTodayDate.setText(todayText);
            tvGreeting.setText("Hi, " + currentUsername + "!");
            tvOopsMessage.setText(currentUsername + " has not achieved\nyour goal today");
            // Đảm bảo bạn có drawable tên là 'ic_sad'
            emoji.setImageResource(R.drawable.ic_sad);


            // Xử lý nút Go To Home
            btnGoHome.setOnClickListener(v -> {
                Log.d(TAG, "Go to Home button clicked.");
                // Đóng Activity này sẽ tự động quay lại màn hình trước đó (StatusActivity)
                finish();
            });
        }
    }

    // --- Hàm xử lý Share ---
    private void shareAchievement() {
        String shareText = "🎉 Hooray! " + currentUsername + " just reached the daily water intake goal! Staying hydrated. 💧 #WaterGoalAchieved #HydrationHero";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentUsername + "'s Hydration Goal Reached!");

        // Tạo chooser để người dùng chọn ứng dụng chia sẻ
        Intent chooserIntent = Intent.createChooser(shareIntent, "Share Achievement via");

        // Kiểm tra xem có ứng dụng nào xử lý được Intent này không
        if (chooserIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(chooserIntent);
        } else {
            Log.e(TAG, "No app available to handle share intent.");
            Toast.makeText(this, "No suitable app found for sharing.", Toast.LENGTH_SHORT).show();
        }
    }
}
