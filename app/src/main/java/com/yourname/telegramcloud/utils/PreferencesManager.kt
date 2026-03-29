package com.yourname.telegramcloud.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFERENCE_NAME)

class PreferencesManager(private val context: Context) {
    
    companion object {
        // Keys
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_AUTO_UPLOAD_CAMERA = stringPreferencesKey("auto_upload_camera")
        val KEY_ENABLE_ENCRYPTION = stringPreferencesKey("enable_encryption")
        val KEY_NOTIFICATIONS_ENABLED = stringPreferencesKey("notifications_enabled")
        val KEY_LAST_LOGGED_IN_USER = stringPreferencesKey("last_logged_in_user")
        
        // Default values
        const val DEFAULT_THEME = "system"
        const val DEFAULT_AUTO_UPLOAD = "false"
        const val DEFAULT_ENCRYPTION = "false"
        const val DEFAULT_NOTIFICATIONS = "true"
    }
    
    // Theme preference
    val themeFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[KEY_THEME] ?: DEFAULT_THEME }
    
    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME] = theme
        }
    }
    
    // Auto upload preference
    val autoUploadFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[KEY_AUTO_UPLOAD_CAMERA]?.toBoolean() ?: DEFAULT_AUTO_UPLOAD.toBoolean() }
    
    suspend fun setAutoUpload(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_UPLOAD_CAMERA] = enabled.toString()
        }
    }
    
    // Encryption preference
    val encryptionEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[KEY_ENABLE_ENCRYPTION]?.toBoolean() ?: DEFAULT_ENCRYPTION.toBoolean() }
    
    suspend fun setEncryptionEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ENABLE_ENCRYPTION] = enabled.toString()
        }
    }
    
    // Notifications preference
    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[KEY_NOTIFICATIONS_ENABLED]?.toBoolean() ?: DEFAULT_NOTIFICATIONS.toBoolean() }
    
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] = enabled.toString()
        }
    }
    
    // Last logged in user
    suspend fun getLastLoggedInUser(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_LAST_LOGGED_IN_USER]
        }.first()
    }
    
    suspend fun setLastLoggedInUser(phoneNumber: String?) {
        context.dataStore.edit { preferences ->
            if (phoneNumber != null) {
                preferences[KEY_LAST_LOGGED_IN_USER] = phoneNumber
            } else {
                preferences.remove(KEY_LAST_LOGGED_IN_USER)
            }
        }
    }
    
    // Clear all preferences (on logout)
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}