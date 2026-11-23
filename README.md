# GameSpace Lite

Panel melayang + auto clicker + monitor suhu/RAM + SpeedClock (timer berskala untuk app sendiri).

## Build tanpa Android Studio (cukup dari HP)
1. Buat repo GitHub, unggah semua isi folder ini (termasuk `.github`).
2. Buka tab **Actions** -> "Build APK" -> tunggu selesai.
3. Unduh artifact `gamespace-debug-apk`, ekstrak, pasang APK-nya.

Atau lokal: `gradle assembleDebug` (JDK 17 + Android SDK 34).

## Pemakaian
1. Beri izin overlay, aktifkan layanan aksesibilitas "GameSpace Lite".
2. Tampilkan panel -> "+ Titik" -> geser penanda ke tombol di game/app.
3. Atur interval -> Mulai. Penanda diketuk berurutan, berulang.

Catatan: koordinat bisa meleset beberapa piksel di HP dengan notch/cutout.
