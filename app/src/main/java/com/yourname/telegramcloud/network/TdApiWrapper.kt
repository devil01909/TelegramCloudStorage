package com.yourname.telegramcloud.network

import android.content.Context
import android.util.Log
import com.yourname.telegramcloud.utils.Constants
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.concurrent.atomic.AtomicLong

class TdApiWrapper(private val context: Context) {
    
    companion object {
        private const val TAG = "TdApiWrapper"
        private var instance: TdApiWrapper? = null
        
        fun getInstance(context: Context): TdApiWrapper {
            return instance ?: synchronized(this) {
                instance ?: TdApiWrapper(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private var client: Client? = null
    private val listener = TdlibListener()
    private var isInitialized = false
    
    // File download tracking
    private val downloadCallbacks = ConcurrentHashMap<Int, (TdApi.File) -> Unit>()
    private val uploadCallbacks = ConcurrentHashMap<Int, (TdApi.File) -> Unit>()
    private val fileIdGenerator = AtomicLong(0)
    
    init {
        initTdlib()
    }
    
    private fun initTdlib() {
        try {
            // Set up TDLib
            Client.setLogVerbosityLevel(1)
            Client.setLogStream(TdApi.LogStreamDefault())
            
            // Create client
            client = Client(listener, null, null)
            
            // Set update handler
            listener.setUpdateHandler { update ->
                handleUpdate(update)
            }
            
            // Send parameters
            sendParameters()
            
            isInitialized = true
            Log.d(TAG, "TDLib initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TDLib: ${e.message}", e)
        }
    }
    
    private fun sendParameters() {
        val parameters = TdApi.SetTdlibParameters().apply {
            useTestDc = false
            databaseDirectory = File(context.filesDir, Constants.TDLIB_DIRECTORY).absolutePath + "/database"
            filesDirectory = File(context.filesDir, Constants.TDLIB_DIRECTORY).absolutePath + "/files"
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
        
        send(parameters) { result ->
            if (result is TdApi.Ok) {
                Log.d(TAG, "Parameters set successfully")
                // Request authorization state
                send(TdApi.GetAuthorizationState()) { state ->
                    Log.d(TAG, "Authorization state: ${state::class.java.simpleName}")
                }
            } else {
                Log.e(TAG, "Failed to set parameters: $result")
            }
        }
    }
    
    private fun handleUpdate(update: TdApi.Object) {
        when (update) {
            is TdApi.UpdateFile -> {
                // File upload/download progress
                handleFileUpdate(update.file)
            }
            is TdApi.UpdateAuthorizationState -> {
                Log.d(TAG, "Auth state: ${update.authorizationState::class.java.simpleName}")
            }
            is TdApi.UpdateNewMessage -> {
                Log.d(TAG, "New message received")
            }
            else -> {
                // Other updates
            }
        }
    }
    
    private fun handleFileUpdate(file: TdApi.File) {
        // Check if this is a tracked file
        downloadCallbacks[file.id]?.let { callback ->
            if (file.local.isDownloadingCompleted) {
                // Download complete
                callback(file)
                downloadCallbacks.remove(file.id)
            } else if (file.local.downloadedSize > 0) {
                // Progress update
                callback(file)
            }
        }
        
        uploadCallbacks[file.id]?.let { callback ->
            if (file.remote.isUploadingCompleted) {
                // Upload complete
                callback(file)
                uploadCallbacks.remove(file.id)
            } else if (file.remote.uploadedSize > 0) {
                // Progress update
                callback(file)
            }
        }
    }
    
    // ========== AUTHENTICATION ==========
    
    fun getAuthorizationState(callback: (TdApi.AuthorizationState) -> Unit) {
        send(TdApi.GetAuthorizationState()) { result ->
            if (result is TdApi.AuthorizationState) {
                callback(result)
            }
        }
    }
    
    fun setPhoneNumber(phoneNumber: String, callback: (TdApi.Object) -> Unit) {
        send(TdApi.SetAuthenticationPhoneNumber(phoneNumber, null), callback)
    }
    
    fun checkAuthenticationCode(code: String, callback: (TdApi.Object) -> Unit) {
        send(TdApi.CheckAuthenticationCode(code), callback)
    }
    
    fun checkAuthenticationPassword(password: String, callback: (TdApi.Object) -> Unit) {
        send(TdApi.CheckAuthenticationPassword(password), callback)
    }
    
    // ========== CHANNEL OPERATIONS ==========
    
    fun createChannel(title: String, about: String, callback: (TdApi.Chat) -> Unit) {
        send(TdApi.CreateNewSupergroupChat(title, about, true)) { result ->
            if (result is TdApi.Chat) {
                callback(result)
            } else if (result is TdApi.Error) {
                Log.e(TAG, "Failed to create channel: ${result.message}")
            }
        }
    }
    
    fun getChannelInfo(chatId: Long, callback: (TdApi.Chat) -> Unit) {
        send(TdApi.GetChat(chatId)) { result ->
            if (result is TdApi.Chat) {
                callback(result)
            }
        }
    }
    
    fun joinChannel(inviteLink: String, callback: (TdApi.Chat) -> Unit) {
        send(TdApi.JoinChatByInviteLink(inviteLink)) { result ->
            if (result is TdApi.Chat) {
                callback(result)
            }
        }
    }
    
    // ========== FILE OPERATIONS ==========
    
    fun sendFileToChat(
        chatId: Long,
        filePath: String,
        caption: String = "",
        onProgress: ((progress: Float) -> Unit)? = null,
        onComplete: ((messageId: Long, fileId: String) -> Unit)? = null
    ) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e(TAG, "File not found: $filePath")
            return
        }
        
        // First, upload the file
        val inputFile = TdApi.InputFileLocal(filePath)
        val uploadFileId = fileIdGenerator.incrementAndGet().toInt()
        
        uploadCallbacks[uploadFileId] = { tdFile ->
            if (tdFile.local.isDownloadingCompleted) {
                // File uploaded, now send message
                val document = TdApi.InputMessageDocument(
                    TdApi.InputFileId(tdFile.id),
                    null,
                    true,
                    caption
                )
                
                send(TdApi.SendMessage(chatId, 0, null, null, null, document)) { result ->
                    if (result is TdApi.Message) {
                        onComplete?.invoke(result.id, tdFile.id.toString())
                    }
                }
            } else if (onProgress != null && tdFile.remote.uploadedSize > 0) {
                val progress = tdFile.remote.uploadedSize.toFloat() / tdFile.expectedSize
                onProgress(progress)
            }
        }
        
        // Start upload
        send(TdApi.AddFileToDownloads(uploadFileId.toString(), inputFile, null, null, null, false)) { result ->
            if (result is TdApi.File) {
                // File upload started
            }
        }
    }
    
    fun downloadFile(
        fileId: String,
        destinationPath: String,
        onProgress: ((progress: Float) -> Unit)? = null,
        onComplete: ((filePath: String) -> Unit)? = null
    ) {
        val fileIdInt = fileId.toIntOrNull()
        if (fileIdInt == null) {
            Log.e(TAG, "Invalid file ID: $fileId")
            return
        }
        
        downloadCallbacks[fileIdInt] = { tdFile ->
            if (tdFile.local.isDownloadingCompleted) {
                // File downloaded, copy to destination
                val sourceFile = File(tdFile.local.path)
                if (sourceFile.exists()) {
                    sourceFile.copyTo(File(destinationPath), overwrite = true)
                    onComplete?.invoke(destinationPath)
                }
                downloadCallbacks.remove(fileIdInt)
            } else if (onProgress != null && tdFile.local.downloadedSize > 0) {
                val progress = tdFile.local.downloadedSize.toFloat() / tdFile.expectedSize
                onProgress(progress)
            }
        }
        
        // Request file download
        send(TdApi.DownloadFile(fileIdInt, 32, 0, 0, true)) { result ->
            if (result is TdApi.File) {
                // Download started
            }
        }
    }
    
    fun deleteMessage(chatId: Long, messageId: Long, callback: ((Boolean) -> Unit)? = null) {
        send(TdApi.DeleteMessages(chatId, longArrayOf(messageId), true)) { result ->
            callback?.invoke(result is TdApi.Ok)
        }
    }
    
    fun getMessages(chatId: Long, limit: Int = 50, callback: (List<TdApi.Message>) -> Unit) {
        send(TdApi.GetChatHistory(chatId, 0, 0, limit, false)) { result ->
            if (result is TdApi.Messages) {
                callback(result.messages.toList())
            }
        }
    }
    
    // ========== SEARCH OPERATIONS ==========
    
    fun searchMessages(
        chatId: Long,
        query: String,
        limit: Int = 50,
        callback: (List<TdApi.Message>) -> Unit
    ) {
        send(TdApi.SearchChatMessages(chatId, query, null, 0, 0, limit, null, 0, 0)) { result ->
            if (result is TdApi.Messages) {
                callback(result.messages.toList())
            }
        }
    }
    
    // ========== UTILITIES ==========
    
    private fun send(request: TdApi.Function, callback: (TdApi.Object) -> Unit) {
        val queryId = listener.sendRequest(request, callback)
        client?.send(request, object : Client.ResultHandler {
            override fun onResult(result: TdApi.Object) {
                listener.handleResponse(queryId, result)
            }
        })
    }
    
    fun close() {
        client?.close()
        client = null
        listener.clear()
        isInitialized = false
    }
    
    fun isReady(): Boolean = isInitialized && client != null
}