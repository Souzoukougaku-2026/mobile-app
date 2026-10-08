package com.example.keyframeplayer.feature.movie.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimeLabel(
    currentTime: Float,
    totalDuration: Float,
    modifier: Modifier = Modifier,
    realTimeMs: Long = 0L
) {
    val h = (currentTime / 3600).toInt()
    val m = ((currentTime % 3600) / 60).toInt()
    val s = (currentTime % 60).toInt()

    val totalH = (totalDuration / 3600).toInt()
    val totalM = ((totalDuration % 3600) / 60).toInt()

    val realTimeFormatted = if (realTimeMs > 0L) {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        sdf.format(Date(realTimeMs))
    } else {
        null
    }

    Column(modifier = modifier) {
        if (realTimeFormatted != null) {
            Text(
                text = "現実日時: $realTimeFormatted",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = "再生時間: %02d:%02d:%02d / %02d:%02d".format(h, m, s, totalH, totalM),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
