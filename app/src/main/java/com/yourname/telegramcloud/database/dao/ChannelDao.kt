package com.yourname.telegramcloud.database.dao

import androidx.room.*
import com.yourname.telegramcloud.database.entities.StorageChannel
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    
    @Query("SELECT * FROM storage_channels ORDER BY minUserId ASC")
    fun getAllChannels(): Flow<List<StorageChannel>>
    
    @Query("SELECT * FROM storage_channels WHERE channelId = :channelId")
    suspend fun getChannelById(channelId: Long): StorageChannel?
    
    @Query("SELECT * FROM storage_channels WHERE :userId BETWEEN minUserId AND maxUserId AND isActive = 1")
    suspend fun getChannelForUser(userId: Int): StorageChannel?
    
    @Query("SELECT * FROM storage_channels WHERE isActive = 1 ORDER BY currentFileCount ASC LIMIT 1")
    suspend fun getLeastUsedChannel(): StorageChannel?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: StorageChannel): Long
    
    @Update
    suspend fun updateChannel(channel: StorageChannel)
    
    @Query("UPDATE storage_channels SET currentFileCount = currentFileCount + 1, currentStorageBytes = currentStorageBytes + :fileSize WHERE channelId = :channelId")
    suspend fun incrementChannelStats(channelId: Long, fileSize: Long)
    
    @Query("UPDATE storage_channels SET currentFileCount = currentFileCount - 1, currentStorageBytes = currentStorageBytes - :fileSize WHERE channelId = :channelId")
    suspend fun decrementChannelStats(channelId: Long, fileSize: Long)
    
    @Query("SELECT SUM(currentStorageBytes) FROM storage_channels")
    suspend fun getTotalStorageUsed(): Long
    
    @Query("SELECT COUNT(*) FROM storage_channels")
    suspend fun getChannelCount(): Int
}