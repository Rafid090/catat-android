# Catat

Aplikasi Android untuk mencatat ide dan mengelola tugas harian, dengan pengingat dan tag.

> **Status:** masih dalam tahap pengembangan. Aplikasi ini dibangun dengan bantuan asisten AI.

## Fitur

- **Catatan:** buat, sunting, sematkan ke atas, dan cari catatan.
- **Tugas:** tenggat waktu (hari ini, besok, terlambat), tanda prioritas tinggi, filter Semua / Aktif / Selesai, dan pencarian.
- **Pengingat:** notifikasi saat tugas jatuh tempo. Memakai alarm presisi bila diizinkan, dan dijadwalkan ulang setelah perangkat dinyalakan ulang.
- **Tag:** kelompokkan catatan dan tugas dengan tag.
- **Geser untuk menghapus**, dengan tombol Urungkan.

## Teknologi

Kotlin · Jetpack Compose (Material 3) · Room (dengan skema migrasi) · Navigation Compose · ViewModel · AlarmManager & BroadcastReceiver

## Menjalankan

1. Buka folder proyek di Android Studio versi terbaru.
2. Tunggu sinkronisasi Gradle selesai, lalu jalankan konfigurasi `app` di emulator atau perangkat.

Minimal Android 8.0 (API 26).

## Struktur

```
app/src/main/java/com/notetodo/app/
├── data/        entitas, DAO, dan database Room
├── reminder/    penjadwalan alarm & notifikasi pengingat
├── ui/          layar catatan, tugas, komponen, dan tema (Compose)
└── util/        format tanggal
```
