package com.yourname.telegramcloud.storage

import android.content.Context
import android.util.Log
import com.yourname.telegramcloud.database.entities.StorageChannel
import com.yourname.telegramcloud.database.repository.StorageRepository
import com.yourname.telegramcloud.network.TdApiWrapper
import com.yourname.telegramcloud.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.drinkless.tdlib.TdApi
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ChannelManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ChannelManager"
        private var instance: ChannelManager? = null
        
        fun getInstance(context: Context): ChannelManager {
            return instance ?: synchronized(this) {
                instance ?: ChannelManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val repository = StorageRepository(context)
    private val tdApi = TdApiWrapper.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Cache of active channels
    private val channelCache = mutableMapOf<Long, StorageChannel>()
    
    /**
     * Get or create a storage channel for a user (Option C - Hybrid approach)
     */
    suspend fun getChannelForUser(userId: Long): StorageChannel {
        // Calculate shard based on user ID
        val shardNumber = (userId / Constants.CHANNEL_SHARD_SIZE).toInt()
        val minUserId = shardNumber * Constants.CHANNEL_SHARD_SIZE
        val maxUserId = minUserId + Constants.CHANNEL_SHARD_SIZE - 1
        
        // Check if channel exists in database
        val existingChannel = repository.getChannelForUser(minUserId)
        
        if (existingChannel != null) {
            return existingChannel
        }
        
        // Create new channel for this shard
        return createNewChannel(shardNumber, minUserId, maxUserId)
    }
    
    /**
     * Create a new storage channel for a shard
     */
    private suspend fun createNewChannel(shardNumber: Int, minUserId: Int, maxUserId: Int): StorageChannel {
        val channelTitle = "Storage_${minUserId}_${maxUserId}"
        val channelAbout = "Telegram Cloud Storage for users ${minUserId}-${maxUserId}"
        
        return suspendCancellableCoroutine { continuation ->
            tdApi.createChannel(channelTitle, channelAbout) { chat ->
                scope.launch {
                    try {
                        val storageChannel = StorageChannel(
                            channelId = chat.id,
                            channelUsername = chat.username ?: "",
                            channelTitle = chat.title,
                            minUserId = minUserId,
                            maxUserId = maxUserId,
                            currentFileCount = 0,
                            currentStorageBytes = 0,
                            isActive = true,
                            createdAt = System.currentTimeMillis()
                        )
                        
                        repository.insertChannel(storageChannel)
                        channelCache[chat.id] = storageChannel
                        
                        Log.d(TAG, "Created new channel: $channelTitle (ID: ${chat.id})")
                        continuation.resume(storageChannel)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to create channel: ${e.message}", e)
                        continuation.resumeWithException(e)
                    }
                }
            }
        }
    }
    
    /**
     * Get the least used channel for load balancing
     */
    suspend fun getLeastUsedChannel(): StorageChannel? {
        return repository.getLeastUsedChannel()
    }
    
    /**
     * Update channel statistics after file operations
     */
    suspend fun updateChannelStats(channelId: Long, fileSize: Long, isAdd: Boolean) {
        if (isAdd) {
            repository.incrementChannelStats(channelId, fileSize)
        } else {
            repository.decrementChannelStats(channelId, fileSize)
        }
        
        // Update cache
        channelCache[channelId]?.let { channel ->
            if (isAdd) {
                channelCache[channelId] = channel.copy(
                    currentFileCount = channel.currentFileCount + 1,
                    currentStorageBytes = channel.currentStorageBytes + fileSize
                )
            } else {
                channelCache[channelId] = channel.copy(
                    currentFileCount = channel.currentFileCount - 1,
                    currentStorageBytes = channel.currentStorageBytes - fileSize
                )
            }
        }
    }
    
    /**
     * Get total storage usage across all channels
     */
    suspend fun getTotalStorageUsed(): Long {
        return repository.getTotalStorageUsed()
    }
    
    /**
     * Validate if a channel exists and is accessible
     */
    suspend fun validateChannel(channelId: Long): Boolean {
        return suspendCancellableCoroutine { continuation ->
            tdApi.getChannelInfo(channelId) { chat ->
                continuation.resume(chat.id == channelId)
            }
        }
    }
    
    /**
     * Get channel by ID from cache or database
     */
    suspend fun getChannelById(channelId: Long): StorageChannel? {
        // Check cache first
        channelCache[channelId]?.let { return it }
        
        // Query database
        val channel = repository.getAllChannels().firstOrNull { it.channelId == channelId }
        channel?.let { channelCache[channelId] = it }
        
        return channel
    }
    
    /**
     * Get all storage channels
     */
    suspend fun getAllChannels(): List<StorageChannel> {
        return repository.getAllChannels().firstOrNull() ?: emptyList()
    }
}