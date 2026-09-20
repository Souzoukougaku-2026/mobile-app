package com.example.keyframeplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.keyframeplayer.model.Topic
import com.example.keyframeplayer.ui.movie.MovieRoute
import com.example.keyframeplayer.ui.movie.MovieViewModel
import com.example.keyframeplayer.ui.screen.DirectoryManagementScreen
import com.example.keyframeplayer.ui.theme.KeyFramePlayerTheme
import com.example.keyframeplayer.ui.viewmodel.VideoManagementViewModel
import com.example.keyframeplayer.util.VideoUtils
import java.io.File

class MainActivity : ComponentActivity() {

    // ViewModelの取得（状態とロジックの保持）
    private val viewModel: VideoManagementViewModel by viewModels()
    private val movieViewModel: MovieViewModel by viewModels()

    // 1秒ごとに権限状態をチェックするバックグラウンド処理の設定
    private val mainHandler = Handler(Looper.getMainLooper())
    private val checkStatusRunnable = object : Runnable {
        override fun run() {
            // 1. 永続化されたURIを取得
            val persistedUri = viewModel.selectedUri.value
                ?: contentResolver.persistedUriPermissions.firstOrNull()?.uri

            // 2. アクセス可否をチェック
            val pickedUri = persistedUri?.takeIf { VideoUtils.checkAccess(this@MainActivity, it) }
            viewModel.onDirectoryPicked(pickedUri)
            mainHandler.postDelayed(this, 1000)
        }
    }

    // OSのフォルダ（ディレクトリ）選択画面を開き、選択結果と権限を受け取るシステム
    private val pickDirLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri: Uri? ->
        if (treeUri != null) {
            val contentResolver = contentResolver
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            // 過去に取得した古い永続権限があれば一度解放する
            contentResolver.persistedUriPermissions.forEach {
                contentResolver.releasePersistableUriPermission(it.uri, takeFlags)
            }

            // 新しく選択されたフォルダのアクセス権限を永続化（アプリ再起動後も有効化）
            contentResolver.takePersistableUriPermission(treeUri, takeFlags)
            viewModel.onDirectoryPicked(treeUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
        lifecycleScope.launch {
            // バックグラウンド（IO）で、定義済みのクリア関数を呼び出す
            withContext(Dispatchers.IO) {
                AppDatabase.clearDatabase(this@MainActivity)
            }
        }
         */

        /*
        // 画像が保存されている専用の「images」フォルダを指定する
        val imageDir = File(this.filesDir, "images")

        // フォルダが存在し、かつディレクトリであることを確認する
        if (imageDir.exists() && imageDir.isDirectory) {
            // 3. フォルダ内のすべてのファイルを安全に削除する
            imageDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.delete()
                }
            }
        }
        */

        setContent {
            KeyFramePlayerTheme(dynamicColor = false) {
                val navController = rememberNavController()
                
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavHost(navController = navController, startDestination = "management") {
                        composable("management") {
                            // ViewModelから各状態（State）をリアルタイムに監視
                            val currentUri by viewModel.selectedUri.collectAsState()
                            val isAccessible by viewModel.isAccessible.collectAsState()
                            val videoInfos by viewModel.videoInfos.collectAsState()
                            val isLoading by viewModel.isLoading.collectAsState()

                            DirectoryManagementScreen(
                                currentUri = currentUri?.toString() ?: "None",
                                isAccessible = isAccessible,
                                videoInfos = videoInfos,
                                isLoading = isLoading,
                                onTopicClick = { topic, sessionVideos ->
                                    // 1. セッション全体の動画をセット
                                    movieViewModel.setVideoSession(sessionVideos)
                                    
                                    // 2. サムネイルパスとBBoxをセット
                                    movieViewModel.setSelectedThumbnail(
                                        path = topic.imagePath,
                                        bbox = android.graphics.RectF(topic.bboxLeft, topic.bboxTop, topic.bboxRight, topic.bboxBottom)
                                    )

                                    // 3. タップされたアイテムに関連する絶対時刻（realTime）からシーク位置を計算
                                    val offsetSec = (topic.realTime - movieViewModel.uiState.value.sessionStartTimeMs) / 1000f
                                    movieViewModel.onTimeChanged(offsetSec)

                                    navController.navigate("movie_detail") 
                                },
                                onChooseClick = { pickDirLauncher.launch(null) }
                            )
                        }
                        composable("movie_detail") {
                            MovieRoute(viewModel = movieViewModel)
                        }
                    }
                }
            }
        }
    }

    // アプリが画面に表示されたら、1秒ごとのアクセス権限チェックを開始
    override fun onResume() {
        super.onResume()
        mainHandler.post(checkStatusRunnable)
    }

    // アプリが裏に隠れたら、バッテリー消費を抑えるためにチェックを停止
    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(checkStatusRunnable)
    }
}