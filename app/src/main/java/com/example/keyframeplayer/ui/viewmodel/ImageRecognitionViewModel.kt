package com.example.keyframeplayer.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.keyframeplayer.core.data.database.dao.CropImageDao
import com.example.keyframeplayer.core.data.database.dao.KeyFrameDao
import com.example.keyframeplayer.core.data.database.dao.VideoDao
import com.example.keyframeplayer.core.data.database.entity.KeyFrameEntity
import com.example.keyframeplayer.core.data.database.entity.VideoEntity
import com.example.keyframeplayer.util.KeyFrameUtils.addMicrosecondsToLong
import com.example.keyframeplayer.util.KeyFrameUtils.forEachKeyframe
import com.example.keyframeplayer.util.KeyFrameUtils.saveBitmapToInternalStorage
import com.example.keyframeplayer.util.VideoInfo
import com.example.keyframeplayer.worker.MLWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ImageRecognitionViewModel @Inject constructor(
    private val keyFrameDao: KeyFrameDao,
    private val cropImageDao: CropImageDao,
    private val videoDao: VideoDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()

    private val _statusText = MutableStateFlow("")
    val statusText = _statusText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun startProcessing(videoInfos: List<VideoInfo>) {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _progress.value = 0f
            _statusText.value = "処理準備中..."
            _errorMessage.value = null

            try {
                withContext(Dispatchers.IO) {
                    // 古いDBデータおよび画像を全クリアしてリフレッシュ
                    cropImageDao.clearCropImages()
                    keyFrameDao.clearKeyFrames()
                    videoDao.clearVideos()

                    val imageDir = File(context.filesDir, "images")
                    if (imageDir.exists() && imageDir.isDirectory) {
                        imageDir.listFiles()?.forEach { file ->
                            if (file.isFile) {
                                file.delete()
                            }
                        }
                    }

                    val dateFormatWithMs = SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS", Locale.getDefault())
                    val dateFormatSec = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())

                    fun parseDate(text: String): Long {
                        return try {
                            dateFormatWithMs.parse(text)?.time ?: dateFormatSec.parse(text)?.time ?: 0L
                        } catch (e: Exception) {
                            try {
                                dateFormatSec.parse(text)?.time ?: 0L
                            } catch (ex: Exception) {
                                0L
                            }
                        }
                    }

                    // フェーズ 0: 動画情報を時系列順にDB保存
                    val videoEntities = videoInfos.map { video ->
                        val startTimeMs = parseDate(video.startTimeText)
                        val endTimeMs = parseDate(video.endTimeText)
                        VideoEntity(
                            uri = video.uri.toString(),
                            name = video.name,
                            realStartTime = startTimeMs,
                            realEndTime = endTimeMs,
                            durationMs = (endTimeMs - startTimeMs).coerceAtLeast(0L)
                        )
                    }.sortedBy { it.realStartTime }

                    videoDao.insertVideos(videoEntities)

                    val videoTotal = videoInfos.size

                    // フェーズ 1: キーフレーム抽出とキーフレームDB保存
                    for ((index, video) in videoInfos.withIndex()) {
                        ensureActive()
                        val videoNum = index + 1

                        val savedFrames = mutableListOf<Pair<Long, String>>()

                        forEachKeyframe(
                            context = context,
                            uri = video.uri,
                            limit = 86400
                        ) { currentFrame, totalFrames, timeUs, bitmap ->
                            ensureActive()

                            val path = saveBitmapToInternalStorage(
                                context,
                                bitmap
                            )

                            if (path != null) {
                                savedFrames.add(timeUs to path)
                            }

                            val msg = "KeyFrame抽出: 動画 $videoNum/$videoTotal 本目 ($currentFrame/$totalFrames 枚)"
                            _statusText.value = msg
                            Log.d("ImageRecognitionVM", msg)
                        }

                        val keyFrameEntities = mutableListOf<KeyFrameEntity>()
                        for ((timeUs, path) in savedFrames) {
                            ensureActive()

                            val entity = KeyFrameEntity(
                                id = UUID.randomUUID(),
                                keyFramePath = path,
                                moviePath = video.uri.toString(),
                                realTime = addMicrosecondsToLong(
                                    video.startTimeText,
                                    timeUs
                                ),
                                fileTime = timeUs
                            )
                            keyFrameEntities.add(entity)
                        }

                        if (keyFrameEntities.isNotEmpty()) {
                            keyFrameDao.insertKeyFrames(keyFrameEntities)
                        }

                        _progress.value = 0.2f * videoNum.toFloat() / videoTotal
                    }

                    if (videoInfos.isEmpty()) {
                        _progress.value = 1f
                        _statusText.value = "完了"
                        return@withContext
                    }

                    // フェーズ 2: 物体検出 Worker (MLWorker) の起動と進捗監視
                    _statusText.value = "画像解析・物体検出準備中..."
                    Log.i("ImageRecognitionVM", "[画像解析・物体検出] MLWorker を開始します")

                    val workManager = WorkManager.getInstance(context)
                    val request = OneTimeWorkRequestBuilder<MLWorker>()
                        .build()

                    workManager.enqueue(request)

                    val finishedWorkInfo = workManager
                        .getWorkInfoByIdFlow(request.id)
                        .first { workInfo ->
                            if (workInfo != null) {
                                val current = workInfo.progress.getInt("current", 0)
                                val total = workInfo.progress.getInt("total", 0)
                                val workerProgress = workInfo.progress
                                    .getInt("progress", 0)
                                    .coerceIn(0, 100) / 100f

                                _progress.value = 0.2f + workerProgress * 0.8f

                                if (total > 0) {
                                    val msg = "画像解析・物体検出: $current/$total 枚"
                                    _statusText.value = msg
                                    Log.d("ImageRecognitionVM", "$msg (${(workerProgress * 100).toInt()}%)")
                                } else {
                                    _statusText.value = "画像解析・物体検出中..."
                                }
                            }

                            workInfo?.state?.isFinished == true
                        } ?: error("WorkInfoを取得できませんでした")

                    when (finishedWorkInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            _progress.value = 1f
                            _statusText.value = "処理完了"
                            Log.i("ImageRecognitionVM", "すべての処理が正常に完了しました")
                        }

                        WorkInfo.State.FAILED -> {
                            _errorMessage.value = "画像認識に失敗しました。"
                            Log.e("ImageRecognitionVM", "画像認識処理が失敗しました")
                        }

                        WorkInfo.State.CANCELLED -> {
                            _errorMessage.value = "画像認識がキャンセルされました。"
                            Log.w("ImageRecognitionVM", "画像認識処理がキャンセルされました")
                        }

                        else -> Unit
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "処理中にエラーが発生しました。"
                Log.e("ImageRecognitionVM", "エラーが発生しました", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
