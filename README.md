# STRUKU - Pemindai Struk & Pelacak Pengeluaran Offline

Aplikasi Android untuk memindai struk belanja, mengekstrak data pengeluaran, dan melacak keuangan pribadi secara offline tanpa memerlukan koneksi internet.

## 📱 Fitur Utama

- **Pemindaian OCR Offline**: Memindai struk menggunakan kamera dan mengenali teks secara offline dengan ML Kit.
- **Ekstraksi Data Otomatis**: Mengekstrak nama pedagang, tanggal, daftar item, harga, dan total.
- **Kategorisasi Otomatis**: Mengkategorikan pengeluaran berdasarkan nama pedagang dan item.
- **Visualisasi Data**: Melihat grafik dan statistik pengeluaran per kategori dan per bulan.
- **Anggaran & Pelacakan**: Menetapkan anggaran per kategori dan memantau penggunaan anggaran.
- **Privasi & Keamanan**: Autentikasi biometrik, data terenkripsi, dan operasi sepenuhnya offline.
- **Ekspor Data**: Mengekspor data ke CSV untuk analisis lebih lanjut.
- **Multi-bahasa**: Mendukung bahasa Inggris dan Indonesia.

## 🛠️ Cara Menggunakan

### Pemindaian Struk
1. Buka aplikasi dan ketuk tombol + untuk memindai struk baru.
2. Posisikan struk dalam bingkai pemindaian dan ambil foto.
3. Aplikasi akan memproses struk dan menampilkan hasil untuk ditinjau.
4. Periksa data yang diekstrak dan lakukan koreksi jika diperlukan.
5. Tekan tombol Simpan untuk menyimpan struk.

### Pengelolaan Struk
- **Melihat Daftar Struk**: Buka tab Struk untuk melihat semua struk tersimpan.
- **Detail Struk**: Ketuk struk untuk melihat detail lengkapnya.
- **Edit Struk**: Dalam tampilan detail, ketuk tombol Edit untuk memodifikasi data.
- **Hapus Struk**: Dalam tampilan detail, ketuk tombol Hapus untuk menghapus struk.

### Analitik & Pelacakan
- **Ringkasan Pengeluaran**: Buka tab Analitik untuk melihat grafik dan visualisasi pengeluaran.
- **Pemilihan Periode**: Gunakan pemilih bulan untuk melihat data dari bulan yang berbeda.
- **Breakdown Kategori**: Lihat breakdown pengeluaran berdasarkan kategori dalam bentuk diagram pie.
- **Status Anggaran**: Cek kemajuan anggaran per kategori jika anggaran telah ditetapkan.

### Ekspor & Berbagi
- Buka menu Pengaturan, kemudian pilih Ekspor Data.
- Pilih format ekspor (CSV) dan bagikan file melalui aplikasi lain.

### Keamanan
- Aplikasi secara default dilindungi dengan autentikasi biometrik.
- Data disimpan di perangkat secara terenkripsi menggunakan SQLCipher.
- Tidak ada data yang dikirim ke server - semuanya tetap di perangkat Anda.

## 🔧 Tech Stack

### OCR dan Pemrosesan Gambar
- **Google ML Kit Text Recognition v2** - OCR offline di perangkat
- **CameraX** - Pengambilan gambar dan antarmuka kamera
- **OpenCV/ML Kit DocumentScanner** - Deteksi dan auto-cropping struk

### Arsitektur Aplikasi
- **Bahasa**: Kotlin
- **Arsitektur**: Clean Architecture (3 lapisan: UI, Domain, Data)
- **Dependency Injection**: Hilt (Dagger)
- **Asynchronous**: Kotlin Coroutines dan Flow

### UI/UX
- **Framework UI**: Jetpack Compose
- **Navigasi**: Compose Navigation
- **Visualisasi Data**: Grafik pie dan progress bar dengan Compose
- **Tema**: Material Design 3 dengan dukungan mode gelap
- **Aksesibilitas**: Dukungan TalkBack dan font yang dapat disesuaikan

### Database dan Penyimpanan
- **Database**: Room (SQLite)
- **Enkripsi**: SQLCipher untuk enkripsi database
- **Preferensi**: Jetpack DataStore atau EncryptedSharedPreferences

### Keamanan
- **Autentikasi**: AndroidX Biometric (sidik jari/PIN)
- **Proteksi Layar**: FLAG_SECURE untuk mencegah screenshot
- **Privasi**: Pemrosesan data sepenuhnya offline
- **Export/Import**: Export data terenkripsi dengan password pengguna

## 💡 Pengembangan

Proyek menggunakan Gradle dengan Kotlin DSL untuk build system. Untuk menjalankan proyek:

1. Clone repositori ini
2. Buka dengan Android Studio
3. Sync Gradle dan build project
4. Run pada emulator atau perangkat fisik

### Persyaratan
- Android Studio Hedgehog (2023.1.1) atau lebih baru
- SDK Android minimum: 24 (Android 7.0)
- SDK Android target: 34 (Android 14)
- Gradle 8.2+
- Kotlin 1.9.22+

## 📊 Struktur Proyek

Aplikasi diorganisir menggunakan arsitektur bersih (Clean Architecture):

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/struku/
│   │   │   ├── data/               # Implementasi repositori, sumber data
│   │   │   │   ├── local/          # Database Room, DAO
│   │   │   │   └── ocr/            # Implementasi OCR, ML Kit
│   │   │   ├── di/                 # Dependency Injection dengan Hilt
│   │   │   ├── domain/             # Model, use cases, antarmuka repositori
│   │   │   │   ├── model/          # Entitas bisnis (Receipt, LineItem, etc)
│   │   │   │   ├── repository/     # Antarmuka repositori
│   │   │   │   └── usecase/        # Kasus penggunaan bisnis
│   │   │   ├── presentation/       # UI, Compose, ViewModels
│   │   │   │   ├── scan/           # Pemindaian struk
│   │   │   │   ├── receipts/       # Daftar & detail struk
│   │   │   │   ├── analytics/      # Dashboard & visualisasi
│   │   │   │   ├── settings/       # Pengaturan aplikasi
│   │   │   │   └── auth/           # Autentikasi biometrik
│   │   │   └── util/               # Utilities & helpers
│   │   ├── res/                    # Resource UI
│   │   └── AndroidManifest.xml
│   ├── test/                       # Unit tests
│   └── androidTest/                # Instrumented tests
```

## 🛡️ Privasi & Keamanan

Kami memprioritaskan privasi dan keamanan data keuangan Anda:

- **Tidak ada izin internet**: Aplikasi tidak memiliki izin internet, sehingga tidak dapat mengirimkan data.
- **Database terenkripsi**: Semua data disimpan menggunakan SQLCipher dengan enkripsi AES-256.
- **Perlindungan biometrik**: Akses ke aplikasi dilindungi dengan sidik jari atau PIN perangkat.
- **FLAG_SECURE**: Mencegah tangkapan layar atau tampilan dalam daftar aplikasi terbaru.
- **Ekspor terenkripsi opsional**: Pilihan untuk mengenkripsi file ekspor dengan kata sandi.

## 📄 Lisensi

[MIT License](LICENSE)

## 🤝 Kontribusi

Kontribusi sangat diterima! Lihat [CONTRIBUTING.md](CONTRIBUTING.md) untuk panduan kontribusi.

---

Dibuat dengan ❤️ untuk membantu pengelolaan keuangan pribadi dengan lebih baik sambil menjaga privasi.
