# Thiết kế Hệ thống: Chế độ Trẻ Em (Kid Mode) & Phụ Huynh (Parent Mode) với Google Authenticator và Bài kiểm tra định kỳ (Quiz Break)

* **Ngày lập:** 2026-11-06
* **Hệ thống liên quan:**
  * `nova-tv-launcher` (`com.appletv.launcher`) - TV Launcher giao diện tvOS Apple TV
  * `tv-parental-control` (`vn.familytv.parentalcontrol` / FamilyTime) - Dịch vụ giám sát, overlay và bảo vệ trẻ em
* **Thiết bị mục tiêu:** TCL 55C7K (Android 14, Board: `merak`, D-pad Remote)

---

## 1. Mục tiêu & Bài toán (Goals & Background)

### 1.1. Vấn đề thực tế
* Trẻ em khi sử dụng Android TV thường bị cuốn vào nội dung thụ động (YouTube, phim hoạt hình) hàng giờ liên tục, gây hại cho mắt và giảm khả năng tập trung.
* Các biện pháp kiểm soát của phụ huynh truyền thống (mã PIN cố định) dễ bị trẻ nhìn trộm remote hoặc thao tác phức tạp trên TV.
* Cần một giải pháp:
  1. **Tự động an toàn ngay khi mở TV:** Mặc định rơi vào Kid Mode (không cần thao tác).
  2. **Bảo mật tuyệt đối, hoạt động offline:** Phụ huynh đăng nhập qua Google Authenticator (TOTP 6 số) - không ai đoán trước được mã và không cần internet.
  3. **Ngắt quãng chủ động (Pomodoro Quiz Break):** Cứ mỗi 10–15 phút xem TV, một bài kiểm tra ngắn (toán hình ảnh, từ vựng tiếng Anh) sẽ xuất hiện đè lên màn hình, tự động dừng âm thanh/video nền để trẻ nghỉ mắt và tương tác tư duy trước khi được xem tiếp.
  4. **Tối ưu TopBar Launcher:** Chuyển nút Wallpaper từ TopBar vào trong Settings để giữ TopBar tối giản tvOS, nhường chỗ cho nút hiển thị chế độ Profile.

---

## 2. Kiến trúc Tổng thể & Phân tách trách nhiệm (Architecture & Separation of Concerns)

Kiến trúc tuân thủ triệt để nguyên lý Phân tách trách nhiệm (Separation of Concerns):

```
+--------------------------------------------------------------------------+
|                           Android TV (TCL 55C7K)                         |
|                                                                          |
|  +-------------------------------------+   Query (ContentProvider)       |
|  |           nova-tv-launcher          | <----------------------------+  |
|  |       (com.appletv.launcher)        |                              |  |
|  |                                     |   Listen (BroadcastReceiver) |  |
|  | - TopBar: Mode Pill [🧒/👨‍👩‍👧]           | <----------------------------+  |
|  | - Adaptive App Filter (Whitelist)   |                              |  |
|  | - Settings Lockout in Kid Mode      |   Explicit Intent (Auth)     |  |
|  | - Wallpaper button moved to Settings| ---------------------------> |  |
|  +-------------------------------------+                              |  |
|                                                                       |  |
|  +-----------------------------------------------------------------+  |  |
|  |                      tv-parental-control                        |  |  |
|  |                 (vn.familytv.parentalcontrol)                   | -+  |
|  |                                                                       |
|  | - ScreenTime Timer (10-15 phút active screen time)                    |
|  | - Audio Focus Manager (AudioManager.requestAudioFocus -> PAUSE media)|
|  | - QuizBreakOverlay (TYPE_APPLICATION_OVERLAY full-screen D-pad)       |
|  | - Offline Quiz Bank (Toán tư duy, Tiếng Anh trẻ em)                   |
|  | - TOTP Engine (RFC 6238 Google Authenticator, offline 100%)           |
|  | - Session Manager (Auto-lock to Kid Mode on SCREEN_OFF / Reboot)     |
|  +-----------------------------------------------------------------------+
+--------------------------------------------------------------------------+
```

### 2.1. Trách nhiệm của `nova-tv-launcher`
* **Hiển thị thích ứng (Adaptive Presentation):**
  * TopBar: Bổ sung `btnMode` hiển thị `[ 🧒 Kid Mode ]` hoặc `[ 👨‍👩‍👧 Phụ huynh ]`.
  * TopBar Clean-up: Chuyển hoàn toàn `btnWallpaper` vào trong màn hình QuickSettings/LauncherSettings.
  * Lọc ứng dụng (App Whitelist): Khi ở `KID_MODE`, chỉ cho phép hiển thị các app đã được phê duyệt cho trẻ (YouTube Kids, SmartTube danh mục trẻ em, app học tập). Ẩn các app người lớn và trình duyệt web.
  * Khóa cài đặt: Khi ở `KID_MODE`, ẩn hoặc khóa `btnSettings`.
* **Kích hoạt xác thực:** Khi bấm `btnMode` hoặc cố vào Settings trong Kid Mode, gọi Explicit Intent sang `tv-parental-control`.

### 2.2. Trách nhiệm của `tv-parental-control`
* **Quản trị phiên (Session Manager):**
  * Mặc định an toàn (Fail-safe): Mọi lần khởi động lại hoặc tỉnh dậy từ Sleep (`ACTION_SCREEN_OFF`) đều đưa về `KID_MODE`.
  * Lưu trữ Secret Key mã hóa trong Android Keystore / EncryptedSharedPreferences.
* **Bộ đếm thời gian (Screen Time Engine):**
  * Tích lũy thời gian màn hình sáng trong `KID_MODE`.
  * Ngưỡng mặc định: 15 phút (cấu hình được: 10, 15, 20 phút).
* **Bài kiểm tra đè màn hình (Quiz Break Overlay):**
  * Tự động chiếm `AUDIOFOCUS_GAIN_TRANSIENT` để PAUSE/MUTE toàn bộ âm thanh và video nền (đã kiểm chứng trên YouTube/SmartTube).
  * Hiển thị Overlay toàn màn hình dạng tvOS Dark Glass, chặn mọi phím tắt của app bên dưới.
  * Quản lý ngân hàng câu hỏi trắc nghiệm D-pad (A/B/C/D) và xử lý kết quả đúng/sai.
  * Nút "Bố Mẹ mở khóa" cho phép nhập TOTP để bỏ qua khẩn cấp.
* **Xác thực TOTP (Google Authenticator RFC 6238):**
  * Tạo mã QR lúc Onboarding ban đầu.
  * Giao diện nhập 6 số dạng D-pad nhanh chóng trên TV.
  * Chấp nhận window `[-1, 0, +1]` (±30s) bù trừ sai lệch đồng hồ TV.

---

## 3. Hợp đồng Giao tiếp Liên ứng dụng (IPC Contract)

### 3.1. Explicit Intent kích hoạt Xác thực
* **Action:** `vn.familytv.parentalcontrol.ACTION_AUTHENTICATE`
* **Target Package:** `vn.familytv.parentalcontrol`
* **Target Activity:** `vn.familytv.parentalcontrol.auth.AuthActivity`
* **Extra Parameter:** `extra_target_mode = "PARENT"`

### 3.2. Broadcast cập nhật Trạng thái Hệ thống
* **Action:** `vn.familytv.parentalcontrol.ACTION_MODE_CHANGED`
* **Extras:**
  * `extra_mode`: String (`"KID"` | `"PARENT"`)
  * `extra_allowed_packages`: `ArrayList<String>` danh sách package ID được cấp phép trong Kid Mode.
* **Permissions:** Broadcast được bảo vệ bởi signature permission hoặc package-targeted broadcast để chống giả mạo.

### 3.3. Tra cứu tức thì qua ContentProvider
* **URI:** `content://vn.familytv.parentalcontrol.provider/status`
* **Columns:**
  * `current_mode`: TEXT (`"KID"` hoặc `"PARENT"`)
  * `screen_time_remaining_sec`: INTEGER (số giây còn lại trước Quiz Break)
  * `whitelist_packages`: TEXT (danh sách phân cách bằng dấu phẩy)

---

## 4. Kịch bản Vận hành Chi tiết (Lifecycle & Scenarios)

### Kịch bản 1: Mở TV buổi sáng
1. TV bật sáng từ chế độ Sleep (`ACTION_SCREEN_ON`).
2. `tv-parental-control` kích hoạt trạng thái `KID_MODE`.
3. `nova-tv-launcher` hiển thị giao diện:
   * TopBar: `[ 🧒 Kid Mode ]` | `[ Wi-Fi ]` | `[ Inputs ]` (Nút Settings bị ẩn).
   * App Grid: Chỉ hiện YouTube Kids, Scratch, Khan Academy Kids.
4. Trẻ chọn YouTube Kids xem phim hoạt hình.

### Kịch bản 2: Hết 15 phút xem TV (Quiz Break)
1. Timer của `tv-parental-control` đạt 15 phút.
2. `tv-parental-control` yêu cầu Audio Focus -> Video hoạt hình trên YouTube Kids lập tức tạm dừng (PAUSED), âm thanh tắt hoàn toàn.
3. Màn hình Overlay xuất hiện:
   * *"Đã xem 15 phút! Bé cùng giải đố để xem tiếp nhé:"*
   * Câu hỏi: *"Con vật nào kêu Gâu Gâu?"*
   * 4 ô đáp án to rõ: `[A: Mèo]` | `[B: Chó]` | `[C: Vịt]` | `[D: Cá]`.
4. Trẻ dùng remote chọn `[B: Chó]`:
   * Màn hình hiện hiệu ứng ngôi sao vui nhộn.
   * Overlay đóng lại, Audio Focus được nhả ra -> Video hoạt hình tiếp tục phát.
   * Timer 15 phút bắt đầu đếm chu kỳ tiếp theo.
5. *(Trường hợp trẻ trả lời sai)*: Màn hình hiển thị: *"Chưa đúng rồi! Bé hãy nhắm mắt nghỉ ngơi 30 giây rồi thử lại nhé!"* kèm đồng hồ đếm ngược 30 giây trước khi mở câu hỏi mới.

### Kịch bản 3: Phụ huynh muốn dùng TV (Chuyển Parent Mode)
1. Phụ huynh bấm phím Home về Launcher.
2. Di chuyển D-pad lên TopBar, bấm vào `[ 🧒 Kid Mode ]`.
3. Màn hình `AuthActivity` của `tv-parental-control` mở ra với 6 ô số.
4. Phụ huynh mở app Google Authenticator trên điện thoại (ví dụ thấy mã `582 109`), dùng remote nhập 6 số.
5. Xác thực thành công:
   * `tv-parental-control` chuyển trạng thái sang `PARENT_MODE`.
   * Bộ đếm 15 phút Quiz Break được tạm dừng.
   * `nova-tv-launcher` nhận tín hiệu chuyển sang Parent Mode: Hiển thị lại toàn bộ app (Netflix, SmartTube thường, Trình duyệt), hiển thị nút `[ Settings ]` và đổi nút trên TopBar thành `[ 👨‍👩‍👧 Phụ huynh ]`.
6. Phụ huynh xem phim xong tắt TV (`ACTION_SCREEN_OFF`): Hệ thống tự động xóa phiên Phụ huynh, lần bật tiếp theo tự động là Kid Mode.

---

## 5. Xử lý Ngoại lệ & Phòng chống Gian lận (Resilience & Anti-Cheat)

1. **Trẻ bấm phím Home / Back khi Overlay bài kiểm tra đang hiện:**
   * Overlay là `TYPE_APPLICATION_OVERLAY` với cờ `FLAG_NOT_TOUCH_MODAL` và intercept key event thông qua `AccessibilityService`, không thể bấm Back để thoát.
   * Nếu trẻ bấm nút Home về Launcher: Launcher phát hiện đang trong thời gian Quiz Break, không cho phép mở app khác cho đến khi bài kiểm tra được giải.
2. **Trẻ rút phích cắm / Tắt bật lại TV để reset:**
   * Khi TV khởi động lại, `tv-parental-control` ghi nhớ thời gian đã xem vào bộ nhớ đệm (hoặc mặc định vẫn vào Kid Mode ngay từ boot).
3. **Lệch giờ đồng hồ TV (Clock Drift):**
   * Thuật toán TOTP mở rộng cửa sổ so khớp 3 chu kỳ (trước 30s, hiện tại, sau 30s) đảm bảo dù đồng hồ TV lệch một chút vẫn nhận diện chuẩn xác mã từ điện thoại.
4. **Fallback khi `tv-parental-control` gặp sự cố hoặc chưa cài:**
   * Launcher kiểm tra nếu không tìm thấy package `vn.familytv.parentalcontrol` thì tự động hoạt động ở chế độ Standalone thông thường, không làm treo launcher.

---

## 6. Kế hoạch Kiểm thử & Bằng chứng (Testing & Verification Strategy)

1. **Unit Tests:**
   * `TOTPVerifierTest`: Kiểm tra sinh mã và kiểm tra mã khớp với Google Authenticator chuẩn RFC 6238.
   * `AppFilterTest`: Kiểm tra danh sách ứng dụng hiển thị chính xác theo từng mode.
   * `SessionTimeoutTest`: Kiểm tra logic hủy phiên khi nhận `ACTION_SCREEN_OFF`.
2. **On-Device Tests (TCL 55C7K):**
   * Kiểm tra chiếm Audio Focus làm dừng YouTube/SmartTube thực tế.
   * Kiểm tra hiển thị Overlay không bị tràn màn hình ở độ phân giải 4K (UI 1080p).
   * Kiểm tra độ mượt D-pad trên remote TCL.

---

## 7. Phân kỳ Triển khai (Decomposition into Implementation Plans)

Do hệ thống bao gồm hai ứng dụng độc lập, quá trình triển khai sẽ được chia làm 2 giai đoạn độc lập, có thể test riêng lẻ:

* **Giai đoạn 1 (Repo `nova-tv-launcher`):**
  1. Dọn dẹp TopBar: Chuyển nút Wallpaper từ TopBar vào trong QuickSettings/LauncherSettings.
  2. Bổ sung `btnMode` (`[🧒 Kid Mode]` / `[👨‍👩‍👧 Phụ huynh]`) trên TopBar cạnh `btnWifi`.
  3. Cài đặt bộ lọc ứng dụng thích ứng (Adaptive App Filter) trong `AppAdapter`: ẩn app người lớn khi ở Kid Mode.
  4. Lắng nghe Broadcast `ACTION_MODE_CHANGED` và mở Intent gọi `tv-parental-control` khi bấm `btnMode`.

* **Giai đoạn 2 (Repo `tv-parental-control`):**
  1. Module Xác thực TOTP (RFC 6238): Tạo mã QR Onboarding, màn hình nhập mã 6 số bằng D-pad.
  2. Module Quản lý Phiên (Session & State Provider): Lưu trữ state KID/PARENT, xuất ContentProvider và bắn Broadcast.
  3. Module Giám sát Thời gian & Ngắt quãng (ScreenTime Engine): Đếm giờ 15 phút, yêu cầu AudioFocus dừng media, hiển thị Overlay bài kiểm tra D-pad (Toán/Tiếng Anh).

