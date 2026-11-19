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
    store: AppStore,
    player: PlayerController,
    onEqualizer: () -> Unit,
    onRescan: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("Settings")

        SettingsCard("Theme") {
            Text("Dark neumorphic. Pick an accent colour:", color = NeuColors.TextDim, fontSize = 13.sp)
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeuChip("Violet", store.accent == 0, { store.updateAccent(0) })
                NeuChip("Blue", store.accent == 1, { store.updateAccent(1) })
            }
        }

        SettingsCard("Audio") {
            Text(
                "Equalizer, bass boost and virtualizer",
                color = AccentColor,
                fontSize = 15.sp,
                modifier = Modifier.plainClickable(onEqualizer).padding(vertical = 4.dp),
            )
        }

        SettingsCard("Playback") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Animated visualizer", color = NeuColors.TextMain, fontSize = 15.sp, modifier = Modifier.weight(1f))
                NeuToggle(store.showVisualizer) { store.updateVisualizer(it) }
            }
            Text(
                "Decorative bars on the Now Playing screen. They move while music plays.",
                color = NeuColors.TextDim,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        SettingsCard("Sleep timer") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 15, 30, 45, 60).forEach { m ->
                    NeuChip(if (m == 0) "Off" else "${m}m", player.sleepMinutes == m, { player.updateSleepTimer(m) })
                }
            }
            if (player.sleepMinutes > 0) {
                Text(
                    "Music will pause after ${player.sleepMinutes} minutes.",
                    color = NeuColors.TextDim,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

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
