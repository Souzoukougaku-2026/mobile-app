package com.example.keyframeplayer.model

import java.util.UUID

data class CropImage(
    val id: UUID,
    val className: String,
    val score: Double,
    val color: ImageColor,
    val bboxLeft: Int,
    val bboxTop: Int,
    val bboxRight: Int,
    val bboxBottom: Int,
    val realTime: Long,
    val idKeyFrame: UUID
) {
    companion object {
        const val CLASS_WALLET = "wallet"
        const val CLASS_HEADPHONE = "headphone"
        const val CLASS_KEY = "key"
        const val CLASS_SMART_PHONE = "smart phone"
        const val CLASS_UMBRELLA = "umbrella"
        
        val ALL_CLASSES = listOf(
            CLASS_WALLET,
            CLASS_HEADPHONE,
            CLASS_KEY,
            CLASS_SMART_PHONE,
            CLASS_UMBRELLA
        )
    }
}

enum class ImageColor(val id: Int) {
    White(0),
    Black(1),
    Gray(2),
    Red(3),
    Orange(4),
    Yellow(5),
    Green(6),
    Cyan(7),
    Blue(8),
    Purple(9),
    Pink(10),
    Brown(11);

    companion object {
        fun fromId(id: Int): ImageColor = entries.find { it.id == id } ?: White
    }
}
