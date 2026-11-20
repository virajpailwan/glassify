package com.glassify.music.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassify.music.data.AppStore
import com.glassify.music.playback.PlayerController
import com.glassify.music.ui.neu.AccentColor
import com.glassify.music.ui.neu.NeuChip
import com.glassify.music.ui.neu.NeuColors
import com.glassify.music.ui.neu.NeuToggle
import com.glassify.music.ui.neu.neuRaised
import com.glassify.music.ui.neu.plainClickable

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .neuRaised(24.dp, 6.dp)
            .padding(18.dp),
    ) {
        Text(title, color = NeuColors.TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Column(Modifier.padding(top = 12.dp)) { content() }
    }
}

@Composable
fun SettingsScreen(
    songCount: Int,
    onRescan: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("Settings")

        SettingsCard("Library") {
            Text("$songCount songs found", color = NeuColors.TextDim, fontSize = 13.sp)
            Text(
                "Rescan music",
                color = AccentColor,
                fontSize = 15.sp,
                modifier = Modifier.plainClickable(onRescan).padding(top = 10.dp, bottom = 4.dp),
            )
        }

        SettingsCard("About") {
            Text("Neumo 1.0", color = NeuColors.TextMain, fontSize = 15.sp)
            Text("A local music player for your own files.", color = NeuColors.TextDim, fontSize = 13.sp)
        }
    }
}
