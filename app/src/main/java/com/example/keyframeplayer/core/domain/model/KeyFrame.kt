package com.example.keyframeplayer.core.domain.model

import java.util.UUID

data class KeyFrame(
    val id: UUID,
    val realTime: Long,
    val fileTime: Long,
    val keyFramePath: String,
    val moviePath: String
)
