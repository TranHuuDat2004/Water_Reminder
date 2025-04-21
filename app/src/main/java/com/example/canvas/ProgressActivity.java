//package com.example.canvas; // Giữ nguyên package của bạn
//
//// import androidx.appcompat.app.AppCompatActivity; // Không cần nữa
//import androidx.cardview.widget.CardView;
//
//        import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.view.ViewGroup; // Để thay đổi chiều cao của View (bar)
//import android.widget.Button;
//import android.widget.FrameLayout; // ID: circularProgressBar
//        import android.widget.LinearLayout; // Container của các bar
//import android.widget.TextView;
//import android.widget.Toast;
//
//        import com.example.canvas.models.User; // Model User
//import com.example.canvas.models.WaterLog; // Giả sử có model cho log nước hàng ngày/tuần
//        import com.google.android.gms.tasks.Task;
//import com.google.android.gms.tasks.Tasks; // Để thực hiện nhiều tác vụ song song
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.firestore.CollectionReference;
//import com.google.firebase.firestore.DocumentReference;
//import com.google.firebase.firestore.DocumentSnapshot;
//import com.google.firebase.firestore.FirebaseFirestore;
//        import com.google.firebase.firestore.Query;
//import com.google.firebase.firestore.QuerySnapshot;
//
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Calendar;
//        import java.util.List;
//import java.util.Locale;
//
//public class ProgressActivity extends NavigationActivity {
//
//    private static final String TAG = "ProgressActivity";
//
//    // --- Views từ XML ---
//    // Date Selector
//    private Button buttonPrev, buttonNext;
//    private TextView dateRangeText;
//
//    // Bar Chart
//    private LinearLayout barChartContainer; // LinearLayout chứa các cột
//    private TextView valueMon, valueTue, valueWed, valueThu, valueFri, valueSat, valueSun;
//    private View barMon, barTue, barWed, barThu, barFri, barSat, barSun;
//    private TextView[] valueTextViews; // Mảng để dễ quản lý
//    private View[] barViews;           // Mảng để dễ quản lý
//
//    // Goal Preview
//    private TextView goalPreviewTitle;
//    private FrameLayout circularProgressBar; // Placeholder hoặc container cho progress bar thật
//    private TextView progressText; // Text "%" trong hình tròn
//    private CardView athletePerformanceCard, cognitivePerformanceCard;
//    // Giả sử bạn thêm ID cho các TextView percentage trong CardView
//    private TextView athletePercentageText;
//    private TextView cognitivePercentageText;
//    // Có thể lấy thêm các TextView khác nếu cần cập nhật
//
//    // --- Firebase ---
//    private FirebaseFirestore db;
//    private FirebaseAuth mAuth;
//    private FirebaseUser currentUser;
//
//    // --- Quản lý ngày tháng ---
//    private Calendar currentWeekStart; // Lưu ngày bắt đầu của tuần hiện tại đang hiển thị
//    private SimpleDateFormat dateFormat; // Để định dạng ngày hiển thị
//
//    // --- Hằng số (Ví dụ) ---
//    private static final double MAX_BAR_HEIGHT_DP = 160.0; // Chiều cao tối đa của cột (dp) khớp với cột Sun trong XML
//    private static final double DAILY_WATER_GOAL_LITERS = 2.0; // Mục tiêu nước hàng ngày (ví dụ)
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.progress); // Đặt layout cho Activity này
//
//        // Gọi setup của BaseNavigationActivity SAU setContentView
//        setupBottomNavigation();
//
//        // Khởi tạo Firebase
//        mAuth = FirebaseAuth.getInstance();
//        db = FirebaseFirestore.getInstance();
//        currentUser = mAuth.getCurrentUser();
//
//        // Khởi tạo Calendar và DateFormat
//        currentWeekStart = Calendar.getInstance();
//        setCalendarToStartOfWeek(currentWeekStart); // Đặt về đầu tuần hiện tại
//        dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault()); // Format cho dateRangeText
//
//        // Tìm và ánh xạ Views
//        setupViews();
//
//        // Thiết lập Listeners cho nút
//        setupListeners();
//
//        // Tải dữ liệu lần đầu cho tuần hiện tại
//        if (currentUser != null) {
//            updateDateRangeText(); // Hiển thị ngày tháng ban đầu
//            loadDataForCurrentWeek(); // Tải dữ liệu từ Firestore
//        } else {
//            showError("Vui lòng đăng nhập để xem tiến độ.");
//            // Có thể chuyển hướng đến màn hình đăng nhập
//            // hoặc hiển thị trạng thái mặc định/lỗi
//            clearUI(); // Xóa dữ liệu cũ hoặc hiển thị trạng thái mặc định
//        }
//    }
//
//    // Implement phương thức trừu tượng từ BaseNavigationActivity
//    @Override
//    protected int getCurrentBottomNavigationItemId() {
//        return R.id.navProgressButton; // ID của mục "Progress" trong menu.xml
//    }
//
//    // --- Ánh xạ Views từ Layout ---
//    private void setupViews() {
//        // Date Selector
//        buttonPrev = findViewById(R.id.buttonPrev);
//        buttonNext = findViewById(R.id.buttonNext);
//        dateRangeText = findViewById(R.id.dateRangeText);
//
//        // Bar Chart Container
//        barChartContainer = findViewById(R.id.barChartContainer);
//
//        // Bar Chart Views - Ánh xạ thủ công vào mảng
//        valueTextViews = new TextView[]{
//                findViewById(R.id.valueMon), findViewById(R.id.valueTue),
//                findViewById(R.id.valueWed), findViewById(R.id.valueThu),
//                findViewById(R.id.valueFri), findViewById(R.id.valueSat),
//                findViewById(R.id.valueSun)
//        };
//        barViews = new View[]{
//                findViewById(R.id.barMon), findViewById(R.id.barTue),
//                findViewById(R.id.barWed), findViewById(R.id.barThu),
//                findViewById(R.id.barFri), findViewById(R.id.barSat),
//                findViewById(R.id.barSun)
//        };
//
//        // Goal Preview
//        goalPreviewTitle = findViewById(R.id.goalPreviewTitle);
//        circularProgressBar = findViewById(R.id.cognitive_percentage_text);
//        progressText = findViewById(R.id.a);
//        athletePerformanceCard = findViewById(R.id.athletePerformanceCard);
//        cognitivePerformanceCard = findViewById(R.id.cognitivePerformanceCard);
//
//        // Lấy TextViews bên trong CardViews (*** YÊU CẦU THÊM ID VÀO XML ***)
//        // Ví dụ: Thêm android:id="@+id/athlete_percentage_text" vào TextView "+100%" trong card Athlete
//        athletePercentageText = findViewById(R.id.athlete_percentage_text); // Thay ID nếu khác
//        cognitivePercentageText = findViewById(R.id.cognitive_percentage_text); // Thay ID nếu khác
//
//        if (athletePercentageText == null || cognitivePercentageText == null) {
//            Log.w(TAG, "Percentage TextViews inside CardViews not found. Did you add IDs (e.g., athlete_percentage_text)?");
//            // Có thể hiển thị lỗi hoặc dùng cách khác để cập nhật text nếu không muốn thêm ID
//        }
//    }
//
//    // --- Thiết lập Listeners ---
//    private void setupListeners() {
//        buttonPrev.setOnClickListener(v -> {
//            currentWeekStart.add(Calendar.WEEK_OF_YEAR, -1); // Lùi 1 tuần
//            updateDateRangeText();
//            loadDataForCurrentWeek();
//        });
//
//        buttonNext.setOnClickListener(v -> {
//            currentWeekStart.add(Calendar.WEEK_OF_YEAR, 1); // Tiến 1 tuần
//            updateDateRangeText();
//            loadDataForCurrentWeek();
//        });
//    }
//
//    // --- Cập nhật Text hiển thị khoảng ngày ---
//    private void updateDateRangeText() {
//        Calendar weekEnd = (Calendar) currentWeekStart.clone();
//        weekEnd.add(Calendar.DAY_OF_YEAR, 6); // Ngày cuối tuần là 6 ngày sau ngày đầu tuần
//        String startDateStr = dateFormat.format(currentWeekStart.getTime());
//        String endDateStr = dateFormat.format(weekEnd.getTime());
//        dateRangeText.setText(String.format("%s - %s", startDateStr, endDateStr));
//    }
//
//    // --- Tải dữ liệu từ Firestore cho tuần đang chọn ---
//    private void loadDataForCurrentWeek() {
//        if (currentUser == null) {
//            showError("Người dùng chưa đăng nhập.");
//            clearUI();
//            return;
//        }
//        String userId = currentUser.getUid();
//
//        // 1. Tạo Task để lấy dữ liệu tổng quan của User (nếu cần cho Goal Preview)
//        DocumentReference userDocRef = db.collection("users").document(userId);
//        Task<DocumentSnapshot> userTask = userDocRef.get();
//
//        // 2. Tạo Task để lấy dữ liệu log nước trong tuần
//        Calendar weekEnd = (Calendar) currentWeekStart.clone();
//        weekEnd.add(Calendar.DAY_OF_YEAR, 7); // Ngày bắt đầu của tuần sau (để query < endDate)
//        // Đặt thời gian về 00:00:00 cho ngày bắt đầu và kết thúc để so sánh chính xác
//        setCalendarToStartOfDay(currentWeekStart);
//        setCalendarToStartOfDay(weekEnd);
//
//        // Giả sử bạn có collection 'waterLogs' hoặc tương tự, chứa các bản ghi theo ngày
//        // CollectionReference waterLogColRef = userDocRef.collection("waterLogs"); // Nếu là subcollection
//        CollectionReference waterLogColRef = db.collection("waterLogs"); // Hoặc collection cấp cao nhất (cần thêm trường userId)
//
//        Query waterQuery = waterLogColRef
//                .whereEqualTo("userId", userId) // Cần trường userId nếu là collection cấp cao
//                .whereGreaterThanOrEqualTo("date", currentWeekStart.getTime()) // Lớn hơn hoặc bằng ngày đầu tuần
//                .whereLessThan("date", weekEnd.getTime()) // Nhỏ hơn ngày đầu tuần sau
//                .orderBy("date", Query.Direction.ASCENDING); // Sắp xếp theo ngày
//
//        Task<QuerySnapshot> waterTask = waterQuery.get();
//
//        // 3. Kết hợp cả hai Task
//        Task<List<Object>> allTasks = Tasks.whenAllSuccess(userTask, waterTask);
//
//        allTasks.addOnSuccessListener(results -> {
//            // Kết quả trả về là một List chứa kết quả của từng Task theo thứ tự
//            DocumentSnapshot userSnapshot = (DocumentSnapshot) results.get(0);
//            QuerySnapshot waterSnapshot = (QuerySnapshot) results.get(1);
//
//            User user = null;
//            if (userSnapshot.exists()) {
//                user = userSnapshot.toObject(User.class);
//            } else {
//                Log.w(TAG, "User document not found for ID: " + userId);
//                // Có thể tạo user mặc định hoặc xử lý khác
//            }
//
//            List<WaterLog> weeklyLogs = new ArrayList<>();
//            if (waterSnapshot != null && !waterSnapshot.isEmpty()) {
//                weeklyLogs = waterSnapshot.toObjects(WaterLog.class);
//                Log.d(TAG, "Fetched " + weeklyLogs.size() + " water logs for the week.");
//            } else {
//                Log.d(TAG, "No water logs found for the week starting " + dateFormat.format(currentWeekStart.getTime()));
//            }
//
//            // 4. Cập nhật UI với dữ liệu đã lấy được
//            updateUI(user, weeklyLogs);
//
//        }).addOnFailureListener(e -> {
//            Log.e(TAG, "Error fetching data for week", e);
//            showError("Lỗi tải dữ liệu: " + e.getMessage());
//            clearUI(); // Hiển thị trạng thái lỗi/trống
//        });
//    }
//
//    // --- Cập nhật toàn bộ giao diện người dùng ---
//    private void updateUI(User user, List<WaterLog> weeklyLogs) {
//        updateBarChart(weeklyLogs);
//        updateGoalPreview(user, weeklyLogs); // Cập nhật cả Goal Preview
//    }
//
//    // --- Cập nhật Biểu đồ cột ---
//    private void updateBarChart(List<WaterLog> weeklyLogs) {
//        double[] dailyTotals = new double[7]; // Mảng lưu tổng lượng nước mỗi ngày (0=Mon, 6=Sun)
//
//        Calendar logCal = Calendar.getInstance();
//        for (WaterLog log : weeklyLogs) {
//            if (log.getDate() != null) {
//                logCal.setTime(log.getDate());
//                int dayOfWeek = getDayOfWeekIndex(logCal); // 0 = Monday, ..., 6 = Sunday
//                if (dayOfWeek >= 0 && dayOfWeek < 7) {
//                    dailyTotals[dayOfWeek] += log.getAmountLiters(); // Cộng dồn lượng nước
//                }
//            }
//        }
//
//        // Tìm giá trị lớn nhất trong tuần để scale (hoặc dùng mục tiêu cố định)
//        double maxWeeklyValue = DAILY_WATER_GOAL_LITERS; // Bắt đầu với mục tiêu
//        for (double total : dailyTotals) {
//            if (total > maxWeeklyValue) {
//                //maxWeeklyValue = total; // Bỏ comment nếu muốn scale theo max thực tế
//            }
//        }
//        // Đảm bảo max value không phải là 0 để tránh chia cho 0
//        maxWeeklyValue = Math.max(maxWeeklyValue, 0.1); // Giá trị tối thiểu để tránh lỗi
//
//        // Cập nhật từng cột
//        for (int i = 0; i < 7; i++) {
//            double value = dailyTotals[i];
//            // Cập nhật text hiển thị lượng nước
//            valueTextViews[i].setText(String.format(Locale.US, "%.1fL", value));
//
//            // Tính toán và cập nhật chiều cao cột
//            double barHeightRatio = value / maxWeeklyValue;
//            int barHeightPx = dpToPx((int) (barHeightRatio * MAX_BAR_HEIGHT_DP));
//            barHeightPx = Math.max(0, barHeightPx); // Đảm bảo chiều cao không âm
//
//            ViewGroup.LayoutParams params = barViews[i].getLayoutParams();
//            params.height = barHeightPx;
//            barViews[i].setLayoutParams(params);
//            barViews[i].requestLayout(); // Yêu cầu vẽ lại layout cho view này
//            // Log.d(TAG, "Day " + i + ": Value=" + value + ", HeightPx=" + barHeightPx);
//        }
//        // Có thể cần gọi invalidate() trên barChartContainer nếu không tự cập nhật
//        // barChartContainer.invalidate();
//    }
//
//    // --- Cập nhật khu vực Goal Preview ---
//    private void updateGoalPreview(User user, List<WaterLog> weeklyLogs) {
//        // --- Cập nhật Circular Progress ---
//        // Logic tính toán % tổng thể có thể phức tạp:
//        // 1. Dựa trên điểm trong User model?
//        // 2. Dựa trên tỷ lệ hoàn thành mục tiêu nước tuần này?
//        // Ví dụ: Tính % hoàn thành mục tiêu tuần
//        double totalWeekWater = 0;
//        for (WaterLog log : weeklyLogs) {
//            totalWeekWater += log.getAmountLiters();
//        }
//        double weeklyGoal = DAILY_WATER_GOAL_LITERS * 7;
//        int overallPercent = 0;
//        if (weeklyGoal > 0) {
//            overallPercent = (int) ((totalWeekWater / weeklyGoal) * 100);
//        }
//        overallPercent = Math.min(100, Math.max(0, overallPercent)); // Giới hạn 0-100%
//
//        progressText.setText(String.format("%d%%", overallPercent));
//        // TODO: Cập nhật hình ảnh của circularProgressBar nếu dùng thư viện progress bar thực sự
//        // Ví dụ: nếu dùng CircularProgressIndicator của Material:
//        // circularProgressIndicator.setProgressCompat(overallPercent, true);
//
//        // --- Cập nhật Performance Cards ---
//        // Logic này phụ thuộc vào cách bạn tính toán "Performance"
//        // Giả sử User model có trường athletePerformance và cognitivePerformance (từ 0-100)
//        int athletePerf = 0;
//        int cognitivePerf = 0;
//        if (user != null) {
//            // Giả sử có các getter này trong User.java
//            // athletePerf = user.getAthletePerformancePercent();
//            // cognitivePerf = user.getCognitivePerformancePercent();
//
//            // **LƯU Ý**: Giao diện đang hiển thị "+100%". Điều này có thể là một sự *tăng* hiệu suất
//            // thay vì một giá trị tuyệt đối. Bạn cần định nghĩa rõ cách tính này.
//            // Ví dụ đơn giản: Nếu hoàn thành mục tiêu tuần thì là +100%?
//            if (overallPercent >= 100) {
//                athletePerf = 100; // Ví dụ
//                cognitivePerf = 100; // Ví dụ
//            } else {
//                // Tính toán dựa trên % hoàn thành?
//                athletePerf = overallPercent; // Ví dụ đơn giản
//                cognitivePerf = overallPercent; // Ví dụ đơn giản
//            }
//        }
//
//        if (athletePercentageText != null) {
//            athletePercentageText.setText(String.format(Locale.US,"+%d%%", athletePerf));
//            // Có thể đổi màu text dựa trên giá trị
//            athletePercentageText.setTextColor(getResources().getColor(athletePerf >= 80 ? R.color.positive_green : R.color.neutral_gray)); // Thêm màu vào colors.xml
//        }
//        if (cognitivePercentageText != null) {
//            cognitivePercentageText.setText(String.format(Locale.US,"+%d%%", cognitivePerf));
//            // cognitivePercentageText.setTextColor(getResources().getColor(cognitivePerf >= 80 ? R.color.positive_green : R.color.neutral_gray));
//        }
//
//    }
//
//    // --- Xóa/Đặt lại UI về trạng thái mặc định ---
//    private void clearUI() {
//        // Đặt lại biểu đồ cột
//        for (int i = 0; i < 7; i++) {
//            valueTextViews[i].setText("0.0L");
//            ViewGroup.LayoutParams params = barViews[i].getLayoutParams();
//            params.height = 0; // Đặt chiều cao về 0
//            barViews[i].setLayoutParams(params);
//        }
//        // Đặt lại Goal Preview
//        progressText.setText("0%");
//        if (athletePercentageText != null) athletePercentageText.setText("+0%");
//        if (cognitivePercentageText != null) cognitivePercentageText.setText("+0%");
//        // Cập nhật hình ảnh progress bar về 0 nếu cần
//    }
//
//
//    // --- Tiện ích ---
//    private void setCalendarToStartOfWeek(Calendar cal) {
//        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek()); // Đặt về ngày đầu tiên của tuần (CN hoặc T2 tùy Locale)
//        setCalendarToStartOfDay(cal); // Đặt về 00:00:00
//        // Nếu tuần của bạn bắt đầu vào Thứ Hai (Monday)
//        if (cal.getFirstDayOfWeek() == Calendar.SUNDAY) {
//            cal.add(Calendar.DAY_OF_YEAR, 1); // Nếu Locale bắt đầu từ CN, dịch sang T2
//            if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
//                // Xử lý trường hợp đặc biệt nếu cần
//            }
//        } else if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
//            // Đảm bảo là thứ hai nếu Locale đã là T2
//            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
//        }
//    }
//
//    private void setCalendarToStartOfDay(Calendar cal) {
//        cal.set(Calendar.HOUR_OF_DAY, 0);
//        cal.set(Calendar.MINUTE, 0);
//        cal.set(Calendar.SECOND, 0);
//        cal.set(Calendar.MILLISECOND, 0);
//    }
//
//    // Trả về index 0-6 (Mon-Sun)
//    private int getDayOfWeekIndex(Calendar cal) {
//        int day = cal.get(Calendar.DAY_OF_WEEK); // Sunday = 1, ..., Saturday = 7
//        // Chuyển đổi sang Monday = 0, ..., Sunday = 6
//        if (day == Calendar.SUNDAY) {
//            return 6;
//        } else {
//            return day - Calendar.MONDAY; // Monday=2 -> 0, Tuesday=3 -> 1,... Saturday=7 -> 5
//        }
//    }
//
//    private int dpToPx(int dp) {
//        float density = getResources().getDisplayMetrics().density;
//        return Math.round((float) dp * density);
//    }
//
//    private void showError(String message) {
//        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
//        Log.e(TAG, message);
//    }
//
//    // --- Model giả định cho Log nước (Cần tạo file WaterLog.java) ---
//    // Ví dụ:
//    // package com.example.canvas.models;
//    // import java.util.Date;
//    // public class WaterLog {
//    //     private String userId;
//    //     private Date date;
//    //     private double amountLiters;
//    //     public WaterLog() {} // Constructor mặc định
//    //     // Getters and Setters...
//    //     public String getUserId() { return userId; }
//    //     public void setUserId(String userId) { this.userId = userId; }
//    //     public Date getDate() { return date; }
//    //     public void setDate(Date date) { this.date = date; }
//    //     public double getAmountLiters() { return amountLiters; }
//    //     public void setAmountLiters(double amountLiters) { this.amountLiters = amountLiters; }
//    // }
//}


package com.example.canvas; // Giữ nguyên package của bạn

// Các import cần thiết cho Activity và View cơ bản
import android.os.Bundle;
import android.util.Log; // Giữ lại Log nếu cần debug cơ bản
import android.widget.Toast;

// Import lớp cơ sở điều hướng của bạn (Giả sử tên là BaseNavigationActivity)
// Nếu bạn đặt tên khác (như NavigationActivity), hãy thay đổi ở đây và dòng extends
import com.example.canvas.NavigationActivity;

// Các import không cần thiết nữa đã được xóa (như Firebase, Calendar, List, CardView, specific Views...)

// 1. Kế thừa từ lớp Activity cơ sở chứa logic điều hướng
//    Đảm bảo tên lớp 'BaseNavigationActivity' là chính xác
public class ProgressActivity extends NavigationActivity { // <<< THAY ĐỔI Ở ĐÂY NẾU TÊN KHÁC

    private static final String TAG = "ProgressActivity"; // Tag để log nếu cần

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Luôn gọi super.onCreate

        // 2. Đặt layout XML tĩnh cho Activity này
        //    Đảm bảo bạn có file res/layout/progress.xml
        try {
            setContentView(R.layout.progress); // <<< SỬ DỤNG LAYOUT XML CỦA BẠN
        } catch (Exception e) {
            Log.e(TAG, "Error setting content view. Check R.layout.progress exists and is valid.", e);
            // Có thể thêm Toast hoặc xử lý lỗi khác ở đây
            Toast.makeText(this, "Layout Error", Toast.LENGTH_LONG).show();
            finish(); // Kết thúc nếu không thể tải layout
            return;
        }


        // 3. Gọi phương thức setup của lớp cơ sở ĐIỀU HƯỚNG SAU setContentView
        //    Phương thức này sẽ tìm BottomNavigationView và thiết lập listener.
        try {
            setupBottomNavigation();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up bottom navigation. Does the layout include the navigation view?", e);
            // Có thể thêm Toast hoặc xử lý lỗi khác
            Toast.makeText(this, "Navigation Error", Toast.LENGTH_LONG).show();
        }


        // --- TOÀN BỘ LOGIC TẢI DỮ LIỆU, CẬP NHẬT UI ĐỘNG ĐÃ ĐƯỢC XÓA ---
        // --- Bạn không cần tìm các TextView, View, ProgressBar, CardView ở đây nữa ---
        // --- vì chúng ta chỉ hiển thị nội dung tĩnh từ XML ---
        // --- Các listener cho nút Prev/Next cũng bị xóa vì không có logic ngày tháng ---

        Log.d(TAG, "ProgressActivity created and static layout displayed."); // Log đơn giản
    }

    // 4. Implement phương thức trừu tượng để cho BaseNavigationActivity biết mục nào đang active
    @Override
    protected int getCurrentBottomNavigationItemId() {
        // Trả về ID của mục "Progress" trong tệp menu.xml của bạn
        // (ví dụ: res/menu/bottom_nav_menu.xml)
        return R.id.navProgressButton; // <<< Đảm bảo ID này khớp với menu item "Progress"
    }

    // --- TẤT CẢ CÁC PHƯƠNG THỨC KHÁC (setupViews, setupListeners, loadData, updateUI, v.v.) ĐÃ BỊ XÓA ---
}