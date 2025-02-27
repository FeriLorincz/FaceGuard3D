# FaceGuard3D

<p align="center">
  <img src="logo.png" alt="FaceGuard3D Logo" width="200"/>
</p>

> Advanced 3D facial authentication and content protection for Android

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://www.android.com/)
[![SDK](https://img.shields.io/badge/SDK-21%2B-orange.svg)](https://developer.android.com/studio/releases/platforms)
[![OpenCV](https://img.shields.io/badge/OpenCV-4.5.1-yellow.svg)](https://opencv.org/)
[![TensorFlow](https://img.shields.io/badge/TensorFlow-2.5.0-red.svg)](https://www.tensorflow.org/)

## Overview

FaceGuard3D is an innovative Android security application that uses advanced 3D facial recognition to protect sensitive content on your device. Unlike traditional security methods, FaceGuard3D is designed to be discreet, with no visible "secure folder" that might raise questions. The app provides seamless protection for specific applications and files without drawing attention to their protected status.

**Key Differentiator**: FaceGuard3D's unique multi-angle facial authentication provides significantly stronger security than standard 2D facial recognition, making it nearly impossible to fool with photographs.

## 🌟 Key Features

- **3D Facial Authentication**: Uses multiple angles for higher security than traditional facial recognition
- **Selective App Protection**: Lock specific applications behind facial authentication
- **Discreet File Protection**: Hide sensitive files without suspicious secure folders
- **Multi-Face Detection**: Automatically hides protected content when multiple faces are detected
- **Adaptive Learning**: Adjusts to facial changes over time (beard growth, glasses, etc.)
- **Lighting Adaptation**: Works effectively in various lighting conditions
- **Backup Authentication**: Password fallback when facial authentication isn't possible

## 📱 Screenshots

<p align="center">
  <img src="screenshots/screen1.png" width="200" />
  <img src="screenshots/screen2.png" width="200" /> 
  <img src="screenshots/screen3.png" width="200" />
  <img src="screenshots/screen4.png" width="200" />
</p>

## 🔧 Technology Stack

- **Language**: Java for Android
- **Facial Recognition**: TensorFlow Lite, OpenCV, ML Kit
- **Camera**: CameraX API
- **Database**: Room Database
- **Security**: AES/GCM encryption, Android KeyStore
- **UI**: Material Design components

## 📋 Requirements

- Android 5.0 (API level 21) or higher
- Camera with autofocus capability
- 100MB free storage space
- 2GB RAM recommended

## ⚙️ Installation

1. Download the APK from the [Releases](https://github.com/FeriLorincz/faceguard3d/releases) page
2. Enable installation from unknown sources in your device settings
3. Install the APK
4. Follow the on-screen instructions to set up facial enrollment

**For Developers:**

```bash
# Clone the repository
git clone https://github.com/FeriLorincz/faceguard3d.git

# Open the project in Android Studio
# Install dependencies and build
```

## 🏗️ Architecture

FaceGuard3D follows a clean architecture approach with these main components:

- **Activities**: User interface components for enrollment, authentication, and settings
- **Services**: Background operations for app monitoring and facial detection
- **Managers**: Business logic for security, protection, and facial processing
- **Utils**: Helper classes for image processing, security, and file operations
- **Database**: Local storage for facial data and protected content information

## 📂 Project Structure

```
com.example.faceguard3d/
├── activities/
│   ├── MainActivity
│   ├── FaceEnrollmentActivity
│   ├── FaceAuthenticationActivity
│   ├── SecuritySettingsActivity
│   └── ...
├── services/
│   ├── FaceAuthService
│   ├── AppMonitoringService
│   └── ...
├── managers/
│   ├── FaceNetModelManager
│   ├── SecuritySettingsManager
│   └── ...
├── utils/
│   ├── ImageUtils
│   ├── SecurityUtils
│   └── ...
├── models/
│   ├── FacialFeatures
│   ├── HiddenContent
│   └── ...
└── database/
    ├── AppDatabase
    ├── entities/
    └── dao/
```

## 🔐 Security Features

FaceGuard3D implements several security measures:

- **3D Mapping**: Uses depth perception and multiple angles to prevent photo-based attacks
- **Liveness Detection**: Ensures the face is a real person, not a photo or video
- **AES/GCM Encryption**: Military-grade encryption for file protection
- **Secure Storage**: Protected data is stored in encrypted format
- **Multiple Face Detection**: Automatically hides sensitive content when unauthorized viewers are present

## 🚀 Future Development

- iOS version
- Cloud backup for facial profiles (encrypted)
- Enhanced ML model for better recognition in extreme conditions
- Enterprise deployment options
- Plugins for popular messaging applications

## ❓ FAQ

**Q: Is my facial data secure?**  
A: Yes, all facial data is stored locally on your device using encryption and is never transmitted to external servers.

**Q: Can FaceGuard3D be fooled with a photo?**  
A: No, the 3D facial mapping requires depth perception and multiple angles, making it resistant to photo-based attacks.

**Q: What happens if I change my appearance (grow a beard, get new glasses)?**  
A: The adaptive learning system will gradually adjust to changes in your appearance over time.

**Q: Will others know my files are protected?**  
A: No, FaceGuard3D is designed to be discreet. Protected files appear to be missing rather than visibly locked.

## 👥 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- OpenCV community for computer vision algorithms
- TensorFlow team for machine learning tools
- Android developer community
- All contributors and testers who helped make this project possible

---

<p align="center">
  <a href="mailto:ferilorincz12@gmail.com">Contact</a> •
  <a href="https://www.faceguard3d.com">Website</a> •
  <a href="https://www.linkedin.com/in/feri-lorincz/">LinkedIn</a>
</p>
