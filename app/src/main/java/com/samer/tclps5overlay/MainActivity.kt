package com.samer.tclps5overlay

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0B1020"))
            setPadding(80, 64, 80, 64)
        }

        val title = TextView(this).apply {
            text = "مُترجم GTA 6 لشاشة TCL (فوق HDMI)"
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }

        val codeInput = EditText(this).apply {
            hint = "أدخل رمز الجوال (6 أرقام)"
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            textSize = 22f
            gravity = Gravity.CENTER
            inputType = InputType.TYPE_CLASS_NUMBER
            setBackgroundColor(Color.parseColor("#182238"))
            setPadding(32, 24, 32, 24)
        }

        val startBtn = Button(this).apply {
            text = "تشغيل الترجمة الشفافة فوق PS5"
            textSize = 18f
            setBackgroundColor(Color.parseColor("#0070D1"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    Toast.makeText(this@MainActivity, "فعّل صلاحية العرض فوق التطبيقات ثم ارجع للتطبيق", Toast.LENGTH_LONG).show()
                    val permIntent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(permIntent)
                    return@setOnClickListener
                }
                val code = codeInput.text.toString().replace(" ", "").trim()
                if (code.length == 6) {
                    val svc = Intent(this@MainActivity, OverlayService::class.java)
                    svc.putExtra("PAIRING_CODE", code)
                    startService(svc)
                    Toast.makeText(this@MainActivity, "تم تشغيل الترجمة! حوّل الآن لمدخل HDMI للـ PS5", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this@MainActivity, "أدخل 6 أرقام صحيحة من الجوال", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val stopBtn = Button(this).apply {
            text = "إيقاف الترجمة"
            textSize = 16f
            setBackgroundColor(Color.parseColor("#334155"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                stopService(Intent(this@MainActivity, OverlayService::class.java))
                Toast.makeText(this@MainActivity, "تم إيقاف الترجمة", Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(title)
        layout.addView(codeInput)
        layout.addView(startBtn)
        layout.addView(stopBtn)
        setContentView(layout)
    }
}
