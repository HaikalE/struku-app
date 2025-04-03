# Struku App Debugging Guide

Dokumen ini berisi petunjuk untuk debugging aplikasi Struku, khususnya untuk masalah visualisasi OCR dan pemrosesan gambar.

## Tampilan Debug OCR

Aplikasi Struku memiliki tampilan debug khusus untuk memvisualisasikan langkah-langkah pemrosesan gambar dalam proses OCR. Tampilan ini dapat diakses melalui menu pengaturan atau menu developer.

### Cara Menggunakan Tampilan Debug

1. Buka layar debug dengan mengakses "Pengaturan > Pengembang > Debug OCR"
2. Pastikan toggle "Debug Mode" diaktifkan (warna hijau)
3. Ambil gambar dengan menekan tombol "Capture Receipt" atau pilih gambar dari galeri
4. Lihat langkah-langkah pemrosesan yang ditampilkan di bagian bawah layar
5. Anda dapat menekan langkah pemrosesan tertentu untuk melihat detail dan gambar hasilnya

### Jika Layar Debug Tidak Menampilkan Gambar

Jika layar debug menampilkan "No image to display" meskipun ada langkah pemrosesan yang tercatat, coba langkah-langkah berikut:

1. Pastikan Debug Mode aktif
2. Coba reset sesi debug dengan menekan tombol "New Debug Session"
3. Ambil gambar baru untuk memicu pemrosesan
4. Restart aplikasi dan coba lagi

## Perbaikan Masalah Visual Debug

### 1. Masalah umum dan solusinya:

- **Tidak ada langkah pemrosesan yang ditampilkan:**
  - Pastikan mode debug diaktifkan
  - Pastikan `samplingRate` diatur ke 1 untuk menangkap semua langkah
  - Periksa log untuk error pemrosesan

- **Gambar tidak ditampilkan meskipun ada data:**
  - Periksa apakah gambar memiliki dimensi valid
  - Periksa apakah `_currentImage` di ViewModel diperbarui dengan benar
  - Pastikan bahwa langkah-langkah pemrosesan memiliki gambar yang valid

- **Langkah pemrosesan tidak sesuai dengan yang diharapkan:**
  - Periksa nilai enumerasi `ProcessingStep` pada UI dan nilai yang dihasilkan dari pemrosesan
  - Pastikan semua langkah pemrosesan yang diharapkan telah diimplementasikan

### 2. Kode Fix untuk Masalah Debug Mode:

```kotlin
// 1. Di PreprocessingDebugViewModel, tambahkan:
init {
    visualizer.setDebugMode(true)
    visualizer.setSamplingRate(1) // Tangkap semua langkah
    refreshProcessingSteps()
}

// 2. Pastikan setDebugMode menimplementasikan:
fun setDebugMode(enabled: Boolean) {
    _isDebugMode.value = enabled
    visualizer.setDebugMode(enabled)
    if (enabled) {
        visualizer.setSamplingRate(1)
    }
}
```

### 3. Menganalisis Log Debug:

Jika terjadi masalah, periksa log dengan tag-tag berikut:
- `ProcessingVisualizer` - Log dari visualizer preprocessing
- `AdvancedImagePreprocessor` - Log dari pemrosesan gambar
- `PreprocessingDebugVM` - Log dari ViewModel debug
- `MlKitOcrEngine` - Log dari mesin OCR

### 4. Alur Standar Debug:

- Gambar diambil atau dipilih
- `PreprocessingDebugViewModel.processImage()` dipanggil
- `AdvancedImagePreprocessor.processReceiptImage()` dipanggil
- Setiap langkah pemrosesan memanggil `visualizer.captureProcessingStep()`
- `_processingSteps` diperbarui di `PreprocessingVisualizer`
- `PreprocessingDebugViewModel.refreshProcessingSteps()` mengambil data dari visualizer
- UI diperbarui dengan data langkah-langkah baru

## Panduan Pengembangan

Saat mengembangkan fitur pemrosesan gambar baru:

1. Selalu tambahkan visualisasi untuk setiap langkah dengan:
   ```kotlin
   visualizer.captureProcessingStep(
       "Nama Langkah",
       "Deskripsi langkah ini",
       hasilGambar
   )
   ```

2. Untuk meningkatkan performa dalam mode produksi:
   - Pastikan mode debug dinonaktifkan secara default
   - Batasi ukuran gambar yang diproses
   - Gunakan caching untuk menghindari pemrosesan berulang

3. Jika menambahkan langkah pemrosesan baru:
   - Tambahkan ke enum `ProcessingStep` di `ProcessingStep.kt`
   - Pastikan UI dapat menampilkannya dengan benar

## Kontak

Jika masalah debug tetap terjadi setelah mengikuti panduan ini, hubungi tim pengembangan di:
- Email: dev@struku-app.example.com
- GitHub Issues: https://github.com/HaikalE/struku-app/issues

---

*Dokumen ini terakhir diperbarui pada: 3 April 2025*
