package com.example.keyframeplayer


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.keyframeplayer.ui.theme.KeyframePlayerTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keyframeplayer.ui.screen.PlayerScreen
import com.example.keyframeplayer.ui.viewmodel.SharedViewModel
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import com.example.keyframeplayer.ui.screen.KeyframeScreen
import com.example.keyframeplayer.data.local.AppDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KeyframePlayerTheme {

                val navController = rememberNavController()
                val sharedViewModel: SharedViewModel = viewModel()

                // ★ DB初期化（ここでOK）
                val context = LocalContext.current

                val db = remember {
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "crop_image_db"
                    ).build()
                }

                NavHost(navController = navController, startDestination = "home") {

                    composable("home") {
                        HomeScreen(navController)
                    }

                    composable("keyframe") {  // ← typo修正
                        KeyframeScreen(navController, sharedViewModel)
                    }

                    composable("player/{timeUs}") { backStackEntry ->
                        val timeUs = backStackEntry.arguments
                            ?.getString("timeUs")?.toLong() ?: 0L

                        PlayerScreen(sharedViewModel, timeUs)
                    }

                    composable("db") {
                        DebugScreen(dao = db.cropImageDao())
                    }
                }
            }
        }
    }
}