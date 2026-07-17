package com.example.keyframeplayer.data

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "keyframes")
data class KeyFrameEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),

    val keyFramePath: String,
    val moviePath: Uri,
    val realTime: Long,
    val fileTime: Long
)