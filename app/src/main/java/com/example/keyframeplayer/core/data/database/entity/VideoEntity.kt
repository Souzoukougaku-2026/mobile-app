package com.example.keyframeplayer.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * フォルダ内の動画ファイルを時系列順に管理するための Room エンティティ
 */
@Entity(tableName = "Video")
data class VideoEntity(
    @PrimaryKey
    val uri: String,
    val name: String,
    val realStartTime: Long, // Epoch ms (現実開始時刻)
    val realEndTime: Long,   // Epoch ms (現実終了時刻)
    val durationMs: Long     // 動画の再生時間 (ミリ秒)
)
