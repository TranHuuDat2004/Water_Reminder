package com.example.canvas;



import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger; // Để đếm số lượng tác vụ bất đồng bộ hoàn thành

public class RewardActivity extends AppCompatActivity { // Kế thừa AppCompatActivity

    private static final String TAG = "RewardActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String userId;

    private ProgressBar loadingProgressBar;
    private View contentContainer; // Layout chứa nội dung chính (sẽ là complete hoặc not_complete)

    // Biến lưu dữ liệu lấy từ Firestore
    private int currentWaterIntake = 0;
    private int waterGoal = 2100; // Giá trị mặc định
    private String userDisplayName = "You"; // Tên mặc định

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Không set content view ngay lập tức, sẽ set sau khi fetch dữ liệu

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Tạm thời set một layout loading đơn giản hoặc chỉ hiển thị ProgressBar
        // Cách 1: Dùng layout riêng cho loading
        // setContentView(R.layout.activity_loading);
        // loadingProgressBar = findViewById(R.id.loading_bar);

        // Cách 2: Tạo ProgressBar bằng code (đơn giản hơn nếu không có layout loading)
        loadingProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge);
        setContentView(loadingProgressBar); // Hiển thị ProgressBar toàn màn hình

        if (currentUser == null) {
            Log.e(TAG, "User not logged in. Cannot display rewards.");
            Toast.makeText(this, "Please log in to view rewards.", Toast.LENGTH_LONG).show();
            // Có thể điều hướng về màn hình Login hoặc hiển thị thông báo lỗi
            finish(); // Đóng activity này
            return;
        }

        userId = currentUser.getUid();
        // Bắt đầu fetch dữ liệu
        fetchUserDataAndDisplay();
    }

    private void fetchUserDataAndDisplay() {
        showLoading(true);

        // Cần lấy 2 thông tin: Profile (goal, name) và Daily Log (current intake)
        // Sử dụng AtomicInteger để theo dõi khi cả hai tác vụ hoàn thành
        AtomicInteger tasksCompleted = new AtomicInteger(0);
        final int TOTAL_TASKS = 2;

        // 1. Fetch User Profile
        DocumentReference userProfileRef = db.collection("users").document(userId);
        userProfileRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot profileSnapshot = task.getResult();
                if (profileSnapshot != null && profileSnapshot.exists()) {
                    Log.d(TAG, "User profile fetched successfully.");
                    // Lấy goal (đảm bảo đúng field name, ví dụ 'intakeGoalM1')
                    Number goal = profileSnapshot.get("intakeGoalM1", Number.class);
                    waterGoal = (goal != null && goal.intValue() > 0) ? goal.intValue() : 2100;

                    // Lấy tên (ưu tiên username -> firstName -> Auth display name -> email)
                    String firestoreUsername = profileSnapshot.getString("username");
                    String firestoreFirstName = profileSnapshot.getString("firstName");

                    if (firestoreUsername != null && !firestoreUsername.trim().isEmpty()) {
                        userDisplayName = firestoreUsername.trim();
                    } else if (firestoreFirstName != null && !firestoreFirstName.trim().isEmpty()) {
                        userDisplayName = firestoreFirstName.trim();
                    } else if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().trim().isEmpty()) {
                        userDisplayName = currentUser.getDisplayName().trim();
                    } else if (currentUser.getEmail() != null) {
                        userDisplayName = currentUser.getEmail();
                    }
                } else {
                    Log.w(TAG, "User profile document does not exist.");
                    // Sử dụng giá trị mặc định đã có (goal=2100, name="You")
                }
            } else {
                Log.e(TAG, "Error fetching user profile", task.getException());
                // Sử dụng giá trị mặc định khi lỗi
            }
            // Đánh dấu task này hoàn thành và kiểm tra
            if (tasksCompleted.incrementAndGet() == TOTAL_TASKS) {
                determineLayoutAndPopulate();
            }
        });

        // 2. Fetch Daily Log for Today
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        DocumentReference dailyLogRef = db.collection("water_tracker").document(userId)
                .collection("daily_logs").document(todayDate);

        dailyLogRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot logSnapshot = task.getResult();
                if (logSnapshot != null && logSnapshot.exists()) {
                    Log.d(TAG, "Daily log fetched successfully for " + todayDate);
                    currentWaterIntake = logSnapshot.getLong("totalWater") != null ? logSnapshot.getLong("totalWater").intValue() : 0;
                } else {
                    Log.d(TAG, "No daily log found for " + todayDate);
                    currentWaterIntake = 0; // Chưa uống gì hôm nay
                }
            } else {
                Log.e(TAG, "Error fetching daily log for " + todayDate, task.getException());
                currentWaterIntake = 0; // Giả định là 0 khi lỗi
            }
            // Đánh dấu task này hoàn thành và kiểm tra
            if (tasksCompleted.incrementAndGet() == TOTAL_TASKS) {
                determineLayoutAndPopulate();
            }
        });
    }

    private void determineLayoutAndPopulate() {
        Log.d(TAG, "Determining layout: Intake=" + currentWaterIntake + ", Goal=" + waterGoal);
        boolean goalAchieved = (waterGoal > 0 && currentWaterIntake >= waterGoal);

        // Set layout phù hợp
        if (goalAchieved) {
            setContentView(R.layout.complete);
            populateCompleteLayout();
        } else {
            setContentView(R.layout.not_complete);
            populateNotCompleteLayout();
        }
        showLoading(false); // Ẩn ProgressBar sau khi set layout
    }

    private void populateCompleteLayout() {
        // Ánh xạ View từ complete.xml
        TextView tvTodayDate = findViewById(R.id.today_date);
        TextView tvGreeting = findViewById(R.id.greeting);
        TextView tvCongratsMessage = findViewById(R.id.congratulations_message);
        Button btnShare = findViewById(R.id.buttonShare);
        ImageView emoji = findViewById(R.id.emoji);


        // *** THÊM PHẦN NÀY: Ánh xạ và xử lý nút Back ***
        ImageButton backButton = findViewById(R.id.backButtonComplete);
        backButton.setOnClickListener(v -> {
            Log.d(TAG, "Back button on complete screen clicked.");
            // Đóng Activity hiện tại để quay lại màn hình trước đó
            finish();
        });
        // Cập nhật nội dung
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault());
        String currentDate = sdf.format(new Date());
        tvTodayDate.setText("Today - " + currentDate);

        tvGreeting.setText("Hi, " + userDisplayName + "!");
        tvCongratsMessage.setText(userDisplayName + " has achieved\nyour goal today");
        emoji.setImageResource(R.drawable.ic_happy); // Đảm bảo icon tồn tại

        // Xử lý nút Share
        btnShare.setOnClickListener(v -> shareAchievement());
    }

    private void populateNotCompleteLayout() {
        // Ánh xạ View từ not_complete.xml
        TextView tvTodayDate = findViewById(R.id.today_date);
        TextView tvGreeting = findViewById(R.id.greeting);
        TextView tvOopsMessage = findViewById(R.id.congratulations_message);
        Button btnGoHome = findViewById(R.id.buttonGoToHome);
        ImageView emoji = findViewById(R.id.emoji);

        // Cập nhật nội dung
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault());
        String currentDate = sdf.format(new Date());
        tvTodayDate.setText("Today - " + currentDate);

        tvGreeting.setText("Hi, " + userDisplayName + "!");
        tvOopsMessage.setText(userDisplayName + " has not achieved\nyour goal today");
        emoji.setImageResource(R.drawable.ic_sad); // Đảm bảo icon tồn tại

        // Xử lý nút Go To Home
        btnGoHome.setOnClickListener(v -> {
            // Đóng Activity này để quay lại màn hình trước đó
            finish();
        });
    }

    private void showLoading(boolean show) {
        // Nếu dùng ProgressBar code-generated
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            // Nếu đang ẩn loading và đã set layout nội dung, không làm gì thêm
            // Nếu đang hiện loading, đảm bảo content view là ProgressBar
            if(show) setContentView(loadingProgressBar);
        }

        // Nếu dùng ProgressBar trong layout loading riêng
        /*
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (contentContainer != null) { // Ẩn/hiện container nội dung chính nếu có
             contentContainer.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        */
    }

    // --- Hàm xử lý Share (Giống trong GoalStatusActivity) ---
    private void shareAchievement() {
        String shareText = "🎉 Hooray! " + userDisplayName + " just reached the daily water intake goal! Staying hydrated. 💧 #WaterGoalAchieved #HydrationHero";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, userDisplayName + "'s Hydration Goal Reached!");

        Intent chooserIntent = Intent.createChooser(shareIntent, "Share Achievement via");

        if (chooserIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(chooserIntent);
        } else {
            Log.e(TAG, "No app available to handle share intent.");
            Toast.makeText(this, "No suitable app found for sharing.", Toast.LENGTH_SHORT).show();
        }
    }
}
