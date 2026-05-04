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
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.keyframeplayer.ui.theme.KeyframePlayerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
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
                        val timeUs = backStackEntry.arguments
                            ?.getString("timeUs")?.toLong() ?: 0L

                        PlayerScreen(
                            viewModel = sharedViewModel,
                            timeUs = timeUs
                        )
                    }
                }
            }
        }
    }
}

fun getKeyframeTimes(context: Context, uri: Uri): List<Long> {
    val extractor = MediaExtractor()
    extractor.setDataSource(context, uri, null)

    var videoTrackIndex = -1
    for (i in 0 until extractor.trackCount) {
        val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
        if (mime?.startsWith("video/") == true) {
            videoTrackIndex = i
            break
        }
    }
    if (videoTrackIndex == -1) return emptyList()

    extractor.selectTrack(videoTrackIndex)
    val times = mutableListOf<Long>()
    while (true) {
        val timeUs = extractor.sampleTime
        if (timeUs < 0) break
        if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
            times.add(timeUs)
        }
        extractor.advance()
    }
    extractor.release()
    return times
}

fun getKeyframeItems(
    context: Context,
    uri: Uri,
    timesUs: List<Long>,
    limit: Int = 10
): List<KeyframeItem> {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(context, uri)
    val items = mutableListOf<KeyframeItem>()
    for (timeUs in timesUs.take(limit)) {
        retriever.getFrameAtTime(
            timeUs,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        )?.let { items.add(KeyframeItem(timeUs, it)) }
    }
    retriever.release()
    return items
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
            getKeyframeItems(context, uri, times)
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
