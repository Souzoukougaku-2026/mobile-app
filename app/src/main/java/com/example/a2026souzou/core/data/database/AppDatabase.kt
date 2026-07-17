package com.example.a2026souzou.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.a2026souzou.core.data.database.dao.CropImageDao
import com.example.a2026souzou.core.data.database.dao.KeyFrameDao
import com.example.a2026souzou.core.data.database.entity.CropImageEntity
import com.example.a2026souzou.core.data.database.entity.KeyFrameEntity

@Database(
    entities = [CropImageEntity::class, KeyFrameEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cropImageDao(): CropImageDao
    abstract fun keyFrameDao(): KeyFrameDao
}
