package com.example.keyframeplayer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.util.UUID

@Dao
interface KeyFrameDao {
    // データの挿入(上書きスキップ、非同期処理)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertKeyFrame(keyFrame: KeyFrameEntity): Long

    // データの検索(keyFrameIDより)
    @Query("SELECT * FROM keyFrames WHERE id = :keyFrameID")
    suspend fun getKeyFrameById(keyFrameID: UUID): KeyFrameEntity?
}