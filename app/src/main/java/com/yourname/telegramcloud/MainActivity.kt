package com.yourname.telegramcloud

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create layout
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }
        
        // Title
        val title = TextView(this).apply {
            text = "Telegram Cloud Storage"
            textSize = 24f
            setTextColor(0xFF0088CC.toInt())
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 50)
        }
        
        // Status text
        val status = TextView(this).apply {
            text = "App is running!\n\nReady to upload files"
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 50)
        }
        
        // Button
        val button = Button(this).apply {
            text = "Coming Soon"
            setOnClickListener {
                Toast.makeText(this@MainActivity, "Full features coming soon!", Toast.LENGTH_SHORT).show()
            }
        }
        
        layout.addView(title)
        layout.addView(status)
        layout.addView(button)
        setContentView(layout)
    }
}
