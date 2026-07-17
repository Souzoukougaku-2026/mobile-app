package com.example.a2026souzou.feature.list.data

import com.example.a2026souzou.core.data.database.dao.CropImageDao
import com.example.a2026souzou.core.data.database.dao.KeyFrameDao
import com.example.a2026souzou.core.data.database.entity.CropImageEntity
import com.example.a2026souzou.core.data.database.entity.KeyFrameEntity
import com.example.a2026souzou.core.domain.model.CropImage
import com.example.a2026souzou.core.domain.model.ImageColor
import com.example.a2026souzou.core.domain.model.KeyFrame
import com.example.a2026souzou.feature.list.domain.ListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class ListRepositoryImpl @Inject constructor(
    private val cropImageDao: CropImageDao,
    private val keyFrameDao: KeyFrameDao
) : ListRepository {

    override fun searchCropImages(
        className: String?,
        color: Int?,
        startTime: Long,
        endTime: Long,
        isAsc: Boolean
    ): Flow<List<CropImage>> {
        return cropImageDao.searchCropImages(
            className = className,
            color = color,
            startTime = startTime,
            endTime = endTime,
            isAsc = isAsc
        ).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertCropData(keyFrame: KeyFrame, cropImages: List<CropImage>) {
        // Domain -> Entity 変換
        val keyFrameEntity = keyFrame.toEntity()
        val cropImageEntities = cropImages.map { it.toEntity() }

        // DB 挿入 (KeyFrame が親なので先に挿入)
        keyFrameDao.insertKeyFrames(listOf(keyFrameEntity))
        cropImageDao.insertCropImages(cropImageEntities)
    }

    private fun CropImageEntity.toDomain(): CropImage {
        return CropImage(
            id = id,
            className = className,
            score = score,
            color = ImageColor.fromId(color),
            bboxLeft = bboxLeft,
            bboxTop = bboxTop,
            bboxRight = bboxRight,
            bboxBottom = bboxBottom,
            realTime = realTime,
            idKeyFrame = idKeyFrame
        )
    }

    private fun CropImage.toEntity(): CropImageEntity {
        return CropImageEntity(
            id = id,
            className = className,
            score = score,
            color = color.id,
            bboxLeft = bboxLeft,
            bboxTop = bboxTop,
            bboxRight = bboxRight,
            bboxBottom = bboxBottom,
            realTime = realTime,
            idKeyFrame = idKeyFrame
        )
    }

    private fun KeyFrame.toEntity(): KeyFrameEntity {
        return KeyFrameEntity(
            id = id,
            realTime = realTime,
            fileTime = fileTime,
            keyFramePath = keyFramePath,
            moviePath = moviePath
        )
    }
}
