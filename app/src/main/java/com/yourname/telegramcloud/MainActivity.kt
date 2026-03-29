package com.yourname.telegramcloud

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Just a simple text view
        val textView = TextView(this)
        textView.text = "Telegram Cloud Storage\n\nVersion 1.0\n\nWorking!"
        textView.textSize = 24f
        textView.gravity = android.view.Gravity.CENTER
        textView.setTextColor(0xFF0088CC.toInt())
        setContentView(textView)
    }
}
