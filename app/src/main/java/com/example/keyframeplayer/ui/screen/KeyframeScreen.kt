package com.example.keyframeplayer.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.keyframeplayer.KeyframeItem
import com.example.keyframeplayer.util.getKeyframeItems
import com.example.keyframeplayer.util.getKeyframeTimes
import com.example.keyframeplayer.ui.viewmodel.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun KeyframeScreen(
    navController: NavController,
    sharedViewModel: SharedViewModel
) {
    val context = LocalContext.current

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var keyframeItems by remember { mutableStateOf<List<KeyframeItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(selectedUri) {
        val uri = selectedUri ?: return@LaunchedEffect

        isLoading = true

        keyframeItems = withContext(Dispatchers.IO) {
            val times = getKeyframeTimes(context, uri)
            getKeyframeItems(context, uri, times)
        }

        isLoading = false
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
            sharedViewModel.setUri(it)
            selectedUri = it
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Button(onClick = { launcher.launch(arrayOf("video/*")) }) {
            Text("動画選択")
        }

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(3)) {
                items(keyframeItems) { item ->
                    Image(
                        bitmap = item.bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(120.dp)
                    )
                }
            }
        }
    }
}