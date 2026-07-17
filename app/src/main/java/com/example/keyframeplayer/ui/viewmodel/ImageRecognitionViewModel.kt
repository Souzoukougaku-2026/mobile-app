package com.example.keyframeplayer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.keyframeplayer.util.KeyFrameUtils.addMicrosecondsToLong
import com.example.keyframeplayer.util.KeyFrameUtils.extractKeyframes
import com.example.keyframeplayer.util.KeyFrameUtils.saveBitmapToInternalStorage
import com.example.keyframeplayer.data.AppDatabase
import com.example.keyframeplayer.data.ClopImageDao
import com.example.keyframeplayer.data.ClopImageEntity
import com.example.keyframeplayer.data.BaseColor
import com.example.keyframeplayer.data.BPoint
import com.example.keyframeplayer.data.KeyFrameEntity
import com.example.keyframeplayer.util.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageRecognitionViewModel(application: Application) : AndroidViewModel(application) {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()

    fun startProcessing(videoInfos: List<VideoInfo>) {
        viewModelScope.launch {
            _isLoading.value = true
            _progress.value = 0f

            val context = getApplication<Application>().applicationContext

            try {
                withContext(Dispatchers.IO) {
                    val database = AppDatabase.getDatabase(context)
                    val keyFrameDao = database.keyFrameDao()
                    // 1. 既存のデータベースからクロップ用のDaoを取得
                    val clopImageDao = database.clopImageDao()

                    val videoTotal = videoInfos.size
                    val insertedEntities = mutableListOf<KeyFrameEntity>()

                    // --- フェーズ 1: 動画の解析とDB保存 ---
                    for ((index, video) in videoInfos.withIndex()) {
                        ensureActive()
                        val keyFrames = extractKeyframes(context, video.uri, limit = 86400)

                        for (keyFrame in keyFrames) {
                            ensureActive()
                            val keyFramePath = saveBitmapToInternalStorage(context, keyFrame.bitmap) ?: continue

                            // UUIDはEntity生成時に自動で初期化されます（id = UUID.randomUUID()）
                            val newKeyFrame = KeyFrameEntity(
                                keyFramePath = keyFramePath,
                                moviePath = video.uri,
                                realTime = addMicrosecondsToLong(video.startTimeText, keyFrame.timeUs),
                                fileTime = keyFrame.timeUs
                            )
                            keyFrameDao.insertKeyFrame(newKeyFrame)

                            // メモリ上に保持するリストに追加（これでUUIDが確定した状態のEntityが残ります）
                            insertedEntities.add(newKeyFrame)
                        }

                        val phase1Progress = (index + 1).toFloat() / videoTotal
                        _progress.value = phase1Progress * 0.5f
                    }

                    // --- フェーズ 2: 保存したデータに対する別の処理 ---
                    val additionalTotal = insertedEntities.size

                    if (additionalTotal > 0) {
                        for ((index, entity) in insertedEntities.withIndex()) {
                            ensureActive()

                            // 2. ループ内でダミーデータ保存関数を呼び出す
                            saveCropImage(clopImageDao, entity)

                            val phase2Progress = (index + 1).toFloat() / additionalTotal
                            _progress.value = 0.5f + (phase2Progress * 0.5f)
                        }
                    } else {
                        _progress.value = 1.0f
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 画像認識処理のダミー関数
    private suspend fun saveCropImage(clopImageDao: ClopImageDao, entity: KeyFrameEntity) {
        val list = listOf("wallet", "headphone", "key", "smart phone", "umbrella")
        val dummyClopEntity = ClopImageEntity(
            classname = list.random(),
            score = 1f,
            color = BaseColor.entries.random (),
            bboxPoint = BPoint (0f, 0f, 500f, 500f),
            idKeyFrame = entity.id
        )

        // データベースに格納
        clopImageDao.insertClopImage(dummyClopEntity)
    }
}