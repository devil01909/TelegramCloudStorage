package com.yourname.telegramcloud.auth

import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.yourname.telegramcloud.MainActivity
import com.yourname.telegramcloud.databinding.ActivityVerificationBinding

class VerificationActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityVerificationBinding
    private lateinit var authManager: AuthManager
    private var phoneNumber: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        phoneNumber = intent.getStringExtra("phone_number") ?: ""
        authManager = AuthManager.getInstance(this)
        
        setupUI()
        setupAuthCallback()
        
        // Show phone number in subtitle
        binding.tvSubtitle.text = getString(R.string.verification_subtitle, phoneNumber)
    }
    
    private fun setupUI() {
        binding.btnVerify.setOnClickListener {
            val code = binding.etCode.text.toString().trim()
            
            if (TextUtils.isEmpty(code)) {
                showError("Please enter the verification code")
                return@setOnClickListener
            }
            
            if (code.length != 5) {
                showError("Code must be 5 digits")
                return@setOnClickListener
            }
            
            verifyCode(code)
        }
        
        binding.tvResend.setOnClickListener {
            resendCode()
        }
    }
    
    private fun setupAuthCallback() {
        authManager.setCallback(object : AuthManager.AuthCallback {
            override fun onCodeRequired() {
                // Already waiting for code
            }
            
            override fun onPasswordRequired() {
                runOnUiThread {
                    showError("2FA password required")
                }
            }
            
            override fun onSuccess(user: com.yourname.telegramcloud.database.entities.User) {
                runOnUiThread {
                    Toast.makeText(
                        this@VerificationActivity,
                        "Login successful!",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    val intent = Intent(this@VerificationActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            
            override fun onError(error: String) {
                runOnUiThread {
                    showError(error)
                }
            }
        })
    }
    
    private fun verifyCode(code: String) {
        showLoading(true)
        authManager.sendVerificationCode(code)
    }
    
    private fun resendCode() {
        showLoading(true)
        authManager.sendPhoneNumber(phoneNumber)
    }
    
    private fun showLoading(show: Boolean) {
        binding.btnVerify.isEnabled = !show
        binding.tvResend.isEnabled = !show
        binding.progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        binding.tvError.visibility = android.view.View.GONE
    }
    
    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = android.view.View.VISIBLE
        binding.btnVerify.isEnabled = true
        binding.tvResend.isEnabled = true
        binding.progressBar.visibility = android.view.View.GONE
    }
}