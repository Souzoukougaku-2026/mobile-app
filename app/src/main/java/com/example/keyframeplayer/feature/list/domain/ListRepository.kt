package com.example.keyframeplayer.feature.list.domain

import com.example.keyframeplayer.core.domain.model.CropImage
import com.example.keyframeplayer.core.domain.model.KeyFrame
import kotlinx.coroutines.flow.Flow

interface ListRepository {
    fun searchCropImages(
        className: String?,
        color: Int?,
        startTime: Long,
        endTime: Long,
        isAsc: Boolean
    ): Flow<List<CropImage>>

    /**
     * 指定された KeyFrame とそれに紐づく CropImage リストをデータベースに挿入します。
     */
    suspend fun insertCropData(keyFrame: KeyFrame, cropImages: List<CropImage>)
}
