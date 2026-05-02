package com.example.keyframeplayer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Button(onClick = {
            navController.navigate("keyframe")
        }) {
            Text("キーフレーム抽出")
        }

        Button(onClick = {
            navController.navigate("db")
        }) {
            Text("DBデバッグ")
        }

        Button(onClick = {
            navController.navigate("insert")
        }) {
            Text("入力フォームへ")
        }
    }
}