package com.example.keyframeplayer.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.keyframeplayer.KeyframeItem

class SharedViewModel : ViewModel() {

    private val _selectedUri = MutableStateFlow<Uri?>(null)
    val selectedUri: StateFlow<Uri?> = _selectedUri

    private val _keyframes = MutableStateFlow<List<KeyframeItem>>(emptyList())
    val keyframes: StateFlow<List<KeyframeItem>> = _keyframes

    fun setUri(uri: Uri) {
        _selectedUri.value = uri
    }

    fun setKeyframes(list: List<KeyframeItem>) {
        _keyframes.value = list
    }
}