package com.example.keyframeplayer.core.domain.model

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

    companion object {
        fun fromId(id: Int): ImageColor = values().find { it.id == id } ?: White
    }
}
