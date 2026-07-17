package com.example.keyframeplayer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.keyframeplayer.util.KeyFrameUtils.addMicrosecondsToLong
import com.example.keyframeplayer.util.KeyFrameUtils.extractKeyframes
import com.example.keyframeplayer.util.KeyFrameUtils.saveBitmapToInternalStorage
import com.example.keyframeplayer.data.AppDatabase
import com.example.keyframeplayer.data.KeyFrameEntity
import com.example.keyframeplayer.util.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SaveKeyFrameViewModel(application: Application) : AndroidViewModel(application) {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()

    // 2. 引数から context を削除
    fun startProcessing(videoInfos: List<VideoInfo>) {
        viewModelScope.launch {
            _isLoading.value = true
            _progress.value = 0f

            // 3. getApplication() で安全な Context を取得する
            val context = getApplication<Application>().applicationContext

            withContext(Dispatchers.IO) {
                // データベースやファイル保存に、この安全な context を渡す
                val database = AppDatabase.getDatabase(context)
                val keyFrameDao = database.keyFrameDao()

                val total = videoInfos.size
                for ((index, video) in videoInfos.withIndex()) {
                    val keyFrames = extractKeyframes(context, video.uri, limit = 86400)

                    for (keyFrame in keyFrames) {
                        val keyFramePath = saveBitmapToInternalStorage(context, keyFrame.bitmap) ?: break
                        val newKeyFrame = KeyFrameEntity(
                            keyFramePath = keyFramePath,
                            moviePath = video.uri,
                            realTime = addMicrosecondsToLong(video.startTimeText, keyFrame.timeUs),
                            fileTime = keyFrame.timeUs
                        )
                        keyFrameDao.insertKeyFrame(newKeyFrame)
                    }
                    _progress.value = (index + 1).toFloat() / total
                }
            }
            _isLoading.value = false
        }
    }
}