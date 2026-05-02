package com.example.keyframeplayer.ui.screen

import android.content.Intent
import android.net.Uri

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import coil.compose.rememberAsyncImagePainter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.example.keyframeplayer.data.local.CropImageDao
import com.example.keyframeplayer.data.local.CropImageEntity

import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height

@Composable
fun DebugInsertScreen(
    dao: CropImageDao
) {
    val context = LocalContext.current

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var className by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var timeReal by remember { mutableStateOf("") }
    var timeFile by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            imageUri = it
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // 📷 画像選択
        Button(onClick = { launcher.launch("image/*") }) {
            Text("画像を選択")
        }

        // 🖼 プレビュー
        imageUri?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .padding(8.dp)
            )
        }

        // 📝 入力欄
        OutlinedTextField(
            value = className,
            onValueChange = { className = it },
            label = { Text("class_name") }
        )

        OutlinedTextField(
            value = score,
            onValueChange = { score = it },
            label = { Text("score (0.0 ~ 1.0)") }
        )

        OutlinedTextField(
            value = color,
            onValueChange = { color = it },
            label = { Text("color") }
        )

        OutlinedTextField(
            value = timeReal,
            onValueChange = { timeReal = it },
            label = { Text("timeStampReal (ms)") }
        )

        OutlinedTextField(
            value = timeFile,
            onValueChange = { timeFile = it },
            label = { Text("timeStampFile (ms)") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 💾 保存ボタン
        Button(onClick = {

            val uri = imageUri ?: return@Button

            val entity = CropImageEntity(
                cropImagePath = uri.toString(),
                className = className,
                score = score.toFloatOrNull() ?: 0f,
                color = color,
                timeStampReal = timeReal.toLongOrNull() ?: 0L,
                timeStampFile = timeFile.toLongOrNull() ?: 0L,
                keyFramePath = null,
                moviePath = null
            )

            CoroutineScope(Dispatchers.IO).launch {
                dao.insert(entity)
            }

        }) {
            Text("DBに追加")
        }
    }
}