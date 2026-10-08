package com.example.keyframeplayer.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.keyframeplayer.core.data.database.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Query("SELECT * FROM Video ORDER BY realStartTime ASC")
    fun getAllVideosOrdered(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM Video ORDER BY realStartTime ASC")
    suspend fun getAllVideosListOrdered(): List<VideoEntity>

    @Query("DELETE FROM Video")
    suspend fun clearVideos()
}
