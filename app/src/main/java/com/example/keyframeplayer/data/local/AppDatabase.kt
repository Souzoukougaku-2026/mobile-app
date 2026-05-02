package com.example.keyframeplayer.data.local


import androidx.room.Database
import androidx.room.RoomDatabase


@Database(
    entities = [CropImageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cropImageDao(): CropImageDao
}