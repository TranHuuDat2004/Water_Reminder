package com.example.canvas;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
// --- THÊM IMPORT FIREBASE FIRESTORE ---
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
// --- KẾT THÚC IMPORT ---

public class ForgotPasswordValidateActivity extends AppCompatActivity {

    private EditText etUsername, etPhone; // Đổi tên biến
    private Button btnVerify;
    private ProgressBar progressBar;
    private ImageView btnBack;
    // --- THÊM FIRESTORE INSTANCE ---
    private FirebaseFirestore db;
    // --- KẾT THÚC THÊM ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password_validate);

        etUsername = findViewById(R.id.etValidateUsername); // Đổi ID
        etPhone = findViewById(R.id.etValidatePhone);
        btnVerify = findViewById(R.id.btnVerifyAccount);
        progressBar = findViewById(R.id.progressBarValidate);
        btnBack = findViewById(R.id.btnValidateBack);

        db = FirebaseFirestore.getInstance(); // Khởi tạo Firestore

        btnBack.setOnClickListener(v -> finish());
        btnVerify.setOnClickListener(v -> attemptVerification());
    }

    private void attemptVerification() {
        String username = etUsername.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        // Validation cơ bản
        if (username.isEmpty()) {
            etUsername.setError(getString(R.string.username_required)); // Thêm string
            etUsername.requestFocus();
            return;
        }
        if (phone.isEmpty()) {
            etPhone.setError(getString(R.string.phone_required)); // Thêm string
            etPhone.requestFocus();
            return;
        }
        // Thêm validation khác nếu cần

        verifyAccountWithFirestore(username, phone); // Gọi hàm kiểm tra bằng Firestore
    }

    private void verifyAccountWithFirestore(String username, String phone) {
        progressBar.setVisibility(View.VISIBLE);
        btnVerify.setEnabled(false);

        // --- TRUY VẤN FIRESTORE ---
        db.collection("users") // Tên collection users của bạn
                .whereEqualTo("username", username) // Kiểm tra trường username
                .whereEqualTo("phoneNumber", phone) // Kiểm tra trường phoneNumber (Đảm bảo tên trường đúng với Firestore của bạn)
                .limit(1) // Chỉ cần tìm 1 kết quả là đủ
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    btnVerify.setEnabled(true);

                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            // --- Tìm thấy tài khoản khớp ---
                            // Lấy document đầu tiên (vì đã limit 1)
                            QueryDocumentSnapshot document = (QueryDocumentSnapshot) querySnapshot.getDocuments().get(0);
                            String userId = document.getId(); // ID của document chính là User ID của Firebase Auth
                            String userEmail = document.getString("email"); // Lấy email từ Firestore

                            // Kiểm tra lại email có tồn tại không (phòng trường hợp dữ liệu lỗi)
                            if (userEmail == null || userEmail.isEmpty()) {
                                Toast.makeText(this, "Error: User data incomplete (missing email).", Toast.LENGTH_LONG).show();
                                return;
                            }

                            Toast.makeText(ForgotPasswordValidateActivity.this, getString(R.string.account_verified_proceed_reset), Toast.LENGTH_SHORT).show();

                            // Chuyển sang màn hình đặt lại mật khẩu, TRUYỀN EMAIL (vì cần email để gọi API reset mật khẩu của Firebase Auth sau này nếu dùng Cloud Function)
                            // Hoặc truyền userId nếu bạn dùng nó để định danh user trong Cloud Function
                            Intent intent = new Intent(ForgotPasswordValidateActivity.this, ResetPasswordVerifiedActivity.class);
                            intent.putExtra("USER_EMAIL", userEmail);
                            // intent.putExtra("USER_ID", userId); // Nếu muốn truyền cả userId
                            startActivity(intent);
                            finish(); // Đóng activity này

                        } else {
                            // --- Không tìm thấy tài khoản nào khớp ---
                            Toast.makeText(ForgotPasswordValidateActivity.this, getString(R.string.verification_failed_check_info_username), Toast.LENGTH_LONG).show(); // Thêm string này (ví dụ: "Verification failed. Check username and phone number.")
                        }
                    } else {
                        // --- Lỗi khi truy vấn Firestore ---
                        Log.w("FirestoreQuery", "Error getting documents: ", task.getException());
                        Toast.makeText(ForgotPasswordValidateActivity.this, "Error verifying account. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
        // --- KẾT THÚC TRUY VẤN FIRESTORE ---
    }
}