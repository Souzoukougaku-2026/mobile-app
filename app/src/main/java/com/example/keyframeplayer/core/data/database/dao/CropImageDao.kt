package com.example.keyframeplayer.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.keyframeplayer.core.data.database.entity.CropImageEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface CropImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCropImages(cropImages: List<CropImageEntity>)

    @Query("""
        SELECT * FROM CropImage 
        WHERE (:className IS NULL OR class_name = :className)
        AND (:color IS NULL OR color = :color)
        AND realTime BETWEEN :startTime AND :endTime
        ORDER BY 
            CASE WHEN :isAsc = 1 THEN realTime END ASC,
            CASE WHEN :isAsc = 0 THEN realTime END DESC
        LIMIT :limit
    """)
    fun searchCropImages(
        className: String?,
        color: Int?,
        startTime: Long,
        endTime: Long,
        isAsc: Boolean,
        limit: Int = 20
    ): Flow<List<CropImageEntity>>

    @Query("SELECT * FROM CropImage WHERE id = :id")
    suspend fun getCropImageById(id: UUID): CropImageEntity?
}
