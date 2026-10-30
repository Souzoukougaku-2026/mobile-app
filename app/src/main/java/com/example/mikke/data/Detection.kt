package com.example.mikke.data

data class Detection(
    val classname: String,
    val score: Float,
    val bbox: FloatArray
)
