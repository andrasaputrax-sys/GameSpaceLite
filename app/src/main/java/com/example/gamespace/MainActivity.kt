package com.example.gamespace

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        status = TextView(this).apply { textSize = 14f }
        fun btn(t: String, a: () -> Unit) = Button(this).apply { text = t; setOnClickListener { a() } }

        root.addView(status)
        root.addView(btn("1. Izinkan tampil di atas aplikasi lain") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        root.addView(btn("2. Aktifkan layanan aksesibilitas") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        root.addView(btn("3. Tampilkan panel melayang") {
            if (Settings.canDrawOverlays(this)) startForegroundService(Intent(this, OverlayService::class.java))
            else Toast.makeText(this, "Izin overlay belum diberikan", Toast.LENGTH_SHORT).show()
        })
        root.addView(btn("Tutup panel") { stopService(Intent(this, OverlayService::class.java)) })
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        status.text = "Overlay: " + (if (Settings.canDrawOverlays(this)) "OK" else "belum") +
            "\nAksesibilitas: " + (if (ClickService.instance != null) "aktif" else "belum aktif")
    }
}
