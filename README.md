# 📖 Folio Reader

A clean, distraction-free e-book reader for Android designed to deliver a smooth, customizable reading experience. Build and download the latest APK directly from this repository using **GitHub Actions**—no local Android Studio setup required.

[![Build Android APK](https://github.com)](https://github.com)
![Platform](https://shields.io)
![Language](https://shields.io)
![License](https://shields.io)

---

## ✨ Features

* **Multi-Format Support:** Smoothly read EPUB, PDF, and local e-book files stored on your device.
* **Distraction-Free Interface:** Full-screen reading mode where menus and controls stay hidden until you tap the screen.
* **Reading Progress Sync:** Automatically remembers the exact page and chapter where you left off for every book.
* **Customizable Themes:** Switch between Light, Sepia, and Dark/Night modes for comfortable reading day or night.
* **Typography Controls:** Adjust font styles, text size, line spacing, and screen margins to fit your preference.
* **Bookmarks & Highlights:** Save important passages, add personal notes, and jump through chapters using the Table of Contents.
* **Offline & Private:** Works 100% offline with no ads, no tracking, and no account required.

---

## 📸 Screenshots

| Library View | Reader Mode | Dark Theme & Settings |
| :---: | :---: | :---: |
| ![Library](docs/screenshots/library.png) | ![Reader](docs/screenshots/reader.png) | ![Settings](docs/screenshots/settings.png) |

*(Optional: Upload your app screenshots to `docs/screenshots/` to display them here, or remove this section until you add them.)*

---

## 🚀 Building & Downloading the APK via GitHub Actions

You do not need to install Android Studio or Gradle on your computer to get an installable APK. Every push or manual trigger builds the APK automatically in the cloud.

### Method 1: Download from an Existing Build
1. Go to the **[Actions](https://github.com)** tab of this repository.
2. Click on the latest successful workflow run.
3. Scroll down to the **Artifacts** section at the bottom of the page.
4. Click the APK artifact (e.g., **app-debug** or **FolioReader-APK**) to download the `.zip` archive.
5. Extract the `.zip` file and install the `.apk` on your Android device.

### Method 2: Trigger a Fresh Build Manually
1. Navigate to the **[Actions](https://github.com)** tab.
2. Select the APK build workflow from the left sidebar.
3. Click the **Run workflow** dropdown button on the right, select the `main` branch, and click **Run workflow**.
4. Once the build finishes (usually 2–4 minutes), download the generated APK from **Artifacts**.

---

## 🛠️ Local Development Setup

If you want to modify the code and build the app locally:

### Prerequisites
* **Android Studio** (Ladybug or newer recommended)
* **JDK 17**
* **Android SDK** (API Level 24+ / Android 7.0 Nougat or higher)

### Steps to Build Locally
1. **Clone the repository:**
   ```bash
   git clone https://github.com
   cd Folio-reader
   ```
2. **Build using Gradle:**
   * **Linux / macOS:**
     ```bash
     ./gradlew assembleDebug
     ```
   * **Windows:**
     ```cmd
     gradlew.bat assembleDebug
     ```
3. **Locate the APK:**
   Find your compiled APK at:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## 🤝 Contributing & Feedback

Contributions, bug reports, and feature requests are welcome!
* Found a bug or have a suggestion? Open an issue on the **[Issues](https://github.com)** page.
* Want to contribute code?
  1. Fork the repository.
  2. Create your feature branch (`git checkout -b feature/AmazingFeature`).
  3. Commit your changes (`git commit -m 'Add some AmazingFeature'`).
  4. Push to the branch (`git push origin feature/AmazingFeature`).
  5. Open a **[Pull Request](https://github.com)**.

---

## 📄 License

Distributed under the **MIT License**. See `LICENSE` for more information.
