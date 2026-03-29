package com.yourname.telegramcloud.storage

import android.content.Context
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

class DownloadManager(private val context: Context) {
    
    companion object {
        private const val TAG = "DownloadManager"
        private var instance: DownloadManager? = null
        
        fun getInstance(context: Context): DownloadManager {
            return instance ?: synchronized(this) {
                instance ?: DownloadManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val repository = StorageRepository(context)
    private val tdApi = TdApiWrapper.getInstance(context)
    private val encryptionManager = EncryptionManager.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Download queue and state
    private val _downloadQueue = MutableStateFlow<List<DownloadTask>>(emptyList())
    val downloadQueue: StateFlow<List<DownloadTask>> = _downloadQueue.asStateFlow()
    
    data class DownloadTask(
        val id: String,
        val fileId: Int,
        val fileName: String,
        val fileSize: Long,
        var progress: Float = 0f,
        var status: DownloadStatus = DownloadStatus.PENDING,
        var localPath: String? = null
    )
    
    enum class DownloadStatus {
        PENDING,
        DOWNLOADING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
    
    private val downloadTasks = mutableMapOf<String, DownloadTask>()
    
    /**
     * Download a file from Telegram storage
     */
    suspend fun downloadFile(
        storageFile: StorageFile,
        destinationPath: String,
        onProgress: ((Float) -> Unit)? = null,
        onComplete: ((String) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val taskId = System.currentTimeMillis().toString()
        val task = DownloadTask(
            id = taskId,
            fileId = storageFile.id,
            fileName = storageFile.fileName,
            fileSize = storageFile.fileSize
        )
        
        downloadTasks[taskId] = task
        updateDownloadQueue()
        
        scope.launch {
            try {
                updateTaskStatus(taskId, DownloadStatus.DOWNLOADING)
                
                // Create temp file for download
                val tempFile = File(context.cacheDir, "temp_${storageFile.fileId}")
                
                tdApi.downloadFile(
                    fileId = storageFile.fileId,
                    destinationPath = tempFile.absolutePath
                ) { progress ->
                    task.progress = progress
                    onProgress?.invoke(progress)
                    updateDownloadQueue()
                } onComplete = { downloadedPath ->
                    // Decrypt if needed
                    val finalPath = if (storageFile.isEncrypted) {
                        val decryptedFile = encryptionManager.decryptFile(
                            File(downloadedPath),
                            File(destinationPath)
                        )
                        decryptedFile?.absolutePath ?: destinationPath
                    } else {
                        // Copy to destination
                        File(downloadedPath).copyTo(File(destinationPath), overwrite = true)
                        destinationPath
                    }
                    
                    // Update access count in database
                    repository.recordAccess(storageFile.id)
                    
                    task.localPath = finalPath
                    updateTaskStatus(taskId, DownloadStatus.COMPLETED)
                    onComplete?.invoke(finalPath)
                    
                    // Clean up temp file
                    tempFile.delete()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Download failed: ${e.message}", e)
                updateTaskStatus(taskId, DownloadStatus.FAILED)
                onError?.invoke(e.message ?: "Download failed")
            }
        }
    }
    
    /**
     * Cancel an ongoing download
     */
    fun cancelDownload(taskId: String) {
        downloadTasks[taskId]?.let { task ->
            updateTaskStatus(taskId, DownloadStatus.CANCELLED)
            // TODO: Implement actual cancellation with TDLib
        }
    }
    
    private fun updateTaskStatus(taskId: String, status: DownloadStatus) {
        downloadTasks[taskId]?.let { task ->
            downloadTasks[taskId] = task.copy(status = status)
            updateDownloadQueue()
        }
    }
    
    private fun updateDownloadQueue() {
        _downloadQueue.value = downloadTasks.values.toList()
    }
    
    fun clearCompletedDownloads() {
        downloadTasks.values.removeAll { it.status == DownloadStatus.COMPLETED || it.status == DownloadStatus.CANCELLED }
        updateDownloadQueue()
    }
}