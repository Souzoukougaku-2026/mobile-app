package com.example.a2026souzou.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.a2026souzou.core.data.database.entity.KeyFrameEntity
import java.util.UUID

@Dao
interface KeyFrameDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeyFrames(keyFrames: List<KeyFrameEntity>)

    @Query("SELECT * FROM KeyFrame WHERE id = :id")
    suspend fun getKeyFrameById(id: UUID): KeyFrameEntity?
}
