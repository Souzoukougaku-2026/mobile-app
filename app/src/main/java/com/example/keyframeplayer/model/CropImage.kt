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
    Silver(1),
    Gray(2),
    Black(3),
    Red(4),
    Maroon(5),
    Yellow(6),
    Olive(7),
    Lime(8),
    Green(9),
    Aqua(10),
    Teal(11),
    Blue(12),
    Navy(13),
    Fuchsia(14),
    Purple(15);

    companion object {
        fun fromId(id: Int): ImageColor = entries.find { it.id == id } ?: White
    }
}
