package com.example.canvas;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
// BỎ CÁC IMPORT LIÊN QUAN ĐẾN VIỆC GỌI API RESET PASSWORD

public class ResetPasswordVerifiedActivity extends AppCompatActivity {

    private EditText etNewPassword, etConfirmPassword;
    private Button btnConfirmReset;
    private ProgressBar progressBar;
    private ImageView btnBack, btnToggleNewPass, btnToggleConfirmPass;
    private String verifiedEmail; // Email hoặc ID user đã xác thực

    private boolean isNewPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password_verified);

        // Ánh xạ view (giữ nguyên)
        etNewPassword = findViewById(R.id.etResetVerifiedNewPassword);
        etConfirmPassword = findViewById(R.id.etResetVerifiedConfirmPassword);
        btnConfirmReset = findViewById(R.id.btnConfirmResetPassword);
        progressBar = findViewById(R.id.progressBarResetVerified);
        btnBack = findViewById(R.id.btnResetVerifiedBack);
        btnToggleNewPass = findViewById(R.id.btnToggleResetVerifiedNewPassword);
        btnToggleConfirmPass = findViewById(R.id.btnToggleResetVerifiedConfirmPassword);

        btnBack.setOnClickListener(v -> finish());

        // Lấy email/userId đã xác thực
        verifiedEmail = getIntent().getStringExtra("USER_EMAIL"); // Hoặc "USER_ID"
        if (verifiedEmail == null || verifiedEmail.isEmpty()) {
            Toast.makeText(this, "Error: Verification data missing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Setup toggle password (giữ nguyên)
        setupPasswordToggle(etNewPassword, btnToggleNewPass, isNewPasswordVisible);
        setupPasswordToggle(etConfirmPassword, btnToggleConfirmPass, isConfirmPasswordVisible);


        btnConfirmReset.setOnClickListener(v -> attemptPasswordReset());
    }

    private void setupPasswordToggle(final EditText editText, final ImageView toggleButton, boolean initialVisibilityState) {
        // Sử dụng một biến tạm để lưu trạng thái, hoặc kiểm tra trực tiếp transformationMethod
        final boolean[] isCurrentlyVisible = {initialVisibilityState}; // Dùng mảng để thay đổi giá trị trong lambda

        // Đặt trạng thái ban đầu (có thể bỏ qua nếu dùng kiểm tra transformationMethod)
        if (initialVisibilityState) {
            editText.setTransformationMethod(null);
            toggleButton.setImageResource(R.drawable.ic_visibility); // Icon mắt mở
        } else {
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            toggleButton.setImageResource(R.drawable.ic_visibility_off); // Icon mắt đóng
        }


        toggleButton.setOnClickListener(v -> {
            // Kiểm tra trạng thái hiện tại của EditText
            if (editText.getTransformationMethod() == null) {
                // Đang hiện -> Chuyển sang ẩn
                editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                toggleButton.setImageResource(R.drawable.ic_visibility_off); // Đặt icon mắt đóng
                isCurrentlyVisible[0] = false; // Cập nhật trạng thái (nếu dùng biến)
            } else {
                // Đang ẩn -> Chuyển sang hiện
                editText.setTransformationMethod(null);
                toggleButton.setImageResource(R.drawable.ic_visibility); // Đặt icon mắt mở
                isCurrentlyVisible[0] = true; // Cập nhật trạng thái (nếu dùng biến)
            }
            // Di chuyển con trỏ về cuối sau khi thay đổi trạng thái
            editText.setSelection(editText.length());
        });
    }
    // =============================================
    // ===== KẾT THÚC PHẦN THÊM HÀM ==============
    // =============================================
    // Hàm setupPasswordToggle giữ nguyên

    private void attemptPasswordReset() {
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validation (giữ nguyên)
        if (newPassword.isEmpty()) { /*...*/ return; }
        if (newPassword.length() < 6) { /*...*/ return; }
        if (confirmPassword.isEmpty()) { /*...*/ return; }
        if (!newPassword.equals(confirmPassword)) { /*...*/ return; }

        // --- XỬ LÝ SAU KHI VALIDATION THÀNH CÔNG ---
        // **KHÔNG GỌI API ĐỂ ĐỔI MẬT KHẨU THỰC SỰ Ở ĐÂY**
        // **VÌ KHÔNG AN TOÀN VÀ KHÔNG THỂ THỰC HIỆN TỪ CLIENT SDK**

        progressBar.setVisibility(View.VISIBLE); // Giả lập đang xử lý
        btnConfirmReset.setEnabled(false);

        // Giả lập thành công sau 1 giây
        new Handler().postDelayed(() -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, getString(R.string.password_reset_success_simulation), Toast.LENGTH_LONG).show(); // Thêm string: "Password reset simulated successfully. Please login."

            // Chuyển về màn hình Login
            Intent intent = new Intent(ResetPasswordVerifiedActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        }, 1000);

    }

    // HÀM confirmPasswordResetRequest BỊ XÓA HOẶC COMMENT OUT HOÀN TOÀN
    // private void confirmPasswordResetRequest(String email, String newPassword) { ... }
}