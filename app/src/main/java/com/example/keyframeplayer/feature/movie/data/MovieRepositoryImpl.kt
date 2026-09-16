package com.example.keyframeplayer.feature.movie.data

import android.net.Uri
import com.example.keyframeplayer.core.data.database.dao.CropImageDao
import com.example.keyframeplayer.core.data.database.dao.KeyFrameDao
import com.example.keyframeplayer.core.domain.model.CropImage
import com.example.keyframeplayer.core.domain.model.ImageColor
import com.example.keyframeplayer.feature.movie.domain.MovieRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.random.Random

class MovieRepositoryImpl @Inject constructor(
    private val cropImageDao: CropImageDao,
    private val keyFrameDao: KeyFrameDao,
    private val videoFrameExtractor: VideoFrameExtractor
) : MovieRepository {
    override fun getBarValues(count: Int): Flow<Result<List<Float>>> = flow {
        try {
            val mockData = List(count) { Random.nextFloat() }
            emit(Result.success(mockData))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun getDetailedBarValues(count: Int): Flow<Result<List<Float>>> = flow {
        try {
            val mockData = List(count) { Random.nextFloat() }
            emit(Result.success(mockData))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    //  動画を処理してDBへ保存
    override suspend fun processVideo(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val times = videoFrameExtractor.getKeyframeTimes(uri)
            val frames = videoFrameExtractor.extractFrames(uri, times)

            frames.forEach { (timeUs, bitmap) ->
                val (keyFrame, cropImage) = videoFrameExtractor.saveFrameAsEntities(bitmap, timeUs, uri)
                keyFrameDao.insertKeyFrames(listOf(keyFrame))
                cropImageDao.insertCropImages(listOf(cropImage))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    //  DBから保存されたデータを取得
    override fun getStoredKeyframes(): Flow<List<CropImage>> {
        return cropImageDao.searchCropImages(null, null, 0, Long.MAX_VALUE, true, 100)
            .map { entities ->
                entities.map { entity ->
                    CropImage(
                        id = entity.id,
                        className = entity.className,
                        score = entity.score,
                        color = ImageColor.fromId(entity.color),
                        bboxLeft = entity.bboxLeft,
                        bboxTop = entity.bboxTop,
                        bboxRight = entity.bboxRight,
                        bboxBottom = entity.bboxBottom,
                        realTime = entity.realTime,
                        fileTime = entity.fileTime.toInt(),
                        keyFramePath = entity.keyFramePath,
                        idKeyFrame = entity.idKeyFrame
                    )
                }
            }
    }
}