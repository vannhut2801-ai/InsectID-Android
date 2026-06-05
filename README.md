# InsectID - Ứng dụng nhận diện côn trùng gây hại nông nghiệp

Ứng dụng Android giúp nông dân và người làm nông nghiệp nhận diện nhanh các loài côn trùng gây hại, tra cứu thông tin chi tiết và nhận tư vấn xử lý thông qua AI.
## Công nghệ sử dụng

- **Kotlin** (Ngôn ngữ chính)
- **TensorFlow Lite** (Nhận diện côn trùng offline với 102 loài)
- **Firebase Auth** (Đăng nhập Email/Password + Google Sign-In)
- **Firebase Firestore** (Cơ sở dữ liệu thông tin côn trùng, lịch sử, bài viết)
- **Firebase Storage** (Lưu trữ ảnh)
- **Google Maps SDK** (Bản đồ dịch hại với marker clustering)
- **Gemini AI** (Chatbot tư vấn nông nghiệp)
- **Glide** (Tải và cache ảnh)
- **Material Design** (Giao diện)

## Tính năng chính

1. **Xác thực người dùng** - Đăng nhập/đăng ký qua Email + Google Sign-In
2. **Nhận diện côn trùng** - Chụp ảnh hoặc chọn từ thư viện, AI phân loại 103 loài
3. **Kết quả chi tiết** - Tên khoa học, hình thái, phân bố, tác hại, biện pháp phòng trừ
4. **Vòng đời côn trùng** - Hiển thị các giai đoạn sinh trưởng với hình ảnh minh họa
5. **Bản đồ dịch hại** - Google Maps hiển thị marker tần suất xuất hiện theo khu vực
6. **Chat AI (Gemini)** - Trợ lý ảo trả lời câu hỏi về nông nghiệp và côn trùng
7. **Gợi ý theo vùng** - Hiển thị côn trùng phổ biến dựa trên vị trí GPS
8. **Danh mục côn trùng** - 9 danh mục với ~95 loài kèm ảnh minh họa
9. **Lịch sử nhận diện** - Lưu và xem lại các lần nhận diện trước đây
10. **Đóng góp cộng đồng** - Chia sẻ kết quả, phản hồi đúng/sai để cải thiện AI
11. **Chia sẻ kết quả** - Gửi kết quả nhận diện qua các ứng dụng khác

## Yêu cầu hệ thống

- Android 8.0 (API 26) trở lên
- Android Studio (khuyến nghị phiên bản mới nhất)

## Hướng dẫn build

### 1. Clone project

```bash
git clone <repo-url>
cd khoaluan
```

### 2. Cấu hình API Keys

Tạo file `local.properties` trong thư mục gốc của project với nội dung:

```properties
sdk.dir=<đường-dẫn-Android-SDK>
MAPS_API_KEY=<Google-Maps-API-Key-của-bạn>
GEMINI_API_KEY=<Gemini-API-Key-của-bạn>
```
### 3. Thêm Firebase config

Lấy file `google-services.json` từ Firebase Console và đặt vào thư mục `app/`.
### 4. Build và chạy

Mở project bằng Android Studio, đồng bộ Gradle và chạy ứng dụng trên thiết bị hoặc máy ảo.

