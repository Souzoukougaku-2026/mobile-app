package com.example.a2026souzou.feature.movie.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun VerticalTable(data: List<Pair<String, String>>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        data.forEach { (key, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = key,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = value,
                    modifier = Modifier.weight(1.5f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            HorizontalDivider(thickness = 0.5.dp, color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f))
        }
    }
}
