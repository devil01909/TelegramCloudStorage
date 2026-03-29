package com.yourname.telegramcloud.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "files",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId", "folderPath"]),
        Index(value = ["fileHash"]),
        Index(value = ["uploadedAt"])
    ]
)
data class StorageFile(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Long,
    val fileId: String, // Telegram file ID
    val fileName: String,
    val fileSize: Long,
    val fileHash: String? = null,
    val mimeType: String? = null,
    val folderPath: String = "/",
    val channelId: Long,
    val messageId: Int,
    val isEncrypted: Boolean = false,
    val isFavorite: Boolean = false,
    val tags: String? = null, // JSON array of tags
    val uploadedAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val accessCount: Int = 0
)