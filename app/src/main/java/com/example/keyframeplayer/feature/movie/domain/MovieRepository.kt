package com.example.keyframeplayer.feature.movie.domain

import com.example.keyframeplayer.core.data.database.entity.VideoEntity
import com.example.keyframeplayer.core.domain.model.CropImage
import com.example.keyframeplayer.core.domain.model.KeyFrame
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface MovieRepository {
    fun getBarValues(count: Int): Flow<Result<List<Float>>>
    fun getDetailedBarValues(count: Int): Flow<Result<List<Float>>>

    suspend fun processVideo(uri: android.net.Uri): Result<Unit>
    fun getStoredKeyframes(): Flow<List<CropImage>>
    fun getVideosOrdered(): Flow<List<VideoEntity>>
    suspend fun getCropImageById(id: UUID): CropImage?
    suspend fun getKeyFrameById(id: UUID): KeyFrame?
    suspend fun ensureVideoEntitiesExist()
}
