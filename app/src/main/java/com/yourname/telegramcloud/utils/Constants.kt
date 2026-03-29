package com.yourname.telegramcloud.utils

object Constants {
    
    // ========== TELEGRAM API CREDENTIALS ==========
    // REPLACE THESE WITH YOUR ACTUAL VALUES FROM my.telegram.org
    const val TELEGRAM_API_ID = 39574983  // ← REPLACE WITH YOUR API ID
    const val TELEGRAM_API_HASH = "b18b09477d27b64aac5be44408cf6d15"  // ← REPLACE WITH YOUR API HASH
    
    // ========== APP CONFIGURATION ==========
    const val APP_VERSION = "1.0.0"
    const val DATABASE_NAME = "telegram_cloud_db"
    const val PREFERENCE_NAME = "telegram_cloud_prefs"
    
    // ========== STORAGE CHANNEL CONFIGURATION ==========
    const val CHANNEL_SHARD_SIZE = 1000  // Users per channel shard
    const val MAX_FILE_SIZE_BYTES = 2L * 1024 * 1024 * 1024 // 2GB
    const val MAX_CONCURRENT_UPLOADS = 3
    const val MAX_CONCURRENT_DOWNLOADS = 3
    
    // ========== FOLDER STRUCTURE ==========
    const val ROOT_FOLDER = "/"
    const val DEFAULT_FOLDER = "/"
    
    // ========== NOTIFICATION IDs ==========
    const val NOTIFICATION_ID_UPLOAD = 1001
    const val NOTIFICATION_ID_DOWNLOAD = 1002
    const val NOTIFICATION_ID_SYNC = 1003
    
    // ========== REQUEST CODES ==========
    const val REQUEST_CODE_STORAGE_PERMISSION = 101
    const val REQUEST_CODE_PICK_FILE = 102
    const val REQUEST_CODE_PICK_IMAGE = 103
    
    // ========== TDLIB CONFIGURATION ==========
    const val TDLIB_DIRECTORY = "tdlib"
    const val TDLIB_API_VERSION = "1.8.0"
    
    // ========== SHARE LINK CONFIGURATION ==========
    const val SHARE_LINK_EXPIRY_DAYS = 7
    const val MAX_SHARE_LINKS_PER_FILE = 10
    
    // ========== CACHE CONFIGURATION ==========
    const val CACHE_SIZE_MB = 100
    const val THUMBNAIL_CACHE_SIZE_MB = 50
    
    // ========== ENCRYPTION ==========
    const val ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding"
    const val KEY_SIZE_BITS = 256
    
    // ========== API LIMITS ==========
    const val MAX_MESSAGES_PER_SECOND = 20
    const val MAX_DOWNLOADS_PER_SECOND = 30
    const val BATCH_SIZE = 50
}