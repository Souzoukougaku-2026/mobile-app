package com.example.keyframeplayer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CropImageDao {
    @Query("SELECT * FROM crop_images ORDER BY timestampFileTime ASC")
    fun getAllFlow(): Flow<List<CropImage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CropImage>)

    @Query("DELETE FROM crop_images")
    suspend fun deleteAll()

    // 既存のメソッドの代替
    @Query("SELECT * FROM crop_images WHERE id = :id")
    suspend fun getById(id: String): CropImage?
}