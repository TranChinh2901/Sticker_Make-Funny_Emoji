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
