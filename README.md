# Sticker Maker & Funny Emoji

Ứng dụng Android mẫu dùng Kotlin, Jetpack Compose và Supabase.

## Công nghệ

- Kotlin 2.2.21, JDK 21
- Jetpack Compose + Material 3
- Supabase Kotlin: Auth, PostgREST (Database), Storage
- Ktor Android client

## Cấu hình Supabase

1. Mở file `local.properties` ở thư mục gốc.
2. Điền hai giá trị lấy từ **Supabase Dashboard → Project Settings → API**:

```properties
SUPABASE_URL=https://your-project-ref.supabase.co
SUPABASE_PUBLISHABLE_KEY=your-publishable-or-anon-key
```

Chỉ dùng **publishable key** hoặc **anon key** trong ứng dụng Android. Không đặt
`service_role` key trong app. `local.properties` đã được Git bỏ qua.

## Chạy dự án

Mở thư mục này bằng Android Studio, chờ Gradle Sync xong, chọn emulator rồi Run.
Hoặc build từ terminal:

```bash
./gradlew assembleDebug
```

APK debug nằm tại `app/build/outputs/apk/debug/app-debug.apk`.

## Giao diện và chức năng mới

Đã bổ sung Create (ảnh, chữ, emoji, brush, outline, layers, undo/redo và bản nháp),
My Studio, chi tiết collection, preview/chia sẻ PNG và Settings theo các frame Figma đã đọc.
Chi tiết phạm vi và bằng chứng kiểm thử: [verification/README.md](verification/README.md).

Để bật lưu cloud trên server hiện tại, chạy [supabase/setup.sql](supabase/setup.sql)
**một lần trên project chưa có schema**, rồi bật **Anonymous Sign-Ins** trong Supabase Auth.
Nếu đã chạy migration trước đó, chỉ chạy các file còn thiếu trong `supabase/migrations/`.
App dùng phiên khách và RLS; mỗi người dùng chỉ đọc/ghi dữ liệu của mình.
Chưa chạy migration thì các thao tác cloud sẽ báo lỗi; bản nháp editor vẫn được giữ trên máy.

## Bổ sung ngày 08/09

- Home, Search và Category dùng chung ID sticker; tìm theo tên, danh mục và tag của catalogue mẫu.
- My Studio có Favorites. Yêu thích được lưu trên máy và xếp hàng đồng bộ lên Supabase;
  trạng thái lỗi có Retry, không hiển thị như đã lưu cloud khi request thất bại.
- Create có **Save PNG to device**, mở hộp chọn nơi lưu của Android và hoạt động offline.
- Tạo sticker từ collection sẽ lưu liên kết vào collection đó. Preview hỗ trợ xuất PNG
  và xóa sticker khỏi Studio cùng các collection sau khi xác nhận.
- Collection/preview đang mở được khôi phục khi Android tạo lại Activity.

Server đã có schema cũ: chạy thêm `supabase/migrations/202609080001_favorites.sql`.
Server mới: `supabase/setup.sql` đã bao gồm migration này.
Catalogue hiện vẫn là các mẫu Figma đóng gói trong app, chưa phải catalogue công khai từ server.
