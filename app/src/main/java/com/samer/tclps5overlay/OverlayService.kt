package com.samer.tclps5overlay

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.TextView

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var subtitleView: TextView? = null
    private var bridgeWebView: WebView? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val code = intent?.getStringExtra("PAIRING_CODE") ?: return START_NOT_STICKY
        setupOverlay()
        connectPeerBridge(code)
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

    private fun connectPeerBridge(code: String) {
        bridgeWebView?.destroy()
        val wv = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(object {
                @JavascriptInterface
                fun onConnected() {
                    subtitleView?.post { subtitleView?.text = "متصل بالجوال • جاهز لترجمة GTA 6" }
                }

                @JavascriptInterface
                fun onSubtitle(arabicText: String) {
                    subtitleView?.post { subtitleView?.text = arabicText }
                }

                @JavascriptInterface
                fun onStyle(fontSize: Float, opacity: Int, colorHex: String) {
                    subtitleView?.post {
                        subtitleView?.textSize = fontSize
                        try { subtitleView?.setTextColor(Color.parseColor(colorHex)) } catch (_: Exception) {}
                        subtitleView?.setBackgroundColor(Color.argb((opacity * 255) / 100, 0, 0, 0))
                    }
                }
            }, "AndroidOverlay")
        }

        val html = """
            <!doctype html><html><head>
            <script src="https://cdn.jsdelivr.net/npm/peerjs@1.5.4/dist/peerjs.min.js"></script>
            </head><body><script>
              const peer = new Peer();
              peer.on('open', () => {
                const conn = peer.connect('ps5-gta6-tcl-$code');
                conn.on('open', () => AndroidOverlay.onConnected());
                conn.on('data', (p) => {
                  if (p && p.type === 'subtitle') AndroidOverlay.onSubtitle(p.ar || '');
                  if (p && p.type === 'style') AndroidOverlay.onStyle(Number(p.fontSize || 26), Number(p.bgOpacity || 70), p.textColor || '#FFFFFF');
                });
              });
            </script></body></html>
        """.trimIndent()

        wv.loadDataWithBaseURL("https://peer.local/", html, "text/html", "UTF-8", null)
        bridgeWebView = wv
    }

    override fun onDestroy() {
        super.onDestroy()
        subtitleView?.let { windowManager?.removeView(it) }
        bridgeWebView?.destroy()
        subtitleView = null
        bridgeWebView = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
