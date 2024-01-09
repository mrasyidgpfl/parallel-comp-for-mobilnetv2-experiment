# SIBI

**Post-Skripsi Project**
Project ini adalah bagian dari penelitian Sistem Penerjemah SIBI oleh Erdefi Rakun, dengan fokus untuk mengembangkan aplikasi yang mampu menerjemahkan gerakan isyarat SIBI dan menghasilkan animasi gerakan isyarat SIBI.

Core Development:
- Output : Aplikasi Android
- Bahasa Pemrograman : Kotlin
- Arsitektur : Partial MVP (Model, View, Presenter)

Developer:
- Misael Jonathan
- Wikan Setiaji

# Misael 
- Description : Terdapat dua environment sistem penerjemah SIBI, yaitu online dan offline. *Online environment* bekerja dengan dukungan server, sehingga seluruh proses penerjemahan (CPU Intensive Tasks) bekerja pada server. Peran aplikasi pada *online environment* untuk menjembatani interaksi user dengan sistem penerjemah. *Offline environment* bekerja dengan menjalankan sepenuhnya operasi sistem penerjemahan pada Android. Model (MobileNetV2, CRF, dan LSTM) bekerja menggunakan TensorFlow-Android, Preprocessing dilakukan dengan menggunakan JavaCV sebagai *Frame Extractor* dan OpenCV untuk *Frame/Image Processing*.
- Skema/Diagram :
- Core Technology : Android (TensorFlow-Android, OpenCV, JavaCV) dan Server (Flask-Python, Heroku)
- API : 

| endpoint | method | description |
| ------ | ------ | ------ |
| /translate | POST | send video filetype to server, return translation stringtype |

# Wikan
- Description : Sistem penerjemah Text to Sign SIBI dibuat menggunakan Unity3D dan selanjutnya diimplementasi menjadi aplikasi native Android. Project Unity3D pada sistem ini dijadikan sebuah modul yang diimplementasi pada project Native Android.
- Skema/Diagram :
- Core Technology : Unity3D dan Android
- Unity and Android Interaction : 
    - Unity functions called by Android : 
        - void **triggerAnimation**(string **text**): called when the text input is submitted.
        - void **triggerModel**(string **model**): called when the chosen animation model is changed.
        - void **setSliderSpeedValue**(string **value**): called when the slider speed value is changed.
    - Android functions called by Unity :
        - fun **showInput**(**param**:String): called when the animation clips are finished playing.

# Rasyid
- Description : Proses ektraksi fitur dengan MobileNetV2 sekarang dilakukan pada kelas `MobileNetV2Classifier` yang menggunakan beberapa modul *framework* TensorFlow Lite. Kelas `TestingModelProvider` ditambahkan untuk mengukur kinerja proses. Kelas `WordSequenceAligner` ditambahkan untuk pengukuran *sentence accuracy* dan *word error rate* (WER).
- Skema/Diagram :

<div align="center">
<img src="/app/src/main/assets/images/class_diagram.png"  width="60%">
</div>

- *Core Technology* : Kotlin dan TensoFlow Lite.
- Penggunaan : Gunakan fitu *Run* pada Android Studio setelah perangkat terhubung.
- *Note* : Pastikan *testing dataset* sudah tersimpan pada perangkat dalam *folder* SIBI/Video/.
