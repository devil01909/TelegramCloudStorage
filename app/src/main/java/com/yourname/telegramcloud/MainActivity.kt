package com.yourname.telegramcloud

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val textView = TextView(this)
        textView.text = "Telegram Cloud Storage\n\nApp is running!\n\nVersion 1.0"
        textView.textSize = 20f
        textView.gravity = android.view.Gravity.CENTER
        textView.setPadding(32, 32, 32, 32)
        
        setContentView(textView)
    }
}
