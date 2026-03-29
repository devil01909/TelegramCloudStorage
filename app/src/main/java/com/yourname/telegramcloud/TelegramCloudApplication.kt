package com.yourname.telegramcloud

import android.app.Application
import android.util.Log
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class TelegramCloudApplication : Application() {
    
    companion object {
        lateinit var instance: TelegramCloudApplication
            private set
        
        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private const val TAG = "TelegramCloudApp"
    }
    
    private val dataStore by lazy {
        PreferenceDataStoreFactory.create(
            corruptionHandler = null,
            migrations = listOf(),
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        ) {
            preferencesDataStoreFile(this, "telegram_cloud_settings")
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "Application initialized")
    }
    
    fun getDataStore() = dataStore
}