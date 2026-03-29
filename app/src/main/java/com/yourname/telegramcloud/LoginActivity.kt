package com.yourname.telegramcloud

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.*

class LoginActivity : AppCompatActivity() {
    
    private lateinit var etPhoneNumber: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var tvError: android.widget.TextView
    
    // Simulate Telegram login (since we don't have real API yet)
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)
        
        btnLogin.setOnClickListener {
            val phoneNumber = etPhoneNumber.text.toString().trim()
            
            if (TextUtils.isEmpty(phoneNumber)) {
                showError("Please enter your phone number")
                return@setOnClickListener
            }
            
            if (phoneNumber.length < 8) {
                showError("Please enter a valid phone number")
                return@setOnClickListener
            }
            
            // Simulate sending verification code
            simulateLogin(phoneNumber)
        }
    }
    
    private fun simulateLogin(phoneNumber: String) {
        showLoading(true)
        
        // Simulate network delay
        handler.postDelayed({
            showLoading(false)
            
            // Navigate to verification screen
            val intent = Intent(this, VerificationActivity::class.java)
            intent.putExtra("phone_number", phoneNumber)
            startActivity(intent)
        }, 1500)
    }
    
    private fun showLoading(show: Boolean) {
        btnLogin.isEnabled = !show
        progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        tvError.visibility = android.view.View.GONE
    }
    
    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = android.view.View.VISIBLE
        btnLogin.isEnabled = true
        progressBar.visibility = android.view.View.GONE
    }
}
