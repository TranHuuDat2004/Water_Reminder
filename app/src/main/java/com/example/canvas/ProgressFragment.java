package com.example.canvas;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar; // Import ProgressBar
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import android.widget.LinearLayout;

public class ProgressFragment extends Fragment {

    private static final String TAG = "ProgressFragment";

    // --- Views ---
    private Button buttonPrev, buttonNext;
    private TextView dateRangeText;
    private TextView valueMon, valueTue, valueWed, valueThu, valueFri, valueSat, valueSun;
    private View barMon, barTue, barWed, barThu, barFri, barSat, barSun;
    private TextView[] valueTextViews;
    private View[] barViews;
    private ProgressBar progressBar;
    private LinearLayout barChartContainer;// Thêm ProgressBar

    // --- Data & Constants ---
    private Calendar currentWeekStart;
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
    private SimpleDateFormat dataKeyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    // *** Điều chỉnh MAX_WATER_GOAL_ML nếu bạn muốn lấy từ dailyGoal trên Firestore ***
    private final int MAX_WATER_GOAL_ML = 3000; // Giữ nguyên hoặc điều chỉnh
    private final int MAX_BAR_HEIGHT_DP = 180;
    private int maxBarHeightPx;

    // --- Firebase ---
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_progress, container, false); // Thay tên layout

        // --- Firebase Init ---
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        } else {
            Log.w(TAG, "User not logged in.");
            // Xử lý khi chưa đăng nhập (ví dụ: hiển thị thông báo)
        }

        // --- View Mapping ---
        buttonPrev = view.findViewById(R.id.buttonPrev);
        buttonNext = view.findViewById(R.id.buttonNext);
        dateRangeText = view.findViewById(R.id.dateRangeText);
        valueMon = view.findViewById(R.id.valueMon);
        valueTue = view.findViewById(R.id.valueTue);
        valueWed = view.findViewById(R.id.valueWed);
        valueThu = view.findViewById(R.id.valueThu);
        valueFri = view.findViewById(R.id.valueFri);
        valueSat = view.findViewById(R.id.valueSat);
        valueSun = view.findViewById(R.id.valueSun);
        barMon = view.findViewById(R.id.barMon);
        barTue = view.findViewById(R.id.barTue);
        barWed = view.findViewById(R.id.barWed);
        barThu = view.findViewById(R.id.barThu);
        barFri = view.findViewById(R.id.barFri);
        barSat = view.findViewById(R.id.barSat);
        barSun = view.findViewById(R.id.barSun);
        barChartContainer = view.findViewById(R.id.barChartContainer);
        // Thêm ProgressBar vào layout XML của bạn nếu chưa có và ánh xạ ở đây
         progressBar = view.findViewById(R.id.progressBar); // Ví dụ ID

        valueTextViews = new TextView[]{valueMon, valueTue, valueWed, valueThu, valueFri, valueSat, valueSun};
        barViews = new View[]{barMon, barTue, barWed, barThu, barFri, barSat, barSun};

        // --- Init Date ---
        currentWeekStart = Calendar.getInstance();
        currentWeekStart.setFirstDayOfWeek(Calendar.MONDAY);
        while (currentWeekStart.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            currentWeekStart.add(Calendar.DATE, -1);
        }

        // --- DP to PX ---
        maxBarHeightPx = dpToPx(MAX_BAR_HEIGHT_DP);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupButtonClickListeners();

        if (userId != null) {
            fetchAndDisplayWeekData(currentWeekStart);
        } else {
            Toast.makeText(getContext(), "Please login to view progress", Toast.LENGTH_LONG).show();
            clearChartUI();
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        // Ẩn/hiện nút
        buttonPrev.setEnabled(!show);
        buttonNext.setEnabled(!show);

        // ***** THAY ĐỔI QUAN TRỌNG *****
        if (show) {
            // Khi đang loading:
            dateRangeText.setText("Loading..."); // Đặt lại text loading
            if (barChartContainer != null) {
                barChartContainer.setVisibility(View.INVISIBLE); // Ẩn biểu đồ (hoặc GONE)
            }
        } else {
            // Khi loading xong (cả thành công và thất bại):
            // KHÔNG đặt lại text ở đây, vì text ngày sẽ được đặt trong fetchAndDisplayWeekData
            if (barChartContainer != null) {
                barChartContainer.setVisibility(View.VISIBLE); // Hiện biểu đồ
            }
            // dateRangeText sẽ được cập nhật với khoảng ngày thực tế sau khi fetch xong
        }
        // ***** KẾT THÚC THAY ĐỔI *****
    }


    private void setupButtonClickListeners() {
        buttonPrev.setOnClickListener(v -> {
            if (userId != null) {
                currentWeekStart.add(Calendar.DATE, -7);
                fetchAndDisplayWeekData(currentWeekStart);
            }
        });

        buttonNext.setOnClickListener(v -> {
            if (userId != null) {
                currentWeekStart.add(Calendar.DATE, 7);
                fetchAndDisplayWeekData(currentWeekStart);
            }
        });
    }

    private void fetchAndDisplayWeekData(Calendar weekStartCalendar) {
        if (userId == null) {
            Log.w(TAG, "Cannot fetch data: userId is null.");
            clearChartUI();
            return;
        }

        showLoading(true); // Hiển thị loading
        clearChartUI(); // Xóa dữ liệu cũ trước khi load

        Calendar weekEndCalendar = (Calendar) weekStartCalendar.clone();
        weekEndCalendar.add(Calendar.DATE, 6);

        String startDateStr = displayDateFormat.format(weekStartCalendar.getTime());
        String endDateStr = displayDateFormat.format(weekEndCalendar.getTime());
        dateRangeText.setText(String.format("%s - %s", startDateStr, endDateStr));

        String startQueryDate = dataKeyFormat.format(weekStartCalendar.getTime());
        String endQueryDate = dataKeyFormat.format(weekEndCalendar.getTime());

        Log.d(TAG, "Fetching data for user " + userId + " in range: " + startQueryDate + " to " + endQueryDate);

        // ***** THAY ĐỔI ĐƯỜNG DẪN COLLECTION *****
        db.collection("water_tracker").document(userId)
                .collection("daily_logs") // ***** THAY ĐỔI SUBCOLLECTION *****
                .whereGreaterThanOrEqualTo(com.google.firebase.firestore.FieldPath.documentId(), startQueryDate)
                .whereLessThanOrEqualTo(com.google.firebase.firestore.FieldPath.documentId(), endQueryDate)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Successfully fetched " + queryDocumentSnapshots.size() + " documents.");
                    processFirestoreResultsAndUpdateUI(queryDocumentSnapshots, weekStartCalendar);
                    showLoading(false); // Ẩn loading khi thành công
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching weekly water intake", e);
                    Toast.makeText(getContext(), "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    clearChartUI();
                    showLoading(false); // Ẩn loading khi lỗi
                });
    }

    private void processFirestoreResultsAndUpdateUI(QuerySnapshot snapshots, Calendar weekStartCalendar) {
        Map<String, Integer> weeklyData = new HashMap<>();
        if (snapshots != null && !snapshots.isEmpty()) {
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                try {
                    // ***** THAY ĐỔI TÊN TRƯỜNG DỮ LIỆU *****
                    // Đọc là Double để linh hoạt, sau đó chuyển sang int
                    Double totalWaterDouble = doc.getDouble("totalWater");
                    int totalWaterMl = (totalWaterDouble != null) ? totalWaterDouble.intValue() : 0; // Lấy giá trị, mặc định là 0 nếu null

                    weeklyData.put(doc.getId(), totalWaterMl);
                    // Log.d(TAG, "Data found for " + doc.getId() + ": " + totalWaterMl);

                    // --- Tùy chọn: Cập nhật MAX_WATER_GOAL_ML từ Firestore ---
                    // Nếu bạn muốn mỗi ngày có mục tiêu riêng hiển thị trên biểu đồ
                    // Double dailyGoalDouble = doc.getDouble("dailyGoal");
                    // if (dailyGoalDouble != null) {
                    //     int dailyGoalMl = dailyGoalDouble.intValue();
                    //     // Bạn cần quyết định cách sử dụng dailyGoal này.
                    //     // Ví dụ: dùng nó thay cho MAX_WATER_GOAL_ML cho ngày cụ thể đó
                    //     // Hoặc tính trung bình tuần... Hiện tại đang dùng MAX_WATER_GOAL_ML cố định.
                    // }
                    // --- Kết thúc tùy chọn ---

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing data for document " + doc.getId(), e);
                    weeklyData.put(doc.getId(), 0); // Đặt là 0 nếu lỗi parsing
                }
            }
        } else {
            Log.d(TAG, "No documents found for the specified week range.");
        }

        Calendar tempCalendar = (Calendar) weekStartCalendar.clone();
        for (int i = 0; i < 7; i++) {
            String currentDayDateString = dataKeyFormat.format(tempCalendar.getTime());
            int waterAmountMl = weeklyData.getOrDefault(currentDayDateString, 0);

            if (i < valueTextViews.length && i < barViews.length) {
                updateBarUI(barViews[i], valueTextViews[i], waterAmountMl);
            }

            tempCalendar.add(Calendar.DATE, 1);
        }
    }


    private void updateBarUI(View barView, TextView valueTextView, int waterAmountMl) {
        int barHeightPx;
        // Sử dụng MAX_WATER_GOAL_ML cố định hoặc giá trị dailyGoal lấy từ Firestore (nếu bạn triển khai)
        int currentGoal = MAX_WATER_GOAL_ML;

        if (waterAmountMl <= 0) {
            barHeightPx = 1;
        } else if (waterAmountMl >= currentGoal) {
            barHeightPx = maxBarHeightPx;
        } else {
            // Đảm bảo currentGoal > 0 để tránh chia cho 0
            if (currentGoal > 0) {
                barHeightPx = (int) (((float) waterAmountMl / currentGoal) * maxBarHeightPx);
            } else {
                barHeightPx = 1; // Hoặc 0 nếu goal = 0
            }
            if (barHeightPx < 1) barHeightPx = 1;
        }

        ViewGroup.LayoutParams params = barView.getLayoutParams();
        if (params != null) {
            params.height = barHeightPx;
            barView.setLayoutParams(params);
        } else {
            Log.e(TAG, "LayoutParams for barView is null!");
        }

        float waterAmountL = waterAmountMl / 1000.0f;
        valueTextView.setText(String.format(Locale.getDefault(), "%.1fL", waterAmountL));
        valueTextView.setVisibility(waterAmountMl > 0 ? View.VISIBLE : View.INVISIBLE);
    }

    private int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    private void clearChartUI() {
        if (valueTextViews == null || barViews == null) return;
        for (int i = 0; i < 7; i++) {
            if (i < valueTextViews.length && i < barViews.length) {
                updateBarUI(barViews[i], valueTextViews[i], 0);
            }
        }
        // dateRangeText.setText("---"); // Reset text nếu muốn
    }

    // --- Hàm ví dụ để cập nhật dữ liệu nước uống lên Firestore ---
    // Hàm này cần được gọi từ nơi khác khi người dùng thêm/sửa lượng nước
    public void updateWaterIntakeOnFirestore(String dateString, int newTotalWaterAmountMl) {
        if (userId == null) {
            Log.w(TAG, "Cannot update data: userId is null.");
            Toast.makeText(getContext(), "Please login to save data", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            dataKeyFormat.parse(dateString);
        } catch (ParseException e) {
            Log.e(TAG, "Invalid date format for update: " + dateString, e);
            Toast.makeText(getContext(), "Invalid date format", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> waterData = new HashMap<>();
        // ***** THAY ĐỔI TÊN TRƯỜNG DỮ LIỆU *****
        waterData.put("totalWater", newTotalWaterAmountMl);
        // Bạn có thể cập nhật các trường khác nếu cần, ví dụ: lastAddedAmount, lastAddedTimestamp
        // waterData.put("lastAddedAmount", amountJustAdded);
        // waterData.put("lastAddedTimestamp", com.google.firebase.Timestamp.now());

        Log.d(TAG, "Attempting to update Firestore for " + dateString + " with totalWater=" + newTotalWaterAmountMl + "ml");

        // ***** THAY ĐỔI ĐƯỜNG DẪN COLLECTION & SUBCOLLECTION *****
        db.collection("water_tracker").document(userId)
                .collection("daily_logs").document(dateString)
                .set(waterData, SetOptions.merge()) // Merge để không ghi đè các field khác như dailyGoal
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Firestore update successful for " + dateString);
                    checkAndRefreshChartIfNeeded(dateString);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating Firestore for " + dateString, e);
                    Toast.makeText(getContext(), "Failed to save data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkAndRefreshChartIfNeeded(String updatedDateString) {
        try {
            Calendar updatedDateCal = Calendar.getInstance();
            updatedDateCal.setTime(Objects.requireNonNull(dataKeyFormat.parse(updatedDateString)));

            Calendar weekStart = (Calendar) currentWeekStart.clone();
            weekStart.set(Calendar.HOUR_OF_DAY, 0); weekStart.set(Calendar.MINUTE, 0); weekStart.set(Calendar.SECOND, 0); weekStart.set(Calendar.MILLISECOND, 0);

            Calendar weekEnd = (Calendar) currentWeekStart.clone();
            weekEnd.add(Calendar.DATE, 6);
            weekEnd.set(Calendar.HOUR_OF_DAY, 23); weekEnd.set(Calendar.MINUTE, 59); weekEnd.set(Calendar.SECOND, 59); weekEnd.set(Calendar.MILLISECOND, 999);

            updatedDateCal.set(Calendar.HOUR_OF_DAY, 12);

            if (!updatedDateCal.before(weekStart) && !updatedDateCal.after(weekEnd)) {
                Log.d(TAG, "Updated date " + updatedDateString + " is within the current week. Refreshing chart.");
                if (getActivity() != null) {
                    // Không cần chạy trên UI thread vì Firestore listener thường đã ở main thread
                    fetchAndDisplayWeekData(currentWeekStart);
                } else {
                    Log.w(TAG,"Fragment detached, cannot refresh chart UI.");
                }
            } else {
                Log.d(TAG, "Updated date " + updatedDateString + " is outside the current week. No chart refresh needed.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking if date needs refresh: " + updatedDateString, e);
        }
    }
}