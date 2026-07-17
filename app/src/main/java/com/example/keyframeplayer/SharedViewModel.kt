package com.example.keyframeplayer

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.keyframeplayer.data.AppDatabase
import com.example.keyframeplayer.data.CropImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class SharedViewModel : ViewModel() {
    private val _selectedUri = MutableStateFlow<Uri?>(null)
    val selectedUri: StateFlow<Uri?> = _selectedUri

    private val _keyframeItems = MutableStateFlow<List<KeyframeItem>>(emptyList())
    val keyframeItems: StateFlow<List<KeyframeItem>> = _keyframeItems

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun getCropImages(context: Context): Flow<List<CropImage>> {
        return AppDatabase.getInstance(context).cropImageDao().getAllFlow()
    }

    suspend fun saveToRoom(context: Context, items: List<CropImage>) {
        val dao = AppDatabase.getInstance(context).cropImageDao()
        dao.deleteAll()
        dao.insertAll(items)
    }

    fun selectVideo(uri: Uri) {
        _selectedUri.value = uri
        _keyframeItems.value = emptyList()
    }

    fun setKeyframeItems(items: List<KeyframeItem>) {
        _keyframeItems.value = items
    }

    fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }

    fun loadFromFolder(
        context: Context,
        treeUri: Uri,
        generateThumbnail: (Context, String) -> Bitmap
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true

            val root = DocumentFile.fromTreeUri(context, treeUri)
            val videoFiles = root?.listFiles()
                ?.filter { it.isFile && it.type?.startsWith("video/") == true}
                ?.sortedBy { it.name } ?: emptyList()

            val items = videoFiles.map { file ->
                KeyframeItem(
                    timeUs = 0,
                    bitmap = generateThumbnail(context, file.uri.toString()),
                    uri = file.uri
                )
            }

            _keyframeItems.value = items
            _isLoading.value = false
        }
    }
}
