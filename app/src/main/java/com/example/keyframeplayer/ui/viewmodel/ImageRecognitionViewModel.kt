package com.example.keyframeplayer.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.keyframeplayer.core.data.repository.ProcessingProgressRepository
import com.example.keyframeplayer.util.VideoInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class ImageRecognitionViewModel @Inject constructor(
    private val progressRepository: ProcessingProgressRepository
) : ViewModel() {

    val progressState = progressRepository.progressState

    val isLoading = progressState.map { it.isProcessing }
    val progress = progressState.map { it.progress }
    val statusText = progressState.map { it.statusText }
    val errorMessage = progressState.map { it.errorMessage }
    val isKeyFrameExtractionFinished = progressState.map { it.isKeyFrameExtractionFinished }

    suspend fun hasPersistedData(): Boolean {
        return progressRepository.hasPersistedData()
    }

    fun startProcessing(videoInfos: List<VideoInfo>) {
        progressRepository.startProcessing(videoInfos)
    }
}
