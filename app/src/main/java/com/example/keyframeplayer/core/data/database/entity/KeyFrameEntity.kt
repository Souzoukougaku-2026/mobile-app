package com.example.keyframeplayer.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "KeyFrame")
data class KeyFrameEntity(
    @PrimaryKey
    val id: UUID,
    val realTime: Long,
    val fileTime: Long,
    val keyFramePath: String,
    val moviePath: String
)
