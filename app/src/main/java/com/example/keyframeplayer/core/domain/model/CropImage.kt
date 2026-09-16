package com.example.keyframeplayer.core.domain.model

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
    val fileTime: Int,
    val keyFramePath: String,
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
