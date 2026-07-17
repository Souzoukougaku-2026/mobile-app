package com.example.a2026souzou.feature.movie.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.a2026souzou.R

@Composable
fun BottomInfoArea(currentTime: Float) {
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
            ZoomableImage(
                painter = painterResource(id = R.drawable.test_image),
                initialTopLeft = Offset(50f, 80f),
                initialBottomRight = Offset(80f, 50f)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .weight(0.8f)
        ) {
            VerticalTable(
                data = listOf(
                    "Timestamp" to "%02d:%02d".format((currentTime/3600).toInt(), ((currentTime%3600)/60).toInt()),
                    "Velocity" to "12.5 m/s",
                    "Altitude" to "450 m",
                    "Status" to "Normal",
                ),
            )
        }
    }
}
