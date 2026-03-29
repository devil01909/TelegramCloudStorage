package com.yourname.telegramcloud.database.dao

import androidx.room.*
import com.yourname.telegramcloud.database.entities.StorageFile
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    
    @Query("SELECT * FROM files WHERE userId = :userId AND folderPath = :folderPath ORDER BY isFavorite DESC, uploadedAt DESC")
    fun getFilesInFolder(userId: Long, folderPath: String): Flow<List<StorageFile>>
    
    @Query("SELECT * FROM files WHERE userId = :userId ORDER BY uploadedAt DESC LIMIT :limit")
    fun getRecentFiles(userId: Long, limit: Int = 20): Flow<List<StorageFile>>
    
    @Query("SELECT * FROM files WHERE userId = :userId AND isFavorite = 1 ORDER BY uploadedAt DESC")
    fun getFavoriteFiles(userId: Long): Flow<List<StorageFile>>
    
    @Query("SELECT * FROM files WHERE userId = :userId AND fileHash = :fileHash LIMIT 1")
    suspend fun findFileByHash(userId: Long, fileHash: String): StorageFile?
    
    @Query("SELECT * FROM files WHERE fileId = :fileId LIMIT 1")
    suspend fun findFileByTelegramId(fileId: String): StorageFile?
    
    @Query("SELECT * FROM files WHERE userId = :userId AND fileName LIKE '%' || :query || '%'")
    fun searchFiles(userId: Long, query: String): Flow<List<StorageFile>>
    
    @Query("SELECT * FROM files WHERE userId = :userId AND mimeType LIKE :mimeType || '%'")
    fun getFilesByType(userId: Long, mimeType: String): Flow<List<StorageFile>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: StorageFile): Long
    
    @Update
    suspend fun updateFile(file: StorageFile)
    
    @Query("UPDATE files SET isFavorite = :isFavorite WHERE id = :fileId")
    suspend fun setFavorite(fileId: Int, isFavorite: Boolean)
    
    @Query("UPDATE files SET folderPath = :newPath WHERE id = :fileId")
    suspend fun moveFile(fileId: Int, newPath: String)
    
    @Query("UPDATE files SET lastAccessedAt = :timestamp, accessCount = accessCount + 1 WHERE id = :fileId")
    suspend fun recordAccess(fileId: Int, timestamp: Long = System.currentTimeMillis())
    
    @Delete
    suspend fun deleteFile(file: StorageFile)
    
    @Query("DELETE FROM files WHERE userId = :userId")
    suspend fun deleteAllUserFiles(userId: Long)
    
    @Query("SELECT COUNT(*) FROM files WHERE userId = :userId")
    suspend fun getFileCount(userId: Long): Int
    
    @Query("SELECT SUM(fileSize) FROM files WHERE userId = :userId")
    suspend fun getTotalStorageUsed(userId: Long): Long
}