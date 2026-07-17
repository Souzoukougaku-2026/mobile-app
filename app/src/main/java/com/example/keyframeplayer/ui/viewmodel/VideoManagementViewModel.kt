package com.example.keyframeplayer.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import com.example.keyframeplayer.util.VideoInfo
import com.example.keyframeplayer.util.VideoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VideoManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedUri = MutableStateFlow<Uri?>(null)
    val selectedUri: StateFlow<Uri?> = _selectedUri

    private val _isAccessible = MutableStateFlow(false)
    val isAccessible: StateFlow<Boolean> = _isAccessible

    private val _videoInfos = MutableStateFlow<List<VideoInfo>>(emptyList())
    val videoInfos: StateFlow<List<VideoInfo>> = _videoInfos

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading


    /**
     * フォルダが選択されたときにActivityなどから呼ばれる関数
     */
    fun onDirectoryPicked(uri: Uri?) {
        // 💡 対策1：今まさに裏で読み込み中（ロード中）なら、1秒タイマーからの重複命令をすべて無視して即終了
        if (_isLoading.value) {
            return
        }

        // 💡 対策2：ロード中でなくても、すでに同じフォルダを過去にスキャン完了しているなら処理をスキップ
        if (_selectedUri.value == uri && _isAccessible.value) {
            return
        }

        // 💡【修正】null ではない（フォルダの新規選択時）ときだけパスを更新する
        if (uri != null) {
            _selectedUri.value = uri
        }

        if (uri != null) {
            val context = getApplication<Application>().applicationContext

            try {
                // 権限チェック
                val hasAccess = VideoUtils.checkAccess(context, uri)
                _isAccessible.value = hasAccess

                if (hasAccess) {
                    // 💡 対策3：解析を始める直前に「ロード中」を true にする
                    // これにより画面のLinearProgressIndicatorが即座に回り出し、ボタンが連打できなくなります
                    _isLoading.value = true

                    // 💡 対策4：viewModelScope を使い、重い動画解析処理を裏スレッド（Dispatchers.IO）に丸投げする
                    // これによりメインスレッドが解放され、画面のフリーズ（カクつき）が100%消え去ります
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val result = VideoUtils.fetchVideoInfosFromDirectory(context, uri)
                            _videoInfos.value = result // バックグラウンドで解析が終わったらデータ格納
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            // 💡 対策5：成功してもエラーになっても、最後に必ずロード中を false に戻す
                            _isLoading.value = false
                        }
                    }
                } else {
                    _videoInfos.value = emptyList()
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isAccessible.value = false
                _videoInfos.value = emptyList()
                _isLoading.value = false
            }
        } else {
            _isAccessible.value = false
            _videoInfos.value = emptyList()
            _isLoading.value = false
        }
    }
}