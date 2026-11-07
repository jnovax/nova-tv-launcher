# Thiết kế Kiến trúc: Monorepo Hệ sinh thái TV & Dynamic Time Pill trong Kid Mode

* **Ngày lập:** 2026-11-07
* **Dự án:** `nova-tv-launcher`
* **Ứng dụng thành phần:**
  * `apps/launcher`: Apple TV tvOS 18 Home Launcher (`com.appletv.launcher`)
  * `apps/parental-control`: FamilyTime TV Parental Control & Quiz Break Service (`vn.familytv.parentalcontrol`)
  * `core/contract`: Shared IPC Contract & Data Models giữa Launcher và Parental Control
* **Thiết bị kiểm thử mục tiêu:** TCL Smart TV Pro (`192.168.1.7:5555`, Android 14)

---

## 1. Mục tiêu & Bối cảnh (Background & Goals)

### 1.1. Bối cảnh
Trước đây, `nova-tv-launcher` và `tv-parental-control` là hai kho mã nguồn (repository) nằm độc lập trên máy:
* `nova-tv-launcher` chịu trách nhiệm làm Launcher mặc định, hiển thị thanh Dock Apple TV tvOS, quản lý wallpaper và lọc ứng dụng theo chế độ Kid/Parent.
* `tv-parental-control` chịu trách nhiệm đếm thời gian sử dụng màn hình (Screen Time Engine), xác thực phụ huynh qua Google Authenticator TOTP và hiển thị màn hình Quiz Break giải đố.

Tuy nhiên, việc duy trì 2 repo tách rời gây ra:
* Trùng lặp mã nguồn các hằng số IPC (Action Intent, Extra key, ContentProvider URI).
* Thao tác build và deploy thủ công tốn thời gian, dễ lệch phiên bản giữa 2 ứng dụng.
* Phụ huynh và trẻ em khi ở màn hình chính trong Kid Mode không nắm được thời gian còn lại trước khi bài Quiz xuất hiện.

### 1.2. Mục tiêu kỹ thuật
1. **Chuyển đổi thành Monorepo tiêu chuẩn Gradle Multi-Project**:
   * Giữ `nova-tv-launcher` làm thư mục gốc (Root Monorepo), bảo toàn commit history.
   * Chuyển `app/` hiện tại thành `apps/launcher`.
   * Đưa kho mã nguồn `tv-parental-control` vào `apps/parental-control`.
   * Tạo module dùng chung `core/contract` chứa toàn bộ hằng số IPC và helper logic.
2. **Linh hoạt trong Build & Deploy (Selective Build/Deploy)**:
   * Có thể build riêng biệt từng app hoặc build toàn bộ chỉ với 1 câu lệnh.
   * Bổ sung `./tools/deploy.sh` hỗ trợ deploy riêng `launcher`, `parental-control` hoặc `all`.
3. **Hiển thị Thời gian Còn lại trong Kid Mode (Dynamic Time Pill)**:
   * Trên Top Bar của Launcher, nút `btnMode` tự động hiển thị thời gian còn lại dạng ngắn gọn: `[ 🧒 12m ]` hoặc `[ 🧒 45s ]`.
   * Chuyển màu trực quan: Trắng (> 5m), Vàng (3 - 5m), Đỏ (< 3m) chuẩn Apple TV.
   * Vòng đời tối ưu: Chỉ đếm khi Launcher ở `RESUMED`, tự động ngắt khi mở app hoặc tắt màn hình để đảm bảo zero-lag cho TV.

---

## 2. Kiến trúc Monorepo & Cấu trúc Thư mục

```
nova-tv-launcher/ (Monorepo Root)
├── apps/
│   ├── launcher/              # ID: com.appletv.launcher (tvOS 18 Launcher)
│   │   ├── src/
│   │   └── build.gradle
│   └── parental-control/      # ID: vn.familytv.parentalcontrol (Parental Control Service)
│       ├── src/
│       └── build.gradle.kts
├── core/
│   └── contract/              # Pure Java/Kotlin Library: IPC contract & models
│       ├── src/main/java/com/appletv/contract/
│       │   ├── ModeContract.java
│       │   ├── StatusProviderContract.java
│       │   └── TimeFormatter.java
│       └── build.gradle
├── tools/                     # Scripts quản trị thiết bị, backup, audit và deploy
│   ├── deploy.sh              # 1-Click selective deploy: launcher | parental-control | all
│   ├── apply.sh
│   ├── audit.sh
│   └── restore.sh
├── build.gradle               # Root build script
├── settings.gradle            # include ':apps:launcher', ':apps:parental-control', ':core:contract'
└── gradle.properties
```

---

## 3. Quản lý Build & Triển khai Độc lập (Selective Build & Deploy)

### 3.1. Các lệnh Gradle độc lập
| Mục tiêu | Lệnh thực thi | Đầu ra APK |
|---|---|---|
| **Chỉ build Launcher** | `./gradlew :apps:launcher:assembleRelease` | `apps/launcher/build/outputs/apk/release/app-release.apk` |
| **Chỉ build Parental Control** | `./gradlew :apps:parental-control:assembleRelease` | `apps/parental-control/build/outputs/apk/release/app-release.apk` |
| **Chỉ test Launcher** | `./gradlew :apps:launcher:testDebugUnitTest` | Báo cáo test Launcher |
| **Chỉ test Parental Control** | `./gradlew :apps:parental-control:testDebugUnitTest` | Báo cáo test Parental Control |
| **Build toàn bộ Monorepo** | `./gradlew assembleRelease` | Cả 2 APK |

### 3.2. Script Triển khai Độc lập `./tools/deploy.sh`
Cú pháp lệnh:
```bash
./tools/deploy.sh <target> [TV_IP]
```
Trong đó `<target>` gồm:
* `launcher`: Build và cài đặt riêng `com.appletv.launcher`.
* `parental-control`: Build và cài đặt riêng `vn.familytv.parentalcontrol`.
* `all`: Build và cài đặt cả 2 ứng dụng đồng thời.

---

## 4. Module `core/contract` & Giao thức IPC

### 4.1. `ModeContract`
* `MODE_KID = "KID"`
* `MODE_PARENT = "PARENT"`
* `ACTION_MODE_CHANGED = "vn.familytv.parentalcontrol.ACTION_MODE_CHANGED"`
* `ACTION_AUTHENTICATE = "vn.familytv.parentalcontrol.ACTION_AUTHENTICATE"`
* `EXTRA_MODE = "extra_mode"`
* `EXTRA_ALLOWED_PACKAGES = "extra_allowed_packages"`
* `EXTRA_REMAINING_SECONDS = "extra_remaining_seconds"`

### 4.2. `StatusProviderContract`
* Authority: `vn.familytv.parentalcontrol.provider`
* Content URI: `content://vn.familytv.parentalcontrol.provider/status`
* Columns:
  * `COLUMN_CURRENT_MODE`: "KID" hoặc "PARENT"
  * `COLUMN_REMAINING_SECONDS`: Số giây còn lại trước Quiz Break (integer)
  * `COLUMN_SESSION_LIMIT_SECONDS`: Giới hạn chu kỳ (mặc định 900 giây = 15 phút)
  * `COLUMN_WHITELIST_PACKAGES`: Danh sách package được cấp phép (phân cách bằng dấu phẩy)

### 4.3. `TimeFormatter`
Cung cấp phương thức thuần tính chuyển đổi số giây sang chuỗi hiển thị ngắn gọn:
```java
public static String formatShortRemaining(long seconds) {
    if (seconds <= 0) return "0s";
    if (seconds < 60) return seconds + "s";
    long minutes = (seconds + 59) / 60; // Làm tròn lên phút tiếp theo
    return minutes + "m";
}
```

---

## 5. Thiết kế Dynamic Time Pill trên Launcher (`MainActivity`)

### 5.1. Hiển thị UI trên Top Bar (`btnMode`)
* **Chế độ Kid Mode**:
  - Chuỗi hiển thị: `[ 🧒 12m ]` hoặc `[ 🧒 45s ]`.
  - Màu sắc:
    - Khi thời gian còn `> 300s` (> 5m): Màu trắng tiêu chuẩn (`#FFFFFF`).
    - Khi thời gian còn `120s – 300s` (2m – 5m): Màu vàng (`#FFD60A`).
    - Khi thời gian còn `< 120s` (< 2m): Màu đỏ (`#FF453A`).
* **Chế độ Phụ huynh (Parent Mode)**:
  - Chuỗi hiển thị: `[ 👨‍👩‍👧 Phụ huynh ]` (màu trắng, không đếm ngược).

### 5.2. Quản lý Vòng đời & Tối ưu hiệu năng
* **Truy vấn trạng thái**:
  - Khi `onResume()`: Launcher query ContentProvider để nhận số giây chính xác tại thời điểm quay lại màn hình chính.
  - Lắng nghe Broadcast `ACTION_MODE_CHANGED`: Cập nhật lại ngay lập tức khi Parental Control phát broadcast (khi đổi chế độ hoặc reset giờ).
* **Bộ đếm Ticker**:
  - Sử dụng Android `Handler` với `postDelayed`.
  - Tần suất:
    - Nếu còn `> 60s`: Cập nhật mỗi 30 giây (giảm tải CPU).
    - Nếu còn `< 60s`: Cập nhật mỗi 1 giây (đếm ngược trực tiếp `45s`, `44s`...).
  - Tự động dừng (`removeCallbacks`) trong `onPause()` để đảm bảo khi trẻ đang xem YouTube hoặc TV tắt màn hình, Launcher hoàn toàn không tốn CPU.

---

## 6. Kế hoạch Kiểm thử & Xác minh (Verification Plan)

1. **Unit Tests**:
   - `core/contract`: Test logic của `TimeFormatterTest` (các mốc thời gian 0s, 30s, 60s, 720s, làm tròn phút).
   - `apps/launcher`: Test mapping của ModeManager với StatusProviderContract, test chu kỳ đếm và cập nhật text màu của `btnMode`.
   - `apps/parental-control`: Test ContentProvider trả về đúng schema và số giây còn lại.
2. **Build Verification**:
   - Chạy `./gradlew :apps:launcher:assembleRelease` -> PASS.
   - Chạy `./gradlew :apps:parental-control:assembleRelease` -> PASS.
   - Chạy `./gradlew assembleRelease` -> PASS.
3. **On-Device Verification**:
   - Chạy `./tools/deploy.sh all 192.168.1.7:5555`.
   - Chụp ảnh màn hình thực tế trên TV để xác nhận Top Bar hiển thị `[ 🧒 15m ]` / `[ 🧒 12m ]`.
   - Chuyển chế độ sang Parent Mode bằng mã TOTP, xác nhận nút chuyển sang `[ 👨‍👩‍👧 Phụ huynh ]`.
