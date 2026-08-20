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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.keyframeplayer.feature.list.presentation.ListViewModel
import com.example.keyframeplayer.feature.movie.presentation.MovieRoute
import com.example.keyframeplayer.feature.movie.presentation.MovieViewModel
import com.example.keyframeplayer.ui.screen.DirectoryManagementScreen
import com.example.keyframeplayer.ui.screen.ListUpScreen
import com.example.keyframeplayer.ui.theme.KeyFramePlayerTheme
import com.example.keyframeplayer.ui.viewmodel.VideoManagementViewModel
import com.example.keyframeplayer.util.VideoUtils
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // ViewModelの取得（状態とロジックの保持）
    private val viewModel: VideoManagementViewModel by viewModels()

    // 1秒ごとに権限状態をチェックするバックグラウンド処理の設定
    private val mainHandler = Handler(Looper.getMainLooper())
    private val checkStatusRunnable = object : Runnable {
        override fun run() {
            // 1. 永続化されたURIを取得
            val persistedUri = viewModel.selectedUri.value
                ?: contentResolver.persistedUriPermissions.filter { android.provider.DocumentsContract.isTreeUri(it.uri) }.firstOrNull()?.uri

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

        setContent {
            KeyFramePlayerTheme(dynamicColor = false) {
                // ViewModelから各状態（State）をリアルタイムに監視
                val currentUri by viewModel.selectedUri.collectAsState()
                val isAccessible by viewModel.isAccessible.collectAsState()
                val videoInfos by viewModel.videoInfos.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // 💡 修正：videoInfos を画面コンポーザブルへそのまま引き渡す
                    // （※画面側である DirectoryManagementScreen の引数名も videoInfos への変更が必要です）
                    /*DirectoryManagementScreen(
                        currentUri = currentUri?.toString() ?: "None",
                        isAccessible = isAccessible,
                        videoInfos = videoInfos,
                        isLoading = isLoading,
                        onChooseClick = { pickDirLauncher.launch(null) }
                    )*/

                    AppNavigation(
                        videoViewModel = viewModel,  //videoViewModel,
                        onChooseClick = { pickDirLauncher.launch(null) }
                    )
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

@Composable
fun AppNavigation(
    videoViewModel: VideoManagementViewModel,
    onChooseClick: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "directory_management",
    ) {
        composable("directory_management") {
            val currentUri by videoViewModel.selectedUri.collectAsState()
            val isAccessible by videoViewModel.isAccessible.collectAsState()
            val videoInfos by videoViewModel.videoInfos.collectAsState()
            val isLoading by videoViewModel.isLoading.collectAsState()

            DirectoryManagementScreen(
                currentUri = currentUri?.toString() ?: "None",
                isAccessible = isAccessible,
                videoInfos = videoInfos,
                isLoading = isLoading,
                onChooseClick = onChooseClick,
                onProcessingFinished = {
                    navController.navigate("list") {
                        popUpTo("directory_management") { inclusive = true }
                    }
                }
            )
        }
        composable("list") {
            val listViewModel: ListViewModel = hiltViewModel()
            val uiState by listViewModel.uiState.collectAsStateWithLifecycle()
            ListUpScreen(
                items = uiState.items,
                onSortByDate = { isAsc -> listViewModel.setSortOrder(isAsc) },
                onNavigateToDetail = { image ->
                    navController.navigate("movie")
                } // ここを修正しました
            )
        }
        composable("movie") {
            val movieViewModel: MovieViewModel = hiltViewModel()
            MovieRoute(viewModel = movieViewModel)
        }
        /*composable("list") {
            val viewModel: ListViewModel = hiltViewModel()
            ListRoute(viewModel = viewModel)
        }*/
    }
}