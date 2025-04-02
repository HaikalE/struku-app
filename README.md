# STRUKU - Pemindai Struk & Pelacak Pengeluaran Offline

Aplikasi Android untuk memindai struk belanja, mengekstrak data pengeluaran, dan melacak keuangan pribadi secara offline tanpa memerlukan koneksi internet.

## 📱 Fitur Utama

- Pemindaian struk secara offline dan cepat (<2 detik per struk)
- Ekstraksi otomatis nama pedagang, tanggal, daftar item, harga, dan total
- Kategorisasi otomatis pengeluaran menggunakan machine learning
- Visualisasi data pengeluaran (grafik per kategori, tren bulanan)
- Deteksi transaksi berulang dan notifikasi anggaran
- Dukungan multi-mata uang
- Privasi prioritas - semua data tetap di perangkat pengguna
- Keamanan lapis ganda dengan enkripsi dan autentikasi biometrik

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
- **Visualisasi Data**: ComposeCharts atau MPAndroidChart
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

## 🏗️ Struktur Proyek

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
│   │   │   └── presentation/       # UI, Compose, ViewModels
│   │   │       ├── scan/           # Pemindaian struk
│   │   │       ├── list/           # Daftar pengeluaran
│   │   │       ├── analytics/      # Dashboard & visualisasi
│   │   │       └── settings/       # Pengaturan aplikasi
│   │   ├── res/                    # Resource UI
│   │   └── AndroidManifest.xml
│   ├── test/                       # Unit tests
│   └── androidTest/                # Instrumented tests
```

## 📊 Model Data

Struktur data utama meliputi:

- **Receipt**: Informasi tentang struk (id, merchantName, date, total, category)
- **LineItem**: Item individual dalam struk (description, quantity, price)
- **Category**: Kategori pengeluaran dengan warna untuk visualisasi
- **Budget**: Anggaran yang ditetapkan pengguna per kategori

## 🚀 Timeline Pengembangan

1. **Fase 1-2**: Riset OCR, prototyping, pengembangan pipeline parsing (Minggu 1-5)
2. **Fase 3-4**: Implementasi database, UI dasar, dan alur kerja pemindaian (Minggu 6-10)
3. **Fase 5**: Fitur analitik, kategorisasi, dashboard (Minggu 11-13)
4. **Fase 6-7**: Penguatan keamanan, pengujian, dan optimasi (Minggu 14-16)
5. **Fase 8**: Rilis dan pemeliharaan (Minggu 17+)

## 📝 Catatan Privasi

Aplikasi ini didesain dengan privasi dan keamanan sebagai prioritas:

- Tidak memerlukan izin internet - semua pemrosesan terjadi di perangkat
- Data pribadi dienkripsi menggunakan SQLCipher
- Tidak ada pengiriman data ke server eksternal
- Opsional: Ekspor data terenkripsi yang dapat dicadangkan oleh pengguna

## 📄 Lisensi

[MIT License](LICENSE)
