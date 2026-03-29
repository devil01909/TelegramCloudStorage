package com.yourname.telegramcloud

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class VerificationActivity : AppCompatActivity() {
    
    private lateinit var etCode: TextInputEditText
    private lateinit var btnVerify: MaterialButton
    private lateinit var tvResend: TextView
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var tvError: TextView
    
    private var phoneNumber: String = ""
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification)
        
        phoneNumber = intent.getStringExtra("phone_number") ?: ""
        
        etCode = findViewById(R.id.etCode)
        btnVerify = findViewById(R.id.btnVerify)
        tvResend = findViewById(R.id.tvResend)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)
        
        val tvSubtitle = findViewById<TextView>(R.id.tvSubtitle)
        tvSubtitle.text = "Enter the code sent to $phoneNumber"
        
        btnVerify.setOnClickListener {
            val code = etCode.text.toString().trim()
            
            if (TextUtils.isEmpty(code)) {
                showError("Please enter the verification code")
                return@setOnClickListener
            }
            
            if (code.length != 5) {
                showError("Code must be 5 digits")
                return@setOnClickListener
            }
            
            simulateVerification(code)
        }
        
        tvResend.setOnClickListener {
            Toast.makeText(this, "Code resent to $phoneNumber", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun simulateVerification(code: String) {
        showLoading(true)
        
        handler.postDelayed({
            showLoading(false)
            
            // For demo, any 5-digit code works
            if (code.length == 5) {
                // Login success
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                showError("Invalid verification code")
            }
        }, 1500)
    }
    
    private fun showLoading(show: Boolean) {
        btnVerify.isEnabled = !show
        tvResend.isEnabled = !show
        progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        tvError.visibility = android.view.View.GONE
    }
    
    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = android.view.View.VISIBLE
        btnVerify.isEnabled = true
        progressBar.visibility = android.view.View.GONE
    }
}
