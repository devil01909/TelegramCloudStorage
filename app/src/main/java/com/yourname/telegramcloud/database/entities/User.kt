package com.yourname.telegramcloud.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val userId: Long,
    val username: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String,
    val authKey: String? = null,
    val isLoggedIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
)