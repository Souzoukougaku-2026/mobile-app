package com.example.keyframeplayer.data.local


import androidx.room.*

@Dao
interface CropImageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CropImageEntity)

    @Query("SELECT * FROM crop_images ORDER BY timeStampReal DESC")
    suspend fun getAll(): List<CropImageEntity>

    @Query("DELETE FROM crop_images")
    suspend fun deleteAll()
}