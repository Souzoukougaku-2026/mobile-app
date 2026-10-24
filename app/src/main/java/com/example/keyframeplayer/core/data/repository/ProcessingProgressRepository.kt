package com.example.keyframeplayer.core.data.repository

import android.content.Context
import android.util.Log
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ProcessingProgressState(
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val statusText: String = "",
    val isKeyFrameExtractionFinished: Boolean = false,
    val errorMessage: String? = null
)

@Singleton
class ProcessingProgressRepository @Inject constructor(
    private val keyFrameDao: KeyFrameDao,
    private val cropImageDao: CropImageDao,
    private val videoDao: VideoDao,
    @ApplicationContext private val context: Context
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var processingJob: Job? = null

    private val _progressState = MutableStateFlow(ProcessingProgressState())
    val progressState: StateFlow<ProcessingProgressState> = _progressState.asStateFlow()

    fun startProcessing(videoInfos: List<VideoInfo>) {
        if (_progressState.value.isProcessing) return

        processingJob?.cancel()
        processingJob = repositoryScope.launch {
            _progressState.value = ProcessingProgressState(
                isProcessing = true,
                progress = 0f,
                statusText = "処理準備中...",
                isKeyFrameExtractionFinished = false,
                errorMessage = null
            )

            try {
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

                        val msg = "KeyFrame抽出: 動画 $videoNum/$videoTotal 本目\n$currentFrame / $totalFrames 枚"
                        _progressState.update {
                            it.copy(
                                progress = (0.2f * index.toFloat() / videoTotal.coerceAtLeast(1)) + (0.2f * currentFrame.toFloat() / totalFrames.coerceAtLeast(1) / videoTotal.coerceAtLeast(1)),
                                statusText = msg
                            )
                        }
                        Log.d("ProcessingProgressRepo", msg)
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

                    _progressState.update {
                        it.copy(
                            progress = 0.2f * videoNum.toFloat() / videoTotal.coerceAtLeast(1)
                        )
                    }
                }

                // キーフレーム抽出完了！リスト画面への遷移フラグをONにする
                _progressState.update {
                    it.copy(
                        progress = 0.2f,
                        statusText = "画像解析・物体検出準備中...",
                        isKeyFrameExtractionFinished = true
                    )
                }

                if (videoInfos.isEmpty()) {
                    _progressState.update {
                        it.copy(
                            isProcessing = false,
                            progress = 1f,
                            statusText = "完了",
                            isKeyFrameExtractionFinished = true
                        )
                    }
                    return@launch
                }

                // フェーズ 2: 物体検出 Worker (MLWorker) の起動と進捗監視
                Log.i("ProcessingProgressRepo", "[画像解析・物体検出] MLWorker を開始します")

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

                            val overallProgress = 0.2f + workerProgress * 0.8f
                            val msg = if (total > 0) {
                                "画像解析・物体検出\n$current / $total 枚"
                            } else {
                                "画像解析・物体検出中..."
                            }

                            _progressState.update {
                                it.copy(
                                    progress = overallProgress,
                                    statusText = msg
                                )
                            }
                            Log.d("ProcessingProgressRepo", "$msg (${(overallProgress * 100).toInt()}%)")
                        }

                        workInfo?.state?.isFinished == true
                    } ?: error("WorkInfoを取得できませんでした")

                when (finishedWorkInfo.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        _progressState.update {
                            it.copy(
                                isProcessing = false,
                                progress = 1f,
                                statusText = "処理完了"
                            )
                        }
                        Log.i("ProcessingProgressRepo", "すべての処理が正常に完了しました")
                    }

                    WorkInfo.State.FAILED -> {
                        _progressState.update {
                            it.copy(
                                isProcessing = false,
                                errorMessage = "画像認識に失敗しました。"
                            )
                        }
                        Log.e("ProcessingProgressRepo", "画像認識処理が失敗しました")
                    }

                    WorkInfo.State.CANCELLED -> {
                        _progressState.update {
                            it.copy(
                                isProcessing = false,
                                errorMessage = "画像認識がキャンセルされました。"
                            )
                        }
                        Log.w("ProcessingProgressRepo", "画像認識処理がキャンセルされました")
                    }

                    else -> Unit
                }

            } catch (e: CancellationException) {
                _progressState.update { it.copy(isProcessing = false) }
                throw e
            } catch (e: Exception) {
                _progressState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = e.message ?: "処理中にエラーが発生しました。"
                    )
                }
                Log.e("ProcessingProgressRepo", "エラーが発生しました", e)
            }
        }
    }
}
