const admin = require('firebase-admin');
const { Timestamp } = require('firebase-admin/firestore'); // Import Timestamp

// *** THAY ĐỔI ĐƯỜNG DẪN ĐẾN FILE SERVICE ACCOUNT KEY CỦA BẠN ***
const serviceAccount = require('./waterreminder2004-firebase-adminsdk-fbsvc-61b8c1d44d.json');

admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    // databaseURL không cần thiết khi chỉ dùng Firestore, nhưng để cũng không sao
    // databaseURL: "https://waterreminder2004-default-rtdb.firebaseio.com"
  });

// Khởi tạo Firestore
const db = admin.firestore(); // Đảm bảo bạn đang dùng Firestore

// *** THAY ĐỔI USER ID BẠN MUỐN THÊM DỮ LIỆU ***
const userId = 'PrfcBbRvmnX7VRd8yZWezDlLX022'; // Hoặc userId khác

// --- Cấu hình dữ liệu Test ---
const numberOfDays = 99; // Số ngày muốn tạo
const endDate = new Date(); // Ngày kết thúc là hôm nay
const startDate = new Date();
startDate.setDate(endDate.getDate() - (numberOfDays - 1)); // Ngày bắt đầu

console.log(`Generating test data for user: ${userId}`);
console.log(`From ${startDate.toISOString().split('T')[0]} to ${endDate.toISOString().split('T')[0]}`);

// Mảng chứa các promise ghi dữ liệu
const writePromises = [];

let currentDate = new Date(startDate);

while (currentDate <= endDate) {
    const dateString = currentDate.toISOString().split('T')[0]; // Định dạng YYYY-MM-DD

    // *** SỬA Ở ĐÂY: Dùng .doc() thay vì .document() ***
    const dailyLogRef = db.collection('water_tracker').doc(userId) // Sửa thành doc()
                          .collection('daily_logs').doc(dateString); // Sửa thành doc()

    // Tạo dữ liệu mẫu
    const totalWater = 500 + Math.floor(Math.random() * 2001); // Random 500-2500
    const dailyGoal = 2100 + Math.floor(Math.random() * 500);
    const lastAddedAmount = 200 + Math.floor(Math.random() * 301);
    const lastAddedTimestamp = Timestamp.now(); // Timestamp hiện tại
    const nextReminderTimestamp = Timestamp.fromMillis(0); // Giá trị mặc định

    const data = {
        dailyGoal,
        totalWater,
        lastAddedAmount,
        lastAddedTimestamp,
        nextReminderTimestamp,
    };

    // Thêm promise vào mảng
    // Dùng set với merge để ghi đè nếu document đã tồn tại
    writePromises.push(
        dailyLogRef.set(data, { merge: true })
            .then(() => console.log(`Successfully wrote data for: ${dateString}`))
            .catch((error) => console.error(`Error writing data for ${dateString}:`, error))
    );

    // Tăng ngày lên 1
    currentDate.setDate(currentDate.getDate() + 1);
}

// Đợi tất cả các promise ghi hoàn thành
Promise.all(writePromises)
    .then(() => {
        console.log('Test data generation complete.');
        // process.exit(); // Có thể bỏ comment dòng này nếu muốn script tự thoát
    })
    .catch((error) => {
        console.error('Error during bulk write:', error);
    });