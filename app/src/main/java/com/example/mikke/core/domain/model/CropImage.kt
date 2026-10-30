package com.example.mikke.core.domain.model

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
    val idKeyFrame: UUID,
    val isLiked: Boolean = false
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

fun String.toJapaneseClassName(): String {
    return when (this.lowercase().trim()) {
        "wallet" -> "財布"
        "headphone", "headphones" -> "ヘッドホン"
        "key", "keys" -> "鍵"
        "smart phone", "smartphone", "phone" -> "スマホ"
        "umbrella" -> "傘"
        "person" -> "人物"
        "car" -> "車"
        "bag", "handbag", "backpack" -> "バッグ"
        "bottle" -> "ボトル"
        "chair" -> "椅子"
        "laptop" -> "パソコン"
        "book" -> "本"
        "photography" -> "写真"
        "keyframe" -> "キーフレーム"
        else -> this.ifEmpty { "オブジェクト" }
    }
}

val CropImage.japaneseClassName: String
    get() = className.toJapaneseClassName()
