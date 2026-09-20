package com.example.keyframeplayer.ui.movie.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.keyframeplayer.R
import java.io.File

@Composable
fun BottomInfoArea(currentTime: Float, thumbnailPath: String?, bbox: android.graphics.RectF?) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data(thumbnailPath?.let { File(it) } ?: R.drawable.ic_launcher_foreground)
                    .crossfade(true)
                    .build()
            )

            // BBox情報を Offset (0-100) に変換
            // dummyデータが 0-500 なので、一旦 500で割って100掛ける（暫定）
            val initialTopLeft = if (bbox != null) {
                Offset(bbox.left / 5f, 100f - (bbox.top / 5f))
            } else {
                Offset(50f, 80f)
            }
            val initialBottomRight = if (bbox != null) {
                Offset(bbox.right / 5f, 100f - (bbox.bottom / 5f))
            } else {
                Offset(80f, 50f)
            }

            ZoomableImage(
                painter = painter,
                initialTopLeft = initialTopLeft,
                initialBottomRight = initialBottomRight
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
