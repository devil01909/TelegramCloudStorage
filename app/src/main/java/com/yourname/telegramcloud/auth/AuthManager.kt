package com.yourname.telegramcloud.auth

import android.content.Context
import android.util.Log
import com.yourname.telegramcloud.database.entities.User
import com.yourname.telegramcloud.database.repository.StorageRepository
import com.yourname.telegramcloud.utils.Constants
import com.yourname.telegramcloud.utils.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AuthManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AuthManager"
        private var instance: AuthManager? = null
        
        fun getInstance(context: Context): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val repository = StorageRepository(context)
    private val preferencesManager = PreferencesManager(context)
    
    // Authentication state
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private var tdlibClient: Client? = null
    private var authorizationState: TdApi.AuthorizationState? = null
    private val authorizationCallbacks = ConcurrentHashMap<Int, (TdApi.Object) -> Unit>()
    private var currentQueryId = 0
    
    sealed class AuthState {
        object Unauthenticated : AuthState()
        object WaitingForCode : AuthState()
        object WaitingForPassword : AuthState()
        object Authenticating : AuthState()
        data class Authenticated(val user: User) : AuthState()
        data class Error(val message: String) : AuthState()
    }
    
    // Callback interface for UI
    interface AuthCallback {
        fun onCodeRequired()
        fun onPasswordRequired()
        fun onSuccess(user: User)
        fun onError(error: String)
    }
    
    private var callback: AuthCallback? = null
    
    fun setCallback(callback: AuthCallback) {
        this.callback = callback
    }
    
    // Initialize TDLib client
    fun initializeTdlib() {
        try {
            // Set up TDLib log verbosity
            TdApi.setLogVerbosityLevel(1)
            TdApi.setLogStream(TdApi.LogStreamDefault())
            
            // Create client
            tdlibClient = Client(object : Client.ResultHandler {
                override fun onResult(result: TdApi.Object) {
                    handleUpdate(result)
                }
            }, null, null)
            
            // Send initial parameters
            sendSetTdlibParameters()
            
            Log.d(TAG, "TDLib initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TDLib: ${e.message}", e)
            _authState.value = AuthState.Error("Failed to initialize: ${e.message}")
        }
    }
    
    private fun sendSetTdlibParameters() {
        val parameters = TdApi.SetTdlibParameters().apply {
            useTestDc = false
            databaseDirectory = "${context.filesDir}/${Constants.TDLIB_DIRECTORY}/database"
            filesDirectory = "${context.filesDir}/${Constants.TDLIB_DIRECTORY}/files"
            useFileDatabase = true
            useChatInfoDatabase = true
            useMessageDatabase = true
            useSecretChats = false
            apiId = Constants.TELEGRAM_API_ID
            apiHash = Constants.TELEGRAM_API_HASH
            systemLanguageCode = "en"
            deviceModel = android.os.Build.MODEL
            systemVersion = android.os.Build.VERSION.RELEASE
            applicationVersion = Constants.APP_VERSION
            enableStorageOptimizer = true
            ignoreFileNames = false
        }
        
        sendRequest(parameters) { result ->
            if (result is TdApi.Ok) {
                Log.d(TAG, "TDLib parameters set successfully")
                // Request authentication
                sendRequest(TdApi.GetAuthorizationState()) { state ->
                    handleAuthorizationState(state)
                }
            } else {
                Log.e(TAG, "Failed to set TDLib parameters: $result")
            }
        }
    }
    
    private fun handleUpdate(update: TdApi.Object) {
        when (update) {
            is TdApi.UpdateAuthorizationState -> {
                handleAuthorizationState(update.authorizationState)
            }
            is TdApi.UpdateUser -> {
                // User info updated
                Log.d(TAG, "User updated: ${update.user.firstName} ${update.user.lastName}")
            }
            else -> {
                // Handle other updates if needed
            }
        }
    }
    
    private fun handleAuthorizationState(state: TdApi.AuthorizationState) {
        authorizationState = state
        Log.d(TAG, "Authorization state: ${state::class.java.simpleName}")
        
        when (state) {
            is TdApi.AuthorizationStateReady -> {
                // User is logged in
                getCurrentUser()
            }
            is TdApi.AuthorizationStateWaitPhoneNumber -> {
                _authState.value = AuthState.Unauthenticated
            }
            is TdApi.AuthorizationStateWaitCode -> {
                _authState.value = AuthState.WaitingForCode
                callback?.onCodeRequired()
            }
            is TdApi.AuthorizationStateWaitPassword -> {
                _authState.value = AuthState.WaitingForPassword
                callback?.onPasswordRequired()
            }
            is TdApi.AuthorizationStateWaitRegistration -> {
                // First time login - need registration
                registerUser(state)
            }
            is TdApi.AuthorizationStateClosing -> {
                _authState.value = AuthState.Unauthenticated
            }
            is TdApi.AuthorizationStateClosed -> {
                _authState.value = AuthState.Unauthenticated
            }
            is TdApi.AuthorizationStateLoggingOut -> {
                _authState.value = AuthState.Unauthenticated
            }
            else -> {
                Log.d(TAG, "Unhandled authorization state: ${state::class.java.simpleName}")
            }
        }
    }
    
    fun sendPhoneNumber(phoneNumber: String) {
        _authState.value = AuthState.Authenticating
        val request = TdApi.SetAuthenticationPhoneNumber(phoneNumber, null)
        sendRequest(request) { result ->
            if (result is TdApi.Ok) {
                Log.d(TAG, "Phone number sent successfully")
            } else if (result is TdApi.Error) {
                val errorMsg = "Failed to send phone number: ${result.message}"
                Log.e(TAG, errorMsg)
                _authState.value = AuthState.Error(errorMsg)
                callback?.onError(errorMsg)
            }
        }
    }
    
    fun sendVerificationCode(code: String) {
        _authState.value = AuthState.Authenticating
        val request = TdApi.CheckAuthenticationCode(code)
        sendRequest(request) { result ->
            if (result is TdApi.Ok) {
                Log.d(TAG, "Verification code accepted")
            } else if (result is TdApi.Error) {
                val errorMsg = "Invalid code: ${result.message}"
                Log.e(TAG, errorMsg)
                _authState.value = AuthState.Error(errorMsg)
                callback?.onError(errorMsg)
            }
        }
    }
    
    fun sendPassword(password: String) {
        _authState.value = AuthState.Authenticating
        val request = TdApi.CheckAuthenticationPassword(password)
        sendRequest(request) { result ->
            if (result is TdApi.Ok) {
                Log.d(TAG, "Password accepted")
            } else if (result is TdApi.Error) {
                val errorMsg = "Invalid password: ${result.message}"
                Log.e(TAG, errorMsg)
                _authState.value = AuthState.Error(errorMsg)
                callback?.onError(errorMsg)
            }
        }
    }
    
    private fun registerUser(state: TdApi.AuthorizationStateWaitRegistration) {
        // For new users, register with first and last name
        val request = TdApi.RegisterUser("", "") // Empty names for now
        sendRequest(request) { result ->
            if (result is TdApi.Ok) {
                Log.d(TAG, "User registered successfully")
            } else if (result is TdApi.Error) {
                Log.e(TAG, "Registration failed: ${result.message}")
            }
        }
    }
    
    private fun getCurrentUser() {
        sendRequest(TdApi.GetMe()) { result ->
            if (result is TdApi.User) {
                val user = User(
                    userId = result.id,
                    username = result.username,
                    firstName = result.firstName,
                    lastName = result.lastName,
                    phoneNumber = result.phoneNumber ?: "",
                    isLoggedIn = true
                )
                
                // Save to database
                kotlinx.coroutines.GlobalScope.launch {
                    val existingUser = repository.getLoggedInUser()
                    if (existingUser == null) {
                        repository.insertUser(user)
                    } else {
                        repository.updateUser(user)
                    }
                    preferencesManager.setLastLoggedInUser(user.phoneNumber)
                }
                
                _authState.value = AuthState.Authenticated(user)
                callback?.onSuccess(user)
                Log.d(TAG, "User authenticated: ${user.firstName} ${user.lastName}")
            } else if (result is TdApi.Error) {
                val errorMsg = "Failed to get user info: ${result.message}"
                Log.e(TAG, errorMsg)
                _authState.value = AuthState.Error(errorMsg)
            }
        }
    }
    
    fun logout() {
        sendRequest(TdApi.LogOut()) { result ->
            if (result is TdApi.Ok) {
                kotlinx.coroutines.GlobalScope.launch {
                    repository.logout()
                    preferencesManager.clearAll()
                }
                _authState.value = AuthState.Unauthenticated
                Log.d(TAG, "Logged out successfully")
            }
        }
    }
    
    private fun sendRequest(request: TdApi.Function, callback: (TdApi.Object) -> Unit) {
        val queryId = currentQueryId++
        authorizationCallbacks[queryId] = callback
        
        tdlibClient?.send(request, object : Client.ResultHandler {
            override fun onResult(result: TdApi.Object) {
                authorizationCallbacks.remove(queryId)?.invoke(result)
            }
        })
    }
    
    fun destroy() {
        tdlibClient?.close()
        tdlibClient = null
    }
}