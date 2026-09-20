package com.example.keyframeplayer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.keyframeplayer.data.AppDatabase
import com.example.keyframeplayer.model.Topic
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ListUpViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val clopImageDao = database.clopImageDao()

    val topics: StateFlow<List<Topic>> = clopImageDao.getAllTopicsFlow()
        .map { list ->
            list.map { item ->
                Topic(
                    class_name = item.clop.classname,
                    fileTime = item.fileTime,
                    imagePath = item.keyFramePath,
                    moviePath = item.moviePath,
                    imageColor = item.clop.color.ordinal,
                    realTime = item.realTime,
                    bboxLeft = item.clop.bboxPoint.ulX,
                    bboxTop = item.clop.bboxPoint.ulY,
                    bboxRight = item.clop.bboxPoint.lrX,
                    bboxBottom = item.clop.bboxPoint.lrY
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
