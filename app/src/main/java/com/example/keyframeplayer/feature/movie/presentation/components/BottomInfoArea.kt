package com.example.keyframeplayer.feature.movie.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.keyframeplayer.R
import com.example.keyframeplayer.core.domain.model.CropImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BottomInfoArea(
    currentTime: Float,
    activeKeyframe: CropImage? = null,
    realTimeMs: Long = 0L
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier
                .weight(1.2f)
        ) {
            if (activeKeyframe != null) {
                ZoomableImage(
                    imagePath = activeKeyframe.keyFramePath,
                    isPixelCoordinates = true,
                    initialTopLeft = Offset(activeKeyframe.bboxLeft.toFloat(), activeKeyframe.bboxTop.toFloat()),
                    initialBottomRight = Offset(activeKeyframe.bboxRight.toFloat(), activeKeyframe.bboxBottom.toFloat())
                )
            } else {
                ZoomableImage(
                    painter = painterResource(id = R.drawable.ic_launcher_background),
                    initialTopLeft = Offset(50f, 80f),
                    initialBottomRight = Offset(80f, 50f)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .weight(0.8f)
        ) {
            val tableData = remember(currentTime, activeKeyframe, realTimeMs) {
                val realTimeStr = if (realTimeMs > 0L) {
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(realTimeMs))
                } else {
                    "%02d:%02d".format((currentTime/3600).toInt(), ((currentTime%3600)/60).toInt())
                }

                buildList {
                    add("Timestamp" to realTimeStr)
                    add("Class" to (activeKeyframe?.className ?: "-"))
                    add("Score" to (activeKeyframe?.score?.let { "%.2f".format(it) } ?: "-"))
                    add("Color" to (activeKeyframe?.color?.name ?: "-"))
                    add("Status" to "Normal")
                }
            }
            VerticalTable(
                data = tableData,
            )
        }
    }
}
