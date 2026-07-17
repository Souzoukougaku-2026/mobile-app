package com.example.a2026souzou.feature.movie.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a2026souzou.feature.movie.domain.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovieUiState())
    val uiState: StateFlow<MovieUiState> = _uiState.asStateFlow()

    init {
        fetchBarData()
        fetchDetailedBarData()
    }

    private fun fetchBarData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getBarValues(_uiState.value.overallBarCount).collect { result ->
                result.onSuccess { data ->
                    _uiState.update { it.copy(barValues = data, isLoading = false) }
                }.onFailure { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
            }
        }
    }

    private fun fetchDetailedBarData() {
        viewModelScope.launch {
            repository.getDetailedBarValues(_uiState.value.totalDetailedBarCount).collect { result ->
                result.onSuccess { data ->
                    _uiState.update { it.copy(detailedBarValues = data) }
                }
            }
        }
    }

    /**
     * 指定された時間に現在の再生位置を変更します。
     * 主にシークバー（スライダー）の操作時に呼び出されます。
     * @param newTime 変更後の時間（秒）
     */
    fun onTimeChanged(newTime: Float) {
        _uiState.update { it.copy(currentTime = newTime.coerceIn(0f, it.totalDurationSeconds)) }
    }

    /**
     * グラフ上のタップされた位置に基づいて、現在の再生位置を変更します。
     * グラフの端から端を 0.0（左端）〜 1.0（右端）として受け取ります。
     * @param progress タップされた位置の割合 (0.0 ~ 1.0)
     */
    fun onProgressTapped(progress: Float) {
        val newTime = progress * _uiState.value.totalDurationSeconds
        onTimeChanged(newTime)
    }

    /**
     * 詳細グラフで現在スクロールして表示されている範囲（ビューポート）を変更します。
     * この変更は全体グラフ上の「黄色いハイライト」の位置に反映されます。
     * @param startProgress 表示開始位置の割合 (0.0 ~ 1.0)
     */
    fun onVisibleRangeChanged(startProgress: Float) {
        _uiState.update { 
            it.copy(visibleRangeStart = startProgress.coerceIn(0f, 1f - it.visibleRangeWidth))
        }
    }
}
