package com.example.keyframeplayer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.keyframeplayer.data.AppDatabase
import com.example.keyframeplayer.data.KeyFrameEntity
import com.example.keyframeplayer.util.KeyFrameUtils.addMicrosecondsToLong
import com.example.keyframeplayer.util.KeyFrameUtils.forEachKeyframe
import com.example.keyframeplayer.util.KeyFrameUtils.saveBitmapToInternalStorage
import com.example.keyframeplayer.util.VideoInfo
import com.example.wasuremono_prj.joint.MLWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageRecognitionViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // 0.0f〜1.0f
    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun startProcessing(videoInfos: List<VideoInfo>) {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _progress.value = 0f
            _errorMessage.value = null

            val context = getApplication<Application>().applicationContext

            try {
                withContext(Dispatchers.IO) {
                    val keyFrameDao =
                        AppDatabase.getDatabase(context).keyFrameDao()
                    val videoTotal = videoInfos.size

                    // フェーズ1: キーフレーム取得・保存・DB登録
                    for ((index, video) in videoInfos.withIndex()) {
                        ensureActive()

                        val savedFrames = mutableListOf<Pair<Long, String>>()

                        forEachKeyframe(
                            context = context,
                            uri = video.uri,
                            limit = 86400
                        ) { timeUs, bitmap ->
                            ensureActive()

                            val path = saveBitmapToInternalStorage(
                                context,
                                bitmap
                            )

                            if (path != null) {
                                savedFrames.add(timeUs to path)
                            }
                        }

                        for ((timeUs, path) in savedFrames) {
                            ensureActive()

                            val entity = KeyFrameEntity(
                                keyFramePath = path,
                                moviePath = video.uri,
                                realTime = addMicrosecondsToLong(
                                    video.startTimeText,
                                    timeUs
                                ),
                                fileTime = timeUs
                            )

                            keyFrameDao.insertKeyFrame(entity)
                        }

                        // フェーズ1は全体の20%
                        _progress.value =
                            0.2f * (index + 1).toFloat() / videoTotal
                    }

                    // 入力動画が空ならWorkerを起動する必要はない
                    if (videoInfos.isEmpty()) {
                        _progress.value = 1f
                        return@withContext
                    }

                    // フェーズ2: 物体検出Worker
                    val workManager = WorkManager.getInstance(context)
                    val request = OneTimeWorkRequestBuilder<MLWorker>()
                        .build()

                    workManager.enqueue(request)

                    // Workが成功・失敗・キャンセルのいずれかになるまで監視
                    val finishedWorkInfo = workManager
                        .getWorkInfoByIdFlow(request.id)
                        .first { workInfo ->
                            if (workInfo != null) {
                                val workerProgress =
                                    workInfo.progress
                                        .getInt("progress", 0)
                                        .coerceIn(0, 100) / 100f

                                // フェーズ2は全体の80%
                                _progress.value =
                                    0.2f + workerProgress * 0.8f
                            }

                            workInfo?.state?.isFinished == true
                        } ?: error("WorkInfoを取得できませんでした")

                    when (finishedWorkInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            _progress.value = 1f
                        }

                        WorkInfo.State.FAILED -> {
                            _errorMessage.value = "画像認識に失敗しました。"
                        }

                        WorkInfo.State.CANCELLED -> {
                            _errorMessage.value = "画像認識がキャンセルされました。"
                        }

                        else -> Unit
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value =
                    e.message ?: "処理中にエラーが発生しました。"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
