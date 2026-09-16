package com.example.keyframeplayer.feature.movie.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TimeLabel(currentTime: Float, totalDuration: Float, modifier: Modifier = Modifier) {
    val h = (currentTime / 3600).toInt()
    val m = ((currentTime % 3600) / 60).toInt()
    val s = (currentTime % 60).toInt()
    Text(
        text = "現在時刻: %02d:%02d:%02d / %02dh".format(h, m, s, (totalDuration/3600).toInt()),
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier
    )
}
