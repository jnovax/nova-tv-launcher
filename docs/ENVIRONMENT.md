# Build & Development Environment Specification

Tài liệu đặc tả môi trường phát triển và cấu hình đường dẫn cho **Apple TV Launcher**.

---

## 🛠️ Cấu hình biến môi trường chuẩn (Environment Variables)

Hệ thống sử dụng các đường dẫn cố định đã cài đặt trên môi trường Linux:

```bash
# Java Development Kit (OpenJDK 17.0.21)
export JAVA_HOME=/opt/jdk17

# Android SDK
export ANDROID_HOME=/opt/android-sdk

# System PATH
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
```

---

## 📁 Cấu hình Project (`local.properties`)

Tệp `local.properties` tại thư mục gốc dự án phải trỏ đúng đến Android SDK:

```properties
sdk.dir=/opt/android-sdk
```

---

## 🚀 Kiểm tra môi trường & Lệnh biên dịch

### 1. Kiểm tra phiên bản Java và Android SDK
```bash
$JAVA_HOME/bin/java -version
# OpenJDK Runtime Environment (build 17.0.21+...)

adb version
# Android Debug Bridge version 1.0.41...
```

### 2. Biên dịch Release APK
```bash
./gradlew assembleRelease
```
Binary APK sau khi biên dịch nằm tại:
`app/build/outputs/apk/release/app-release.apk`
