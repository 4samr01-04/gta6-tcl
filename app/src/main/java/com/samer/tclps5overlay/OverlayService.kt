package com.samer.tclps5overlay

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var subtitleView: TextView? = null
    private var activeConn: HttpURLConnection? = null
    @Volatile private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val code = intent?.getStringExtra("PAIRING_CODE") ?: return START_NOT_STICKY
        setupOverlay()
        startStream(code)
        return START_STICKY
    }

    private fun setupOverlay() {
        if (subtitleView != null) return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val tv = TextView(this).apply {
            text = "جاري الاتصال بالجوال... (GTA 6)"
            textSize = 26f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.argb(185, 0, 0, 0))
            setPadding(36, 18, 36, 18)
            gravity = Gravity.CENTER
            textDirection = TextView.TEXT_DIRECTION_RTL
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 70
        }

        wm.addView(tv, params)
        subtitleView = tv
    }

    private fun startStream(code: String) {
        isRunning = false
        try { activeConn?.disconnect() } catch (_: Exception) {}
        isRunning = true

        Thread {
            while (isRunning) {
                try {
                    val url = URL("https://ntfy.sh/gta6tcl-$code/json")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 10000
                        readTimeout = 0
                    }
                    activeConn = conn
                    val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                   var line: String? = null
                    while (isRunning && reader.readLine().also { line = it } != null) {
                        val raw = line?.trim() ?: continue
                        if (raw.isEmpty()) continue
                        val outer = JSONObject(raw)
                        when (outer.optString("event")) {
                            "open" -> subtitleView?.post {
                                subtitleView?.text = "متصل بالجوال • جاهز لترجمة GTA 6"
                            }
                            "message" -> {
                                val msg = outer.optString("message")
                                if (msg.isNotEmpty()) {
                                    val payload = JSONObject(msg)
                                    when (payload.optString("type")) {
                                        "subtitle" -> {
                                            val ar = payload.optString("ar")
                                            if (ar.isNotEmpty()) {
                                                subtitleView?.post { subtitleView?.text = ar }
                                            }
                                        }
                                        "style" -> {
                                            val fs = payload.optDouble("fontSize", 26.0).toFloat()
                                            val op = payload.optInt("bgOpacity", 70)
                                            val col = payload.optString("textColor", "#FFFFFF")
                                            subtitleView?.post {
                                                subtitleView?.textSize = fs
                                                try { subtitleView?.setTextColor(Color.parseColor(col)) } catch (_: Exception) {}
                                                subtitleView?.setBackgroundColor(Color.argb((op * 255) / 100, 0, 0, 0))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    reader.close()
                } catch (_: Exception) {
                    if (isRunning) {
                        try { Thread.sleep(2000) } catch (_: Exception) {}
                    }
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try { activeConn?.disconnect() } catch (_: Exception) {}
        subtitleView?.let { windowManager?.removeView(it) }
        subtitleView = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
