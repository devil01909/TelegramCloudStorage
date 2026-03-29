package com.yourname.telegramcloud.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yourname.telegramcloud.database.dao.ChannelDao
import com.yourname.telegramcloud.database.dao.FileDao
import com.yourname.telegramcloud.database.dao.UserDao
import com.yourname.telegramcloud.database.entities.StorageChannel
import com.yourname.telegramcloud.database.entities.StorageFile
import com.yourname.telegramcloud.database.entities.User

@Database(
    entities = [User::class, StorageChannel::class, StorageFile::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun channelDao(): ChannelDao
    abstract fun fileDao(): FileDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "telegram_cloud_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}