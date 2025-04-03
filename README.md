# Struku - Receipt Scanner App

Aplikasi Android pemindai struk dan pelacak pengeluaran yang berjalan sepenuhnya offline dengan fitur OCR, analisis pengeluaran, dan kategorisasi otomatis.

## Build Fixes - April 3, 2025

The following issues were fixed in the latest commits:

1. Fixed property name mismatches in LineItemMapper.kt (using 'name' instead of 'description')
2. Added ApplicationContext parameter to AdvancedImagePreprocessor in OcrModule
3. Fixed CategorizeExpenseUseCase to use the correct property name ('name')
4. Added getTotalByCategory method with Date parameters to ReceiptRepository
5. Implemented getTotalByCategory in ReceiptRepositoryImpl
6. Added getTotalByCategoryInDateRange to ReceiptDao
7. Fixed AddReceiptScreen to use Date object correctly
8. Added scanReceipt and saveReceiptImage methods to OcrRepository interface
9. Implemented scanReceipt and saveReceiptImage in OcrRepositoryImpl
10. Fixed ScanReceiptUseCase by implementing necessary repository methods

## Features

- Pindai struk dengan kamera
- Ekstraksi otomatis Toko, Tanggal, dan Total
- Kategorisasi otomatis pengeluaran
- Analisis pengeluaran bulanan 
- Pencarian struk

## Tech Stack

- Kotlin
- MVVM Architecture
- Jetpack Compose
- Hilt Dependency Injection
- Room Database
- ML Kit OCR
- Work Manager
- Unit & UI Testing

## Setup & Installation

1. Clone repository
2. Buka project di Android Studio
3. Sync project dan tunggu gradle build selesai
4. Run di emulator atau device fisik

## Kontribusi

Kontribusi selalu diterima! Silakan buat issue atau pull request.
