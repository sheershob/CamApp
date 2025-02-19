# CamApp - Android Camera Application

CamApp is a simple camera application for Android, built using Jetpack Compose and various other Android libraries. It allows users to capture photos and videos, switch between front and back cameras, scan QR codes, and record videos.

## Features

* **Photo Capture:** Capture high-resolution images using the device's camera.
* **Video Recording:** Record videos with audio enabled.
* **Camera Switching:** Easily switch between the front and back cameras.
* **QR Code and Barcode Scanning:** Scan QR and barcodes and process the data contained within them.
* **Flash Photography:** Flash photography is available for the back camera.

## Technologies Used

* **Android Studio:** Used Android Studio Ladybug in Kotlin language.
* **Jetpack Compose:** Modern toolkit for building native Android UI.
* **CameraX:** Support library for interacting with the camera.
* **ML Kit Barcode Scanning:** Library for scanning and decoding QR codes.

## Screenshots

**![Image](https://github.com/user-attachments/assets/7ccb02c7-b1aa-423e-ac77-251ab3433061)**

*These screenshots showcases the app's main camera interface, highlighting the various buttons.*

**![Image](https://github.com/user-attachments/assets/d28fbe33-daeb-450b-a038-9e1551fc3420)**

*The integrated QR code scanner quickly decodes the QR codes and provides the link. The barcode scanner similarly decodes the various encoding standards of barcodes.*

## Storage Stats

**![Image](https://github.com/user-attachments/assets/0d1e8862-1c19-42af-bcd2-7d7d96c3a889)**

*This chart illustrates the breakdown of the APK's contents, showing the size contribution of various components.  Optimizations have been implemented to minimize the app's storage by removing unnecessary libraries. It has a download size (compressed size) of 18.6 MB and apk size (entire package size) of 48.2 MB*

## Implementation Details

### Camera Features (CameraX)

CameraX is used for all camera-related operations, including preview, photo capture, and video recording. It provides a consistent and reliable API for working with different camera devices. The `PreviewView` composable is used to display the camera preview, and `ImageCapture` and `VideoCapture` are used for capturing photos and videos, respectively.  `ContextCompat.getMainExecutor()` is used to ensure that camera operations and UI updates are performed on the main thread.

### QR and Barcode Scanning (ML Kit Barcode Scanning)

ML Kit's Barcode Scanning API is integrated to enable QR code scanning functionality.  When the user initiates a QR code scan, `ImageCapture` takes a picture.  The captured image is then passed to the ML Kit barcode scanner, which processes the image and extracts the QR code data.  The `scanQRCode` function handles capturing the image specifically for QR scanning, and `processQRCode` uses the ML Kit library to decode the QR code. The `QRDialog` composable then displays the scanned information to the user.

### Video Recording with Audio

Video recording is implemented using CameraX's `VideoCapture` API. The `startRecording` function initializes and starts the video recording process, saving the video file to the device's external storage directory. The `stopRecording` function stops the ongoing recording. The `withAudioEnabled()` option ensures that audio is captured along with the video, provided the user has granted the necessary audio recording permission. The `FileOutputOptions` class is used to specify the location and filename for the saved video.  The `VideoRecordEvent` is used to track the recording state.

## Getting Started

1. Clone the repository: `git clone https://github.com/sheershob/CamApp.git`
2. Open the project in Android Studio.
3. Build and run the app on an Android device or emulator.

## Permissions

The app requires the following permissions:

* `CAMERA`: For accessing the device's camera.
* `RECORD_AUDIO`: For recording audio while capturing videos.

## Contributing

Contributions are welcome! Feel free to submit pull requests or open issues.
