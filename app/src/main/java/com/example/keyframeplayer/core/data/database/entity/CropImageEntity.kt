package com.example.keyframeplayer.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "CropImage",
    foreignKeys = [
        ForeignKey(
            entity = KeyFrameEntity::class,
            parentColumns = ["id"],
            childColumns = ["id_keyFrame"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["class_name", "color", "realTime"]),
        Index(value = ["color", "realTime"]),
        Index(value = ["realTime"])
    ]
)
data class CropImageEntity(
    @PrimaryKey
    val id: UUID,
    @ColumnInfo(name = "class_name")
    val className: String,
    val score: Double,
    val color: Int, // imageColor enumeration id
    @ColumnInfo(name = "bbox_left")
    val bboxLeft: Int,
    @ColumnInfo(name = "bbox_top")
    val bboxTop: Int,
    @ColumnInfo(name = "bbox_right")
    val bboxRight: Int,
    @ColumnInfo(name = "bbox_bottom")
    val bboxBottom: Int,
    val realTime: Long,
    val fileTime: Long,
    @ColumnInfo(name = "id_keyFrame")
    val idKeyFrame: UUID,
    val keyFramePath: String
)
