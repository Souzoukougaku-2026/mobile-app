package com.example.keyframeplayer.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.keyframeplayer.core.data.database.dao.CropImageDao
import com.example.keyframeplayer.core.data.database.dao.KeyFrameDao
import com.example.keyframeplayer.core.data.database.dao.VideoDao
import com.example.keyframeplayer.core.data.database.entity.CropImageEntity
import com.example.keyframeplayer.core.data.database.entity.KeyFrameEntity
import com.example.keyframeplayer.core.data.database.entity.VideoEntity

@Database(
    entities = [CropImageEntity::class, KeyFrameEntity::class, VideoEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cropImageDao(): CropImageDao
    abstract fun keyFrameDao(): KeyFrameDao
    abstract fun videoDao(): VideoDao
}
