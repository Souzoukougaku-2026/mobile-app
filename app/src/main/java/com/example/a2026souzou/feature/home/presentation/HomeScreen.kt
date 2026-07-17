package com.example.a2026souzou.feature.home.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onNavigateToMovie: () -> Unit,
    onNavigateToList: () -> Unit
) {
    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "2026 Souzou App",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onNavigateToMovie,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(56.dp)
                ) {
                    Text("ムービー画面へ (Go to Movie)")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onNavigateToList,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(56.dp)
                ) {
                    Text("リスト画面へ (Go to List)")
                }
            }
        }
    }
}
