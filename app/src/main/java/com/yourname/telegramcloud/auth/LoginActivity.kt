package com.yourname.telegramcloud.auth

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yourname.telegramcloud.MainActivity
import com.yourname.telegramcloud.databinding.ActivityLoginBinding
import com.yourname.telegramcloud.utils.Constants
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private lateinit var authManager: AuthManager
    private var phoneNumber: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        authManager = AuthManager.getInstance(this)
        
        setupUI()
        setupAuthCallback()
        initializeTelegram()
    }
    
    private fun setupUI() {
        binding.btnLogin.setOnClickListener {
            phoneNumber = binding.etPhoneNumber.text.toString().trim()
            
            if (TextUtils.isEmpty(phoneNumber)) {
                showError("Please enter your phone number")
                return@setOnClickListener
            }
            
            if (!isValidPhoneNumber(phoneNumber)) {
                showError("Please enter a valid phone number")
                return@setOnClickListener
            }
            
            // Format phone number with country code if needed
            if (!phoneNumber.startsWith("+")) {
                phoneNumber = "+$phoneNumber"
            }
            
            startLogin()
        }
    }
    
    private fun setupAuthCallback() {
        authManager.setCallback(object : AuthManager.AuthCallback {
            override fun onCodeRequired() {
                runOnUiThread {
                    // Navigate to verification activity
                    val intent = Intent(this@LoginActivity, VerificationActivity::class.java).apply {
                        putExtra("phone_number", phoneNumber)
                    }
                    startActivity(intent)
                    finish()
                }
            }
            
            override fun onPasswordRequired() {
                runOnUiThread {
                    showError("2FA password required (will be handled in next version)")
                }
            }
            
            override fun onSuccess(user: com.yourname.telegramcloud.database.entities.User) {
                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        "Welcome ${user.firstName ?: user.username ?: "User"}!",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Navigate to main activity
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
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
    
    private fun initializeTelegram() {
        lifecycleScope.launch {
            authManager.initializeTdlib()
        }
    }
    
    private fun startLogin() {
        showLoading(true)
        authManager.sendPhoneNumber(phoneNumber)
    }
    
    private fun isValidPhoneNumber(phone: String): Boolean {
        // Basic phone number validation
        val cleaned = phone.replace(Regex("[^0-9+]"), "")
        return cleaned.length in 8..15 && (cleaned.startsWith("+") || cleaned.all { it.isDigit() })
    }
    
    private fun showLoading(show: Boolean) {
        binding.btnLogin.isEnabled = !show
        binding.progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        binding.tvError.visibility = android.view.View.GONE
    }
    
    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = android.view.View.VISIBLE
        binding.btnLogin.isEnabled = true
        binding.progressBar.visibility = android.view.View.GONE
    }
}