package com.example.a2026souzou

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.a2026souzou.feature.home.presentation.HomeScreen
import com.example.a2026souzou.feature.list.presentation.ListRoute
import com.example.a2026souzou.feature.list.presentation.ListViewModel
import com.example.a2026souzou.feature.movie.presentation.MovieRoute
import com.example.a2026souzou.feature.movie.presentation.MovieViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RootActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "home",
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToMovie = {
                    navController.navigate("movie")
                },
                onNavigateToList = {
                    navController.navigate("list")
                }
            )
        }
        composable("movie") {
            val viewModel: MovieViewModel = hiltViewModel()
            MovieRoute(viewModel = viewModel)
        }
        composable("list") {
            val viewModel: ListViewModel = hiltViewModel()
            ListRoute(viewModel = viewModel)
        }
    }
}
