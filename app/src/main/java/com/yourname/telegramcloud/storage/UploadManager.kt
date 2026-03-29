package com.yourname.telegramcloud.storage

import android.content.Context
import android.net.Uri
import android.util.Log
import com.yourname.telegramcloud.database.entities.StorageFile
import com.yourname.telegramcloud.database.repository.StorageRepository
import com.yourname.telegramcloud.encryption.EncryptionManager
import com.yourname.telegramcloud.network.TdApiWrapper
import com.yourname.telegramcloud.utils.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest

class UploadManager(private val context: Context) {
    
    companion object {
        private const val TAG = "UploadManager"
        private var instance: UploadManager? = null
        
        fun getInstance(context: Context): UploadManager {
            return instance ?: synchronized(this) {
                instance ?: UploadManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val repository = StorageRepository(context)
    private val tdApi = TdApiWrapper.getInstance(context)
    private val channelManager = ChannelManager.getInstance(context)
    private val encryptionManager = EncryptionManager.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Upload queue and state
    private val _uploadQueue = MutableStateFlow<List<UploadTask>>(emptyList())
    val uploadQueue: StateFlow<List<UploadTask>> = _uploadQueue.asStateFlow()
    
    private val _currentUpload = MutableStateFlow<UploadTask?>(null)
    val currentUpload: StateFlow<UploadTask?> = _currentUpload.asStateFlow()
    
    data class UploadTask(
        val id: String,
        val fileName: String,
        val filePath: String,
        val fileSize: Long,
        val userId: Long,
        val folderPath: String,
        var progress: Float = 0f,
        var status: UploadStatus = UploadStatus.PENDING,
        var fileHash: String? = null
    )
    
    enum class UploadStatus {
        PENDING,
        UPLOADING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
    
    private val uploadTasks = mutableMapOf<String, UploadTask>()
    
    /**
     * Upload a file to Telegram storage
     */
    suspend fun uploadFile(
        fileUri: Uri,
        userId: Long,
        folderPath: String = "/",
        onProgress: ((Float) -> Unit)? = null,
        onComplete: ((StorageFile) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        // Get file details
        val fileInfo = FileUtils.getFileInfo(context, fileUri)
        if (fileInfo == null) {
            onError?.invoke("Could not read file info")
            return
        }
        
        // Check file size limit
        if (fileInfo.size > Constants.MAX_FILE_SIZE_BYTES) {
            onError?.invoke("File too large. Max size: 2GB")
            return
        }
        
        // Create upload task
        val taskId = System.currentTimeMillis().toString()
        val task = UploadTask(
            id = taskId,
            fileName = fileInfo.name,
            filePath = FileUtils.getRealPathFromUri(context, fileUri) ?: "",
            fileSize = fileInfo.size,
            userId = userId,
            folderPath = folderPath
        )
        
        uploadTasks[taskId] = task
        updateUploadQueue()
        _currentUpload.value = task
        
        scope.launch {
            try {
                // Update status
                updateTaskStatus(taskId, UploadStatus.UPLOADING)
                
                // Calculate file hash for deduplication
                val fileHash = calculateFileHash(fileUri)
                task.fileHash = fileHash
                
                // Check for duplicate
                val existingFile = repository.findFileByHash(userId, fileHash)
                if (existingFile != null) {
                    // File already exists, just reference it
                    updateTaskStatus(taskId, UploadStatus.COMPLETED)
                    onComplete?.invoke(existingFile)
                    return@launch
                }
                
                // Get storage channel for this user
                val channel = channelManager.getChannelForUser(userId)
                
                // Encrypt if enabled
                val finalFilePath = if (encryptionManager.isEncryptionEnabled()) {
                    val encryptedFile = encryptionManager.encryptFile(fileUri, taskId)
                    encryptedFile?.absolutePath ?: task.filePath
                } else {
                    task.filePath
                }
                
                // Upload to Telegram
                tdApi.sendFileToChat(
                    chatId = channel.channelId,
                    filePath = finalFilePath,
                    caption = "user:${userId}|name:${task.fileName}|hash:${fileHash}|path:${folderPath}"
                ) { progress ->
                    task.progress = progress
                    onProgress?.invoke(progress)
                    updateUploadQueue()
                } onComplete = { messageId, fileId ->
                    // Save to database
                    val storageFile = StorageFile(
                        userId = userId,
                        fileId = fileId,
                        fileName = task.fileName,
                        fileSize = task.fileSize,
                        fileHash = fileHash,
                        mimeType = fileInfo.mimeType,
                        folderPath = folderPath,
                        channelId = channel.channelId,
                        messageId = messageId.toInt(),
                        isEncrypted = encryptionManager.isEncryptionEnabled(),
                        uploadedAt = System.currentTimeMillis()
                    )
                    
                    repository.insertFile(storageFile)
                    
                    // Update channel stats
                    channelManager.updateChannelStats(channel.channelId, task.fileSize, true)
                    
                    updateTaskStatus(taskId, UploadStatus.COMPLETED)
                    onComplete?.invoke(storageFile)
                    
                    // Clean up temp encrypted file
                    if (encryptionManager.isEncryptionEnabled()) {
                        File(finalFilePath).delete()
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Upload failed: ${e.message}", e)
                updateTaskStatus(taskId, UploadStatus.FAILED)
                onError?.invoke(e.message ?: "Upload failed")
            } finally {
                _currentUpload.value = null
            }
        }
    }
    
    /**
     * Cancel an ongoing upload
     */
    fun cancelUpload(taskId: String) {
        uploadTasks[taskId]?.let { task ->
            updateTaskStatus(taskId, UploadStatus.CANCELLED)
            // TODO: Implement actual cancellation with TDLib
        }
    }
    
    /**
     * Calculate file hash for deduplication
     */
    private suspend fun calculateFileHash(fileUri: Uri): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(fileUri)
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            var bytesRead: Int
            
            while (inputStream?.read(buffer).also { bytesRead = it ?: -1 } != -1) {
                if (bytesRead > 0) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            
            inputStream?.close()
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate hash: ${e.message}", e)
            System.currentTimeMillis().toString() // Fallback
        }
    }
    
    private fun updateTaskStatus(taskId: String, status: UploadStatus) {
        uploadTasks[taskId]?.let { task ->
            uploadTasks[taskId] = task.copy(status = status)
            updateUploadQueue()
        }
    }
    
    private fun updateUploadQueue() {
        _uploadQueue.value = uploadTasks.values.toList()
    }
    
    fun clearCompletedUploads() {
        uploadTasks.values.removeAll { it.status == UploadStatus.COMPLETED || it.status == UploadStatus.CANCELLED }
        updateUploadQueue()
    }
}