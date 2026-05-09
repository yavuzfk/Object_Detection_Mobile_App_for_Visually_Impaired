# Object Detection for the Visually Impaired

> A real-time Android application that turns a smartphone camera into audio guidance — detecting objects on-device and announcing their identity and relative position through speech, designed entirely around non-visual interaction.

[![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)]()
[![Language](https://img.shields.io/badge/language-Java-orange?logo=java&logoColor=white)]()
[![ML Framework](https://img.shields.io/badge/ML-TensorFlow%20Lite-FF6F00?logo=tensorflow&logoColor=white)]()
[![Project](https://img.shields.io/badge/type-Final%20Year%20Project-blue)]()

---

## 📽️ Demo

https://user-images.githubusercontent.com/17355685/192622072-f28219b8-7284-474c-b1e2-3a542a61a07b.mp4

---

## Overview

This project addresses a concrete accessibility problem: helping visually impaired users locate everyday objects in their environment without relying on sight. The application uses on-device computer vision to detect objects in real time, then communicates both the **identity** and **spatial location** of each object through Android's Text-to-Speech engine — entirely offline, with no cloud dependency.

Developed as my graduation project at Yeditepe University, Department of Computer Engineering (2022).

## Key Features

- **Real-time object detection** using TensorFlow Lite, optimized for on-device inference
- **Spatial awareness** — detected objects are described with their relative position in the camera frame (e.g., *"chair on the left"*, *"bottle in front"*)
- **Voice-first interaction** — every output flows through TextToSpeech; the UI itself is designed for non-visual use
- **Offline architecture** — no internet connection required, all inference runs locally
- **Audio onboarding** — application instructions are announced to the user, removing the need for visual tutorials

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Platform | Android (native) |
| Language | Java |
| ML Inference | [TensorFlow Lite](https://www.tensorflow.org/lite) |
| Detection Model | [e.g., MobileNet SSD v2 — fill in actual model] |
| Audio Output | Android TextToSpeech API |
| Camera | CameraX / Camera2 API |
| IDE | Android Studio |

## How It Works

1. **Frame capture** — The camera streams live frames to the inference pipeline.
2. **Object detection** — Each frame passes through a quantized TFLite model, returning bounding boxes and class labels.
3. **Spatial mapping** — Detected bounding boxes are mapped to spatial regions (left / center / right) of the camera frame to derive directional cues.
4. **Speech synthesis** — Detected objects with their positions are converted into natural-language phrases and announced via TextToSpeech.

The pipeline runs continuously, prioritizing low-latency feedback over absolute detection accuracy — a deliberate tradeoff for the use case.


## Project Report

A detailed technical report documents the full engineering process — problem analysis, model selection criteria, system architecture, UML diagrams, and test results.

![image](https://user-images.githubusercontent.com/17355685/192619450-87c6484b-5649-4580-bb98-084f93a6221c.png)

[📄 Full Report (PDF)](./OBJECT_DETECTION_APP.pdf) <!-- update with actual filename if different -->

## Getting Started

### Prerequisites
- Android Studio Arctic Fox or newer
- Android SDK 21+ (Android 5.0 Lollipop)
- A physical Android device (recommended; emulators have limited camera support)

### Build & Run
```bash
git clone https://github.com/yavuzfk/[repo-name].git
cd [repo-name]
# Open the project in Android Studio
# Connect an Android device via USB with developer mode enabled
# Click Run
```

## Future Improvements

- Improved spatial localization using monocular depth estimation
- Distance-to-object estimation via reference object calibration
- Multi-language TextToSpeech support
- Migration to a more recent detection model (e.g., EfficientDet-Lite, MediaPipe Object Detector)

## Academic Context

Final Year Graduation Project — Yeditepe University, Department of Computer Engineering (2022). The project explores accessibility-driven mobile engineering: applying computer vision techniques to create assistive technology for users with visual impairments.

## Author

**Yavuz Furkan Karabıyık**
Software Engineer · [LinkedIn](https://linkedin.com/in/yavuzfk) · [GitHub](https://github.com/yavuzfk)
