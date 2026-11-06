# Kế hoạch Triển khai: Kid/Parent Mode Adaptive UI & Tối ưu TopBar (Giai đoạn 1: Launcher)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Triển khai cơ chế phân định Chế độ Trẻ Em (Kid Mode) và Phụ Huynh (Parent Mode) trên `nova-tv-launcher`, tối ưu TopBar bằng cách đưa nút Wallpaper vào QuickSettings, thêm nút chuyển Profile `btnMode`, và áp dụng bộ lọc ứng dụng thích ứng (Adaptive App Filter).

**Architecture:** Launcher hoạt động theo mô hình Adaptive Presentation: mặc định rơi vào `KID_MODE`, ẩn app người lớn và khóa cài đặt. Launcher giao tiếp với `tv-parental-control` qua Explicit Intent (`ACTION_AUTHENTICATE`) và Broadcast (`ACTION_MODE_CHANGED`), tự động khóa về Kid Mode khi màn hình tắt (`ACTION_SCREEN_OFF`), đồng thời có fallback an toàn nếu `tv-parental-control` chưa được cài.

**Tech Stack:** Java 17, Android SDK 34 (Android TV Leanback, RecyclerView, BroadcastReceiver, ContentProvider client).

**Spec:** [docs/superpowers/specs/2026-11-06-kid-parent-mode-auth-quiz-design.md](file:///home/lamnh/projects/nova-tv-launcher/docs/superpowers/specs/2026-11-06-kid-parent-mode-auth-quiz-design.md)

## Global Constraints
- Target device: TCL 55C7K (Android 14, resolution 3840x2160, UI 1920x1080 320dpi, D-pad navigation).
- Fail-safe default: Mặc định luôn là `KID_MODE` khi boot hoặc tỉnh dậy từ Sleep.
- Zero accessibility lag: Không can thiệp hoặc chặn key volume/navigation ngoài lề.
- Full offline compatibility: Hoạt động không cần kết nối internet.

---

### Task 1: Dọn dẹp TopBar - Chuyển nút Wallpaper vào QuickSettings

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/java/com/appletv/launcher/MainActivity.java:336-377`
- Modify: `app/src/main/java/com/appletv/launcher/QuickSettingsActivity.java:27-34`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `MainActivity.showWallpaperPickerGrid()`
- Produces: `MainActivity.ACTION_SHOW_WALLPAPERS = "com.appletv.launcher.ACTION_SHOW_WALLPAPERS"`

- [ ] **Step 1: Thêm Action Intent trong `MainActivity.java` và mục Wallpaper vào `QuickSettingsActivity.java`**
  - Trong `MainActivity.java`: Khai báo hằng số `public static final String ACTION_SHOW_WALLPAPERS = "com.appletv.launcher.ACTION_SHOW_WALLPAPERS";` và trong `onNewIntent(Intent intent)` xử lý nếu action là `ACTION_SHOW_WALLPAPERS` thì gọi `showWallpaperPickerGrid()`.
  - Trong `QuickSettingsActivity.java`: Thêm hàng `Wallpaper`:
    ```java
    addRow(rows, R.drawable.ic_picture, R.string.wallpaper, 0,
            new Intent(this, MainActivity.class).setAction(MainActivity.ACTION_SHOW_WALLPAPERS));
    ```

- [ ] **Step 2: Xóa `btnWallpaper` khỏi `activity_main.xml` và cập nhật điều hướng D-pad**
  - Xóa phần tử `TextView android:id="@+id/btnWallpaper"` khỏi `activity_main.xml`.
  - Cập nhật biến `firstButton` trong `MainActivity.java` thành `btnInputs` (hoặc `btnMode` ở Task 4).

- [ ] **Step 3: Chạy test kiểm tra biên dịch và layout**
  Run: `JAVA_HOME=/opt/jdk17 ANDROID_HOME=/opt/android-sdk ./gradlew testDebugUnitTest`
  Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**
  ```bash
  git add app/src/main/res/layout/activity_main.xml app/src/main/java/com/appletv/launcher/MainActivity.java app/src/main/java/com/appletv/launcher/QuickSettingsActivity.java
  git commit -m "refactor(ui): move wallpaper button from top bar into quick settings"
  ```

---

### Task 2: Mô hình Trạng thái Chế độ (`ModeManager`) & Bộ lọc Whitelist

**Files:**
- Create: `app/src/main/java/com/appletv/launcher/ModeManager.java`
- Create: `app/src/test/java/com/appletv/launcher/ModeManagerTest.java`

**Interfaces:**
- Produces:
  - `ModeManager.MODE_KID = "KID"`
  - `ModeManager.MODE_PARENT = "PARENT"`
  - `ModeManager.ACTION_MODE_CHANGED = "vn.familytv.parentalcontrol.ACTION_MODE_CHANGED"`
  - `ModeManager.ACTION_AUTHENTICATE = "vn.familytv.parentalcontrol.ACTION_AUTHENTICATE"`
  - `boolean isKidMode(Context context)`
  - `void setMode(Context context, String mode)`
  - `boolean isAppAllowedInKidMode(String packageName, Set<String> customWhitelist)`

- [ ] **Step 1: Viết failing test `ModeManagerTest.java`**
  Tạo `app/src/test/java/com/appletv/launcher/ModeManagerTest.java` kiểm tra:
  - Mặc định là `MODE_KID`.
  - Package `com.google.android.youtube.tvkids` hoặc `org.smarttube.stable` được phép trong Kid Mode.
  - Package `com.android.settings` hoặc `com.google.android.tv.settings` bị chặn trong Kid Mode.
  - Khi mode là `MODE_PARENT`, mọi package đều được phép.

- [ ] **Step 2: Chạy test để xác nhận FAIL**
  Run: `JAVA_HOME=/opt/jdk17 ANDROID_HOME=/opt/android-sdk ./gradlew testDebugUnitTest --tests com.appletv.launcher.ModeManagerTest`
  Expected: FAIL (lớp `ModeManager` chưa tồn tại).

- [ ] **Step 3: Cài đặt `ModeManager.java`**
  Tạo `app/src/main/java/com/appletv/launcher/ModeManager.java` với:
  - SharedPreferences lưu `current_mode`, mặc định `"KID"`.
  - Hỗ trợ fallback default whitelist: YouTube Kids, SmartTube, Khan Academy Kids, Duolingo, VLC, TizenTube.
  - Phương thức `isAppAllowedInKidMode(String pkg, Set<String> whitelist)`.

- [ ] **Step 4: Chạy lại test để xác nhận PASS**
  Run: `JAVA_HOME=/opt/jdk17 ANDROID_HOME=/opt/android-sdk ./gradlew testDebugUnitTest --tests com.appletv.launcher.ModeManagerTest`
  Expected: PASS

- [ ] **Step 5: Commit**
  ```bash
  git add app/src/main/java/com/appletv/launcher/ModeManager.java app/src/test/java/com/appletv/launcher/ModeManagerTest.java
  git commit -m "feat(auth): implement ModeManager for Kid/Parent state and whitelist filtering"
  ```

---

### Task 3: Tích hợp Lọc Ứng dụng trong `AppAdapter`

**Files:**
- Modify: `app/src/main/java/com/appletv/launcher/AppAdapter.java:151-175`
- Create: `app/src/test/java/com/appletv/launcher/AppAdapterModeFilterTest.java`

**Interfaces:**
- Consumes: `ModeManager.isKidMode(Context)`, `ModeManager.isAppAllowedInKidMode(String, Set<String>)`

- [ ] **Step 1: Viết failing test `AppAdapterModeFilterTest.java`**
  Kiểm tra logic lọc package khi ở chế độ Kid Mode: nếu `ModeManager.isKidMode()` trả về `true`, các app không thuộc whitelist bị loại trừ khỏi danh sách tiles.

- [ ] **Step 2: Chạy test để xác nhận FAIL**
  Run: `JAVA_HOME=/opt/jdk17 ANDROID_HOME=/opt/android-sdk ./gradlew testDebugUnitTest --tests com.appletv.launcher.AppAdapterModeFilterTest`
  Expected: FAIL

- [ ] **Step 3: Cập nhật `AppAdapter.java`**
  Trong `AppAdapter.reload(List<Cards.Card> cards)`:
  Kiểm tra điều kiện:
  ```java
  boolean kidMode = ModeManager.isKidMode(ctx);
  // Trong vòng lặp ResolveInfo:
  if (kidMode && !ModeManager.isAppAllowedInKidMode(pkg, null)) continue;
  ```

- [ ] **Step 4: Chạy test để xác nhận PASS**
  Run: `JAVA_HOME=/opt/jdk17 ANDROID_HOME=/opt/android-sdk ./gradlew testDebugUnitTest --tests com.appletv.launcher.AppAdapterModeFilterTest`
  Expected: PASS

- [ ] **Step 5: Commit**
  ```bash
  git add app/src/main/java/com/appletv/launcher/AppAdapter.java app/src/test/java/com/appletv/launcher/AppAdapterModeFilterTest.java
  git commit -m "feat(launcher): filter leanback app grid based on Kid Mode status"
  ```

---

### Task 4: Nút Chế độ (`btnMode`) trên TopBar & Quản lý Phiên trong `MainActivity`

**Files:**
- Create: `app/src/main/res/drawable/ic_mode_kid.xml`
- Create: `app/src/main/res/drawable/ic_mode_parent.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/layout/activity_main.xml:196-235`
- Modify: `app/src/main/java/com/appletv/launcher/MainActivity.java`

**Interfaces:**
- Consumes: `ModeManager`, `tv-parental-control` Intent / Broadcast
- Produces: UI Pill `btnMode` trên TopBar, xử lý chuyển chế độ và auto-lock.

- [ ] **Step 1: Tạo icon Vector Drawable và chuỗi ngôn ngữ**
  - `ic_mode_kid.xml`: Vector icon khuôn mặt trẻ em / biểu tượng Kid.
  - `ic_mode_parent.xml`: Vector icon gia đình / người lớn.
  - `strings.xml`: Thêm `mode_kid`, `mode_parent`, `mode_switch_title`, `mode_switch_parent`, `mode_lock_kid`, `mode_parent_desc`, `tv_parental_control_missing`.

- [ ] **Step 2: Thêm `btnMode` vào `activity_main.xml`**
  Đặt `btnMode` ở vị trí đầu tiên trong `topButtons` LinearLayout:
  ```xml
  <TextView
      android:id="@+id/btnMode"
      style="@style/TopButton"
      android:text="@string/mode_kid"
      android:drawableStart="@drawable/ic_mode_kid"
      android:nextFocusDown="@+id/grid" />
  ```

- [ ] **Step 3: Xử lý tương tác D-pad & Broadcast trong `MainActivity.java`**
  - Khai báo `TextView btnMode`.
  - Hàm `updateModeUI()`: Cập nhật text, icon của `btnMode`, ẩn `btnSettings` khi ở `KID_MODE`.
  - Sự kiện click `btnMode`:
    - Nếu đang là `KID_MODE`: Gửi Intent `ACTION_AUTHENTICATE` sang `vn.familytv.parentalcontrol`. Nếu ứng dụng chưa cài đặt, hiển thị Dialog thông báo và tùy chọn nhập mã PIN tạm thời.
    - Nếu đang là `PARENT_MODE`: Hiển thị Dialog tùy chọn `[Khóa ngay về Kid Mode]`.
  - Đăng ký `BroadcastReceiver`:
    - Bắt `ACTION_MODE_CHANGED`: Đọc mode mới và gọi `updateModeUI()`, `reloadApps(true)`.
    - Bắt `Intent.ACTION_SCREEN_OFF`: Tự động gọi `ModeManager.setMode(this, ModeManager.MODE_KID)` để khóa ngay về Kid Mode cho lần bật sau.

- [ ] **Step 4: Chạy toàn bộ Unit Tests**
  Run: `JAVA_HOME=/opt/jdk17 ANDROID_HOME=/opt/android-sdk ./gradlew testDebugUnitTest`
  Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**
  ```bash
  git add app/src/main/res/drawable/ic_mode_*.xml app/src/main/res/values/strings.xml app/src/main/res/layout/activity_main.xml app/src/main/java/com/appletv/launcher/MainActivity.java
  git commit -m "feat(ui): add mode pill button and screen-off auto lock in MainActivity"
  ```

---

### Task 5: Kiểm thử Tổng thể & Biên dịch Release APK

**Files:**
- Toàn bộ codebase `nova-tv-launcher`

- [ ] **Step 1: Chạy toàn bộ Unit Tests**
  Run: `JAVA_HOME=/opt/jdk17 ANDROID_HOME=/opt/android-sdk ./gradlew test`
  Expected: All tests PASS.

- [ ] **Step 2: Biên dịch Release APK**
  Run: `JAVA_HOME=/opt/jdk17 ANDROID_HOME=/opt/android-sdk ./gradlew assembleRelease`
  Expected: APK sinh ra tại `app/build/outputs/apk/release/app-release-unsigned.apk` không có lỗi.

- [ ] **Step 3: Commit & Ghi nhận kết quả**
  ```bash
  git status
  ```
