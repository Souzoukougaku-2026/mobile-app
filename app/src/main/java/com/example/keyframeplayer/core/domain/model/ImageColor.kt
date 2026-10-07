package com.example.keyframeplayer.core.domain.model

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
        fun fromId(id: Int): ImageColor = values().find { it.id == id } ?: White
    }
}
