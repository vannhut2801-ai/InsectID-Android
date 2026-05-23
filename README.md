🔧 Installation & Local Setup

- Prerequisites

- Android Studio Jellyfish (or newer)

- Android SDK 34+ (Target SDK 34, Min SDK 26)

Configuration Steps
1. Clone the Repository:
```
git clone [https://github.com/vannhut2801-ai/InsectID-Android.git](https://github.com/vannhut2801-ai/InsectID-Android.git)
cd InsectID-Android
```
2. Add Firebase Credentials:
   
   Place your downloaded google-services.json file directly inside the local /app directory:
```
InsectID-Android/app/google-services.json
```
3. Configure Local Model Asset:

   Ensure your quantized TensorFlow Lite model file (insect_classifier.tflite) and your text labels file (labels.txt) match perfectly inside the production assets folder:
```
InsectID-Android/app/src/main/assets/insect_classifier.tflite
InsectID-Android/app/src/main/assets/labels.txt
```
4. Sync and Run:

   Sync the project with Gradle Files inside Android Studio, connect your physical device via USB debugging, and execute Run 'app'.
   
📊 Performance Analytics

During target device evaluation (Xiaomi Android Hardware Testbed), the application showcased the following metrics:

- Model Inference Latency: ~42ms (Running across 4 CPU Threads via TFLite Interpreter Configuration)

- FPS Stability during CameraX Preview: Stays locked at a stable 60 FPS

- Offline Operation Capability: 100% fully functional classification engine when cellular radio data is disabled

📸 Screenshots

1. Authentication & Dashboard
   
   <img width="200" height="450" alt="image" src="https://github.com/user-attachments/assets/7dff3792-9e52-43c1-9b8c-a9ab3428815c" />
   <img width="200" height="450" alt="image" src="https://github.com/user-attachments/assets/3639fbe8-1ab0-4498-9103-b40fbd1c9d0f" />
   
2. Edge AI Real-Time Inference (ResultActivity)

   <img width="200" height="450" alt="image" src="https://github.com/user-attachments/assets/9506abda-4ad3-465a-bba3-4c1b4b70d7aa" />

3. Geospatial Pest Clustering Map

   <img width="200" height="450" alt="image" src="https://github.com/user-attachments/assets/8177f518-2f52-4d3f-a19d-be40e252f309" />

