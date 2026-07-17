package com.example.keyframeplayer

import android.content.Context
import android.content.Intent
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.keyframeplayer.data.CropImage
import com.example.keyframeplayer.ui.theme.KeyframePlayerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class MainActivity : ComponentActivity() {
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KeyframePlayerTheme {
                val navController = rememberNavController()
                val sharedViewModel: SharedViewModel = viewModel()

                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        MainScreen(navController, sharedViewModel)
                    }
                    composable("player/{timeUs}") { backStackEntry ->
                        val timeUs = backStackEntry.arguments?.getString("timeUs")?.toLong() ?: 0L

                        // ViewModelから必要なデータを取り出す
                        val uri by sharedViewModel.selectedUri.collectAsState()
                        val keyframes by sharedViewModel.getCropImages(LocalContext.current).collectAsState(initial = emptyList())

                        if (uri != null && keyframes.isNotEmpty()) {
                            PlayerScreen(
                                uri = uri!!,
                                keyframes = keyframes,
                                timeUs = timeUs
                        )
                    }
                }
            }
        }
    }
}

fun getKeyframeTimes(context: Context, uri: Uri): List<Long> {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        val intervalUs = 60 * 1000_000L // 1分

        val times = mutableListOf<Long>()
        var currentUs = 0L
        while (currentUs < durationMs * 1000) {
            times.add(currentUs)
            currentUs += intervalUs
        }
        times
    } finally {
        retriever.release()
    }
}

fun getKeyframeItems(
    context: Context,
    uri: Uri,
    timesUs: List<Long>,
    limit: Int = 100
): List<KeyframeItem> {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(context, uri)
    val items = mutableListOf<KeyframeItem>()
    for (timeUs in timesUs.take(limit)) {
        val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 512, 512)
        } else {
            retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }

        bitmap?.let { items.add(KeyframeItem(timeUs, it)) }
    }
    retriever.release()
    return items
}

private fun saveBitmapAndCreateEntity(context: Context, bitmap: android.graphics.Bitmap, timeUs: Long, videoUri: Uri): CropImage {
    val filename = "frame_${timeUs}.jpg"
    context.openFileOutput(filename, Context.MODE_PRIVATE).use {
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it)
    }
    val path = context.getFileStreamPath(filename).absolutePath

    return CropImage(
        id = UUID.randomUUID().toString(),
        cropImagePath = path,
        className = "Keyframe",
        score = 1.0f,
        color = "#FFFFFF",
        timestampRealTime = System.currentTimeMillis(),
        timestampFileTime = timeUs / 1000, // ms単位
        keyFrame = (timeUs / 1000000).toInt(),
        movieAddress = videoUri.toString()
    )
}

@Composable
fun MainScreen(
    navController: NavController,
    sharedViewModel: SharedViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val selectedUri by sharedViewModel.selectedUri.collectAsState()
    val keyframeItems by sharedViewModel.keyframeItems.collectAsState()
    val isLoading by sharedViewModel.isLoading.collectAsState()

    LaunchedEffect(selectedUri) {
        val uri = selectedUri ?: return@LaunchedEffect
        if (keyframeItems.isNotEmpty()) return@LaunchedEffect

        sharedViewModel.setLoading(true)
        val items = withContext(Dispatchers.IO) {
            val times = getKeyframeTimes(context, uri)
            val kfItems = getKeyframeItems(context, uri, times)
            val cropImages = kfItems.map { item ->
                saveBitmapAndCreateEntity(context, item.bitmap, item.timeUs, uri)
            }
            sharedViewModel.saveToRoom(context, cropImages)
            kfItems
        }
        sharedViewModel.setKeyframeItems(items)
        sharedViewModel.setLoading(false)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            sharedViewModel.selectVideo(it)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = { launcher.launch(arrayOf("video/*")) }) {
            Text("動画を選択")
        }

        selectedUri?.let { Text("選択された動画:\n$it") }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize()
            ) {
                items(keyframeItems) { item ->
                    Image(
                        bitmap = item.bitmap.asImageBitmap(),
                        contentDescription = "Keyframe",
                        modifier = Modifier
                            .padding(4.dp)
                            .size(120.dp)
                            .clickable {
                                navController.navigate("player/${item.timeUs}")
                            }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KeyframePlayerTheme {
        MainScreen(
            navController = rememberNavController(),
            sharedViewModel = SharedViewModel()
        )
    }
}
}
