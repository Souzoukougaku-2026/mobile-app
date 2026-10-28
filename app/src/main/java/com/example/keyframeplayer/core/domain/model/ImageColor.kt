package com.example.keyframeplayer.core.domain.model

import androidx.compose.ui.graphics.Color

enum class ImageColor(val id: Int, val displayName: String) {
    White(0, "白"),
    Black(1, "黒"),
    Gray(2, "グレー"),
    Red(3, "赤"),
    Orange(4, "オレンジ"),
    Yellow(5, "黄"),
    Green(6, "緑"),
    Cyan(7, "シアン"),
    Blue(8, "青"),
    Purple(9, "紫"),
    Pink(10, "ピンク"),
    Brown(11, "茶");

    val composeColor: Color
        get() = when (this) {
            White -> Color(0xFFE0E0E0) // 白が見えにくくならないよう少しグレーがかった白に
            Black -> Color(0xFF212121)
            Gray -> Color(0xFF8D8D8D)
            Red -> Color(0xFFE53935)
            Orange -> Color(0xFFFB8C00)
            Yellow -> Color(0xFFFBC02D)
            Green -> Color(0xFF4CAF50)
            Cyan -> Color(0xFF00ACC1)
            Blue -> Color(0xFF1E88E5)
            Purple -> Color(0xFF8E24AA)
            Pink -> Color(0xFFD81B60)
            Brown -> Color(0xFF6D4C41)
        }

    companion object {
        fun fromId(id: Int): ImageColor = values().find { it.id == id } ?: White
    }
}
