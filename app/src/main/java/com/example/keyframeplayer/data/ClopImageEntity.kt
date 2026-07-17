package com.example.keyframeplayer.data

import  androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

enum class ImageColor {
    red,
    blue,
    green,
    yellow,
}

data class BPoint(
    val ulX: Float,
    val ulY: Float,
    val lrX: Float,
    val lrY: Float
)

@Entity(
    tableName = "clopimages",
    foreignKeys = [
        ForeignKey(
            entity = KeyFrameEntity::class,
            parentColumns = ["id"],
            childColumns = ["idKeyFrame"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ClopImageEntity(
    @PrimaryKey
    val uuid: UUID = UUID.randomUUID(),

    val classname: String,
    val score: Float,
    val color: ImageColor,
    val bboxPoint: BPoint,

    @ColumnInfo(index = true)
    val idKeyFrame: UUID,
)