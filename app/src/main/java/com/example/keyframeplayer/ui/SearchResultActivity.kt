package com.example.keyframeplayer.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.keyframeplayer.data.AppDatabase
import com.example.keyframeplayer.data.CropImage
import com.example.keyframeplayer.data.CropImageTimestamp
import com.example.keyframeplayer.ui.theme.KeyframePlayerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchResultActivity : ComponentActivity() {

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = AppDatabase.getInstance(this)

        setContent {
            KeyframePlayerTheme {
                SearchResultScreen(
                    loadItems = {
                        db.cropImageDao().getAll()
                    },
                    loadTimestamp = { cropImageId ->
                        db.cropImageDao().getTimestampById(cropImageId)
                    },
                    onTimestampLoaded = ::handleTimestamp
                )
            }
        }
    }

    private fun handleTimestamp(result: CropImageTimestamp) {
        Log.d(
            "TIMESTAMP",
            """
            id: ${result.id}
            className: ${result.className}
            timestampRealTime: ${result.timestampRealTime} ms
            timestampFileTime: ${result.timestampFileTime} ms
            movieAddress: ${result.movieAddress}
            """.trimIndent()
        )

        // Next step: open result.movieAddress and seek to result.timestampFileTime.
    }
}

@Composable
private fun SearchResultScreen(
    loadItems: suspend () -> List<CropImage>,
    loadTimestamp: suspend (cropImageId: String) -> CropImageTimestamp?,
    onTimestampLoaded: (CropImageTimestamp) -> Unit
) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<CropImage>>(emptyList()) }
    var selectedTimestamp by remember { mutableStateOf<CropImageTimestamp?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        items = withContext(Dispatchers.IO) {
            loadItems()
        }
        isLoading = false
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            items.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No search results")
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    selectedTimestamp?.let { result ->
                        TimestampInfo(
                            result = result,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    message?.let {
                        Text(
                            text = it,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    CropImageGrid(
                        items = items,
                        modifier = Modifier.fillMaxSize(),
                        onItemClick = { cropImageId ->
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    loadTimestamp(cropImageId)
                                }

                                if (result == null) {
                                    message = "Timestamp not found"
                                } else {
                                    message = null
                                    selectedTimestamp = result
                                    onTimestampLoaded(result)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimestampInfo(
    result: CropImageTimestamp,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text("Selected image: ${result.className}")
        Text("Video position: ${result.timestampFileTime} ms")
        Text("Movie: ${result.movieAddress}")
    }
}
