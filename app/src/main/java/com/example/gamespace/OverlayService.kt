package com.example.gamespace

import android.app.*
import android.content.*
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.*
import android.widget.*

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var panel: LinearLayout
    private lateinit var panelLp: WindowManager.LayoutParams
    private lateinit var info: TextView
    private lateinit var intervalLabel: TextView
    private lateinit var startBtn: Button
    private val markers = mutableListOf<Pair<TextView, WindowManager.LayoutParams>>()
    private var running = false
    private var idx = 0
    private var intervalMs = 500L
    private val baseFlags =
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_NOT_STICKY

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        buildPanel()
        handler.post(monitor)
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacksAndMessages(null)
        markers.forEach { runCatching { wm.removeView(it.first) } }
        runCatching { wm.removeView(panel) }
        super.onDestroy()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("gs", "GameSpace", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "gs")
            .setContentTitle("GameSpace Lite aktif")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else startForeground(1, n)
    }

    private fun btn(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label; textSize = 12f; minHeight = 0; minimumHeight = 0
        setPadding(dp(10), dp(4), dp(10), dp(4))
        setOnClickListener { onClick() }
    }

    private fun row(vararg v: View) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        v.forEach { addView(it) }
    }

    private fun drag(lp: WindowManager.LayoutParams, target: View): View.OnTouchListener {
        var sx = 0; var sy = 0; var tx = 0f; var ty = 0f
        return View.OnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = lp.x; sy = lp.y; tx = e.rawX; ty = e.rawY }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = sx + (e.rawX - tx).toInt()
                    lp.y = sy + (e.rawY - ty).toInt()
                    wm.updateViewLayout(target, lp)
                }
            }
            true
        }
    }

    private fun buildPanel() {
        panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply { setColor(0xE6202020.toInt()); cornerRadius = dp(10).toFloat() }
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        val header = TextView(this).apply {
            text = "≡ GameSpace Lite (tahan & geser)"; setTextColor(Color.WHITE); textSize = 12f
        }
        info = TextView(this).apply { setTextColor(0xFFB0BEC5.toInt()); textSize = 11f }
        intervalLabel = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 12f; gravity = Gravity.CENTER; minWidth = dp(80)
        }
        startBtn = btn("Mulai") { toggle() }

        listOf(
            header, info,
            row(btn("+ Titik") { addMarker() }, startBtn),
            row(btn("−") { changeInterval(-50) }, intervalLabel, btn("+") { changeInterval(50) }),
            row(btn("Hapus titik") { clearMarkers() }, btn("Tutup") { stopSelf() })
        ).forEach { panel.addView(it) }
        updateIntervalLabel()

        panelLp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, baseFlags, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = dp(8); y = dp(80) }
        header.setOnTouchListener(drag(panelLp, panel))
        wm.addView(panel, panelLp)
    }

    private fun addMarker() {
        if (running) return
        val size = dp(44)
        val tv = TextView(this).apply {
            text = "${markers.size + 1}"; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xAAFF5722.toInt()) }
        }
        val lp = WindowManager.LayoutParams(
            size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, baseFlags, PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels / 2 - size / 2 + markers.size * dp(12)
            y = resources.displayMetrics.heightPixels / 2 - size / 2 + markers.size * dp(12)
        }
        tv.setOnTouchListener(drag(lp, tv))
        wm.addView(tv, lp)
        markers.add(tv to lp)
    }

    private fun clearMarkers() {
        stopRun()
        markers.forEach { runCatching { wm.removeView(it.first) } }
        markers.clear()
    }

    private fun setTouchable(touchable: Boolean) {
        markers.forEach { (v, lp) ->
            lp.flags = if (touchable) baseFlags else baseFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            wm.updateViewLayout(v, lp)
        }
    }

    private fun changeInterval(delta: Int) {
        intervalMs = (intervalMs + delta).coerceIn(50, 5000)
        updateIntervalLabel()
    }

    private fun updateIntervalLabel() { intervalLabel.text = "$intervalMs ms" }

    private fun toggle() { if (running) stopRun() else startRun() }

    private fun startRun() {
        if (ClickService.instance == null) { toast("Aktifkan layanan aksesibilitas dulu"); return }
        if (markers.isEmpty()) { toast("Tambah titik dulu"); return }
        running = true; idx = 0
        setTouchable(false)   // supaya ketukan tidak tertahan oleh penanda
        startBtn.text = "Stop"
        handler.post(tapLoop)
    }

    private fun stopRun() {
        running = false
        handler.removeCallbacks(tapLoop)
        setTouchable(true)
        startBtn.text = "Mulai"
    }

    private val tapLoop = object : Runnable {
        override fun run() {
            if (!running) return
            val svc = ClickService.instance ?: run { stopRun(); return }
            val lp = markers[idx % markers.size].second
            svc.tap(lp.x + lp.width / 2f, lp.y + lp.height / 2f)
            idx++
            handler.postDelayed(this, intervalMs)
        }
    }

    private val monitor = object : Runnable {
        override fun run() {
            val i = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temp = (i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
            val lvl = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val mi = ActivityManager.MemoryInfo()
            (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
            info.text = "Suhu $temp°C · Baterai $lvl% · RAM bebas ${mi.availMem / 1048576} MB"
            handler.postDelayed(this, 2000)
        }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
