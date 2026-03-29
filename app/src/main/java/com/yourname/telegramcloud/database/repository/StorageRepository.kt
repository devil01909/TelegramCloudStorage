package com.yourname.telegramcloud.database.repository

import android.content.Context
import com.yourname.telegramcloud.database.AppDatabase
import com.yourname.telegramcloud.database.entities.StorageChannel
import com.yourname.telegramcloud.database.entities.StorageFile
import com.yourname.telegramcloud.database.entities.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class StorageRepository(private val context: Context) {
    
    private val database = AppDatabase.getInstance(context)
    private val userDao = database.userDao()
    private val channelDao = database.channelDao()
    private val fileDao = database.fileDao()
    
    // User operations
    suspend fun getLoggedInUser(): User? = userDao.getLoggedInUser()
    
    suspend fun insertUser(user: User): Long = userDao.insertUser(user)
    
    suspend fun updateUser(user: User) = userDao.updateUser(user)
    
    suspend fun logout() = userDao.logoutAllUsers()
    
    suspend fun updateLastActive(userId: Long) = userDao.updateLastActive(userId)
    
    // Channel operations
    fun getAllChannels(): Flow<List<StorageChannel>> = channelDao.getAllChannels()
    
    suspend fun getChannelForUser(userId: Int): StorageChannel? = channelDao.getChannelForUser(userId)
    
    suspend fun getLeastUsedChannel(): StorageChannel? = channelDao.getLeastUsedChannel()
    
    suspend fun insertChannel(channel: StorageChannel): Long = channelDao.insertChannel(channel)
    
    suspend fun incrementChannelStats(channelId: Long, fileSize: Long) = 
        channelDao.incrementChannelStats(channelId, fileSize)
    
    suspend fun decrementChannelStats(channelId: Long, fileSize: Long) = 
        channelDao.decrementChannelStats(channelId, fileSize)
    
    suspend fun getTotalStorageUsed(): Long = channelDao.getTotalStorageUsed()
    
    // File operations
    fun getFilesInFolder(userId: Long, folderPath: String): Flow<List<StorageFile>> = 
        fileDao.getFilesInFolder(userId, folderPath)
    
    fun getRecentFiles(userId: Long, limit: Int = 20): Flow<List<StorageFile>> = 
        fileDao.getRecentFiles(userId, limit)
    
    fun getFavoriteFiles(userId: Long): Flow<List<StorageFile>> = 
        fileDao.getFavoriteFiles(userId)
    
    fun searchFiles(userId: Long, query: String): Flow<List<StorageFile>> = 
        fileDao.searchFiles(userId, query)
    
    suspend fun insertFile(file: StorageFile): Long = fileDao.insertFile(file)
    
    suspend fun updateFile(file: StorageFile) = fileDao.updateFile(file)
    
    suspend fun setFavorite(fileId: Int, isFavorite: Boolean) = 
        fileDao.setFavorite(fileId, isFavorite)
    
    suspend fun moveFile(fileId: Int, newPath: String) = 
        fileDao.moveFile(fileId, newPath)
    
    suspend fun recordAccess(fileId: Int) = fileDao.recordAccess(fileId)
    
    suspend fun deleteFile(file: StorageFile) = fileDao.deleteFile(file)
    
    suspend fun getFileCount(userId: Long): Int = fileDao.getFileCount(userId)
    
    suspend fun getUserTotalStorage(userId: Long): Long = fileDao.getTotalStorageUsed(userId)
    
    suspend fun findFileByHash(userId: Long, hash: String): StorageFile? = 
        fileDao.findFileByHash(userId, hash)
}