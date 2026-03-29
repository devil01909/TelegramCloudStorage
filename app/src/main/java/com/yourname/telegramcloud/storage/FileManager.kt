package com.yourname.telegramcloud.storage

import android.content.Context
import android.util.Log
import com.yourname.telegramcloud.database.entities.StorageFile
import com.yourname.telegramcloud.database.repository.StorageRepository
import com.yourname.telegramcloud.network.TdApiWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FileManager(private val context: Context) {
    
    companion object {
        private const val TAG = "FileManager"
        private var instance: FileManager? = null
        
        fun getInstance(context: Context): FileManager {
            return instance ?: synchronized(this) {
                instance ?: FileManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val repository = StorageRepository(context)
    private val tdApi = TdApiWrapper.getInstance(context)
    private val channelManager = ChannelManager.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    
    /**
     * Get files in a folder
     */
    fun getFilesInFolder(userId: Long, folderPath: String): Flow<List<StorageFile>> {
        return repository.getFilesInFolder(userId, folderPath)
    }
    
    /**
     * Get recent files
     */
    fun getRecentFiles(userId: Long, limit: Int = 20): Flow<List<StorageFile>> {
        return repository.getRecentFiles(userId, limit)
    }
    
    /**
     * Get favorite files
     */
    fun getFavoriteFiles(userId: Long): Flow<List<StorageFile>> {
        return repository.getFavoriteFiles(userId)
    }
    
    /**
     * Search files
     */
    fun searchFiles(userId: Long, query: String): Flow<List<StorageFile>> {
        return repository.searchFiles(userId, query)
    }
    
    /**
     * Delete a file from storage
     */
    suspend fun deleteFile(storageFile: StorageFile, userId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Delete from Telegram channel
                tdApi.deleteMessage(storageFile.channelId, storageFile.messageId.toLong()) { success ->
                    if (success) {
                        // Update channel stats
                        scope.launch {
                            channelManager.updateChannelStats(storageFile.channelId, storageFile.fileSize, false)
                        }
                        
                        // Delete from database
                        scope.launch {
                            repository.deleteFile(storageFile)
                        }
                        
                        Log.d(TAG, "File deleted: ${storageFile.fileName}")
                    }
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete file: ${e.message}", e)
                false
            }
        }
    }
    
    /**
     * Rename a file
     */
    suspend fun renameFile(fileId: Int, newName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = repository.getFilesInFolder(0, "").firstOrNull { it.id == fileId }
                file?.let {
                    val updatedFile = it.copy(fileName = newName)
                    repository.updateFile(updatedFile)
                    true
                } ?: false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to rename file: ${e.message}", e)
                false
            }
        }
    }
    
    /**
     * Move file to different folder
     */
    suspend fun moveFile(fileId: Int, newFolderPath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                repository.moveFile(fileId, newFolderPath)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to move file: ${e.message}", e)
                false
            }
        }
    }
    
    /**
     * Toggle favorite status
     */
    suspend fun toggleFavorite(fileId: Int, isFavorite: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                repository.setFavorite(fileId, isFavorite)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle favorite: ${e.message}", e)
                false
            }
        }
    }
    
    /**
     * Create a new folder (virtual folder in database)
     */
    fun createFolder(userId: Long, folderPath: String): Boolean {
        // Folders are virtual - just store the path structure
        // We'll create a placeholder entry or just ensure path exists
        return true
    }
    
    /**
     * Delete a folder and all its contents
     */
    suspend fun deleteFolder(userId: Long, folderPath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val files = repository.getFilesInFolder(userId, folderPath).firstOrNull()
                files?.forEach { file ->
                    deleteFile(file, userId)
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete folder: ${e.message}", e)
                false
            }
        }
    }
    
    /**
     * Get storage statistics for user
     */
    suspend fun getUserStorageStats(userId: Long): StorageStats {
        return withContext(Dispatchers.IO) {
            val totalFiles = repository.getFileCount(userId)
            val totalSize = repository.getUserTotalStorage(userId)
            StorageStats(totalFiles, totalSize)
        }
    }
    
    data class StorageStats(
        val fileCount: Int,
        val totalSizeBytes: Long
    ) {
        val totalSizeMB: Float get() = totalSizeBytes / (1024f * 1024f)
        val totalSizeGB: Float get() = totalSizeBytes / (1024f * 1024f * 1024f)
    }
}