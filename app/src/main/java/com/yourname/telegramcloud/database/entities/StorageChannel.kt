package com.yourname.telegramcloud.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "storage_channels")
data class StorageChannel(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val channelId: Long,
    val channelUsername: String,
    val channelTitle: String,
    val minUserId: Int,
    val maxUserId: Int,
    val currentFileCount: Int = 0,
    val currentStorageBytes: Long = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)