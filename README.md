# SIBI

**Post-Thesis Project**
This project is a part of the SIBI Translator System research by Erdefi Rakun, focusing on developing an application capable of translating SIBI sign language gestures and generating SIBI sign language movement animations.

Core Development:

Output: Android Application
Programming Language: Kotlin
Architecture: Partial MVP (Model, View, Presenter)

# Misael 
- Description: The SIBI translator system has two environments, namely online and offline. The online environment operates with server support, allowing all translation processes (CPU-intensive tasks) to run on the server. The application's role in the online environment is to facilitate user interaction with the translator system. The offline environment operates by fully running the translation system operations on Android. Models (MobileNetV2, CRF, and LSTM) use TensorFlow-Android, preprocessing is done using JavaCV as a Frame Extractor, and OpenCV for Frame/Image Processing.
- Scheme/Diagram:
- Core Technology: Android (TensorFlow-Android, OpenCV, JavaCV) and Server (Flask-Python, Heroku)
- API:

| endpoint | method | description |
| ------ | ------ | ------ |
| /translate | POST | Send video filetype to the server, return translation stringtype |

# Wikan
- Description: The Text to Sign SIBI translator system is created using Unity3D and subsequently implemented into a native Android application. The Unity3D project in this system is turned into a module that is implemented in the Native Android project.
- Scheme/Diagram:
- Core Technology: Unity3D and Android
- Unity and Android Interaction: 
    - Unity functions called by Android : 
        - void **triggerAnimation**(string **text**): called when the text input is submitted.
        - void **triggerModel**(string **model**): called when the chosen animation model is changed.
        - void **setSliderSpeedValue**(string **value**): called when the slider speed value is changed.
    - Android functions called by Unity :
        - fun **showInput**(**param**:String): called when the animation clips are finished playing.

# Rasyid
- Description: Feature extraction with MobileNetV2 is now performed in the  `MobileNetV2Classifier` class using several TensorFlow Lite framework modules. The `TestingModelProvider` class is added to measure the performance of the process. The `WordSequenceAligner` class is added for measuring sentence accuracy and word error rate (WER).
- Scheme/Diagram :

<div align="center">
<img src="/app/src/main/assets/images/class_diagram.png"  width="60%">
</div>

- Core Technology: Kotlin and TensorFlow Lite.
- Usage: Use the Run feature in Android Studio after connecting the device.
- Note: You need the testing dataset (videos) stored on your Android device specifically in a folder named "SIBI/Video/".
