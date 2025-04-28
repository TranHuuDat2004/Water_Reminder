package com.example.canvas;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

// Import Glide để load ảnh
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions; // Để làm tròn ảnh

// Import Firebase
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions; // Rất quan trọng cho cập nhật
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

// Kế thừa từ lớp Navigation cơ sở của bạn
public class EditProfileActivity extends NavigationActivity { // Hoặc tên lớp cơ sở khác

    private static final String TAG = "EditProfileActivity";

    // --- Views ---
    private ImageView ivBackButton; // Đổi tên biến cho rõ ràng
    private ImageView ivProfileImage; // Đổi tên biến
    private EditText etFirstName, etLastName, etEmail, etAge; // Đổi tên biến
    private RadioGroup rgGender; // Đổi tên biến
    private RadioButton rbMale, rbFemale, rbOther; // Đổi tên biến
    private Button btnSave; // Đổi tên biến
    // private ProgressBar progressBar; // Bỏ comment nếu có ProgressBar

    // --- Firebase ---
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;
    private DocumentReference userDocRef;
    private StorageReference profileImageStorageRef; // Đổi tên biến

    // --- Image Handling ---
    private Uri selectedImageUri = null;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_profile); // Sử dụng layout của bạn

        // Setup navigation (từ lớp cha)
        setupBottomNavigation();

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Kiểm tra đăng nhập
        if (currentUser == null) {
            showToast("Vui lòng đăng nhập để chỉnh sửa.");
            finish(); // Đóng activity nếu chưa đăng nhập
            return;
        }

        // Lấy tham chiếu Firestore và Storage
        String userId = currentUser.getUid();
        userDocRef = db.collection("users").document(userId);
        profileImageStorageRef = storage.getReference().child("profile_images").child(userId);

        // Ánh xạ Views
        setupViews();

        // Khởi tạo trình chọn ảnh
        setupImagePicker();

        // Thiết lập listeners
        setupListeners();

        // Tải dữ liệu người dùng hiện tại
        loadUserProfile();
    }

    @Override
    protected int getCurrentBottomNavigationItemId() {
        // Đảm bảo ID này khớp với item 'Setting' hoặc 'Profile' trong menu của bạn
        return R.id.navProfileButton; // VÍ DỤ - THAY THẾ BẰNG ID ĐÚNG
    }

    private void setupViews() {
        ivBackButton = findViewById(R.id.back_button);
        ivProfileImage = findViewById(R.id.profile_image);
        etFirstName = findViewById(R.id.edit_firstName);
        etLastName = findViewById(R.id.edit_lastName);
        etEmail = findViewById(R.id.edit_email);
        etAge = findViewById(R.id.edit_age);
        rgGender = findViewById(R.id.radioGroupGender);
        rbMale = findViewById(R.id.radioButtonMale);
        rbFemale = findViewById(R.id.radioButtonFemale);
        rbOther = findViewById(R.id.radioButtonOther);
        btnSave = findViewById(R.id.save_button);
        // progressBar = findViewById(R.id.progressBar); // Ánh xạ ProgressBar nếu có
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        selectedImageUri = result.getData().getData();
                        // Hiển thị ảnh mới chọn, bo tròn
                        Glide.with(this)
                                .load(selectedImageUri)
                                .apply(RequestOptions.circleCropTransform()) // Bo tròn ảnh
                                .placeholder(R.drawable.avatar1) // Ảnh mặc định khi tải
                                .error(R.drawable.avatar1) // Ảnh mặc định khi lỗi
                                .into(ivProfileImage);
                    }
                });
    }

    private void setupListeners() {
        ivBackButton.setOnClickListener(v -> finish()); // Nút back chỉ cần đóng activity
        ivProfileImage.setOnClickListener(v -> openImageChooser());
        btnSave.setOnClickListener(v -> saveUserProfile());
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void loadUserProfile() {
        setLoadingState(true); // Hiển thị trạng thái tải
        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            setLoadingState(false); // Ẩn trạng thái tải
            if (documentSnapshot.exists()) {
                Log.d(TAG, "User profile data loaded successfully.");
                populateUIWithData(documentSnapshot); // Điền dữ liệu vào UI
            } else {
                Log.w(TAG, "User profile document does not exist for user: " + currentUser.getUid());
                // Đặt email mặc định nếu profile chưa tồn tại
                if (currentUser.getEmail() != null) {
                    etEmail.setText(currentUser.getEmail());
                }
                showToast("Không tìm thấy hồ sơ, hãy tạo mới.");
            }
        }).addOnFailureListener(e -> {
            setLoadingState(false); // Ẩn trạng thái tải
            Log.e(TAG, "Error loading user profile", e);
            showToast("Lỗi tải hồ sơ: " + e.getMessage());
        });
    }

    private void populateUIWithData(DocumentSnapshot snapshot) {
        // Lấy dữ liệu từ snapshot và điền vào các trường UI
        etFirstName.setText(snapshot.getString("firstName"));
        etLastName.setText(snapshot.getString("lastName"));
        etEmail.setText(snapshot.getString("email")); // Có thể không cho sửa email này

        // Xử lý Age (đã lưu là number/Long trên Firestore)
        Long age = snapshot.getLong("age");
        etAge.setText(age != null ? String.valueOf(age) : ""); // Hiển thị nếu có, nếu không thì trống

        // Xử lý Gender
        String gender = snapshot.getString("gender");
        if (gender != null) {
            switch (gender.toLowerCase()) {
                case "male": rbMale.setChecked(true); break;
                case "female": rbFemale.setChecked(true); break;
                case "other": rbOther.setChecked(true); break;
                default: rgGender.clearCheck(); // Bỏ chọn nếu giá trị không hợp lệ
            }
        } else {
            rgGender.clearCheck(); // Bỏ chọn nếu không có dữ liệu
        }

        // Load ảnh profile bằng Glide
        String imageUrl = snapshot.getString("profileImageUrl");
        Glide.with(this)
                .load(imageUrl) // Glide tự xử lý nếu imageUrl là null hoặc trống
                .apply(RequestOptions.circleCropTransform()) // Bo tròn
                .placeholder(R.drawable.avatar1) // Ảnh mặc định
                .error(R.drawable.avatar1) // Ảnh lỗi
                .into(ivProfileImage);
    }

    private void saveUserProfile() {
        // Lấy dữ liệu từ UI
        final String firstName = etFirstName.getText().toString().trim();
        final String lastName = etLastName.getText().toString().trim();
        final String email = etEmail.getText().toString().trim(); // Email thường không nên cho sửa đổi dễ dàng
        final String ageStr = etAge.getText().toString().trim();

        // --- Validation ---
        if (TextUtils.isEmpty(firstName)) { etFirstName.setError("First name required"); etFirstName.requestFocus(); return; }
        if (TextUtils.isEmpty(lastName)) { etLastName.setError("Last name required"); etLastName.requestFocus(); return; }
        // Thêm validation cho email nếu cần
        long age = -1; // Giá trị không hợp lệ ban đầu
        if (!TextUtils.isEmpty(ageStr)) {
            try {
                age = Long.parseLong(ageStr);
                if (age < 0 || age > 130) { // Giới hạn tuổi hợp lý
                    etAge.setError("Invalid age"); etAge.requestFocus(); return;
                }
            } catch (NumberFormatException e) {
                etAge.setError("Invalid age format"); etAge.requestFocus(); return;
            }
        } else {
            etAge.setError("Age required"); etAge.requestFocus(); return; // Yêu cầu nhập tuổi
        }

        // Lấy Gender
        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        String gender = "";
        if (selectedGenderId == R.id.radioButtonMale) gender = "Male";
        else if (selectedGenderId == R.id.radioButtonFemale) gender = "Female";
        else if (selectedGenderId == R.id.radioButtonOther) gender = "Other";
        else { showToast("Please select gender"); return; } // Yêu cầu chọn giới tính

        // Bắt đầu quá trình lưu
        setLoadingState(true);

        // Tạo Map dữ liệu để cập nhật Firestore
        final Map<String, Object> profileUpdates = new HashMap<>();
        profileUpdates.put("firstName", firstName);
        profileUpdates.put("lastName", lastName);
        profileUpdates.put("email", email); // Cập nhật email (cẩn thận nếu dùng email làm login)
        profileUpdates.put("age", age); // Lưu dưới dạng số (Long)
        profileUpdates.put("gender", gender);
        // Không cần thêm các trường không thay đổi (như registrationDate) vì dùng merge

        // --- Upload ảnh nếu có ảnh mới ---
        if (selectedImageUri != null) {
            Log.d(TAG, "Uploading new profile image...");
            profileImageStorageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        Log.d(TAG, "Image uploaded successfully. Getting download URL...");
                        profileImageStorageRef.getDownloadUrl()
                                .addOnSuccessListener(downloadUri -> {
                                    Log.d(TAG, "Download URL obtained: " + downloadUri);
                                    profileUpdates.put("profileImageUrl", downloadUri.toString()); // Thêm URL ảnh vào map
                                    updateFirestoreDocument(profileUpdates); // Cập nhật Firestore với cả ảnh
                                }).addOnFailureListener(e -> {
                                    setLoadingState(false);
                                    Log.e(TAG, "Failed to get download URL", e);
                                    showToast("Lỗi lấy URL ảnh: " + e.getMessage());
                                });
                    })
                    .addOnFailureListener(e -> {
                        setLoadingState(false);
                        Log.e(TAG, "Failed to upload profile image", e);
                        showToast("Lỗi tải ảnh lên: " + e.getMessage());
                    });
        } else {
            // --- Không có ảnh mới, cập nhật Firestore ngay ---
            Log.d(TAG, "No new image selected. Updating Firestore directly...");
            updateFirestoreDocument(profileUpdates);
        }
    }

    // Hàm thực hiện cập nhật Firestore
    private void updateFirestoreDocument(Map<String, Object> data) {
        userDocRef.set(data, SetOptions.merge()) // Sử dụng merge để chỉ cập nhật các trường trong map
                .addOnSuccessListener(aVoid -> {
                    setLoadingState(false);
                    Log.d(TAG, "Firestore document updated successfully.");
                    showToast("Cập nhật hồ sơ thành công!");
                    finish(); // Đóng màn hình sau khi cập nhật
                })
                .addOnFailureListener(e -> {
                    setLoadingState(false);
                    Log.e(TAG, "Error updating Firestore document", e);
                    showToast("Lỗi cập nhật hồ sơ: " + e.getMessage());
                });
    }

    // --- Tiện ích ---
    private void setLoadingState(boolean isLoading) {
        // Hiện/ẩn ProgressBar và disable/enable nút Save
        // if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!isLoading);
        // Có thể disable các input khác khi đang loading
        etFirstName.setEnabled(!isLoading);
        etLastName.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etAge.setEnabled(!isLoading);
        rgGender.setEnabled(!isLoading); // Có thể cần lặp qua các RadioButton
        for(int i = 0; i < rgGender.getChildCount(); i++){
            ((RadioButton)rgGender.getChildAt(i)).setEnabled(!isLoading);
        }
        ivProfileImage.setEnabled(!isLoading); // Cho phép/không cho phép đổi ảnh khi đang lưu
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
