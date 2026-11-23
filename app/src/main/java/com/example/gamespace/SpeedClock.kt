package com.example.gamespace

import android.os.SystemClock

/**
 * Jam berskala untuk aplikasi/game MILIK SENDIRI.
 * scale 2.0 = waktu berjalan 2x lebih cepat, 0.5 = 2x lebih lambat.
 * Pakai SpeedClock.nowMs() sebagai pengganti SystemClock.elapsedRealtime()
 * di logika timer/game Anda.
 */
object SpeedClock {
    @Volatile var scale = 1.0
        private set
    private var baseReal = SystemClock.elapsedRealtime()
    private var baseVirtual = baseReal

    @Synchronized fun nowMs(): Long {
        val r = SystemClock.elapsedRealtime()
        return baseVirtual + ((r - baseReal) * scale).toLong()
    }

    @Synchronized fun setScale(s: Double) {
        val r = SystemClock.elapsedRealtime()
        baseVirtual += ((r - baseReal) * scale).toLong()
        baseReal = r
        scale = s.coerceIn(0.1, 10.0)
    }
}
