package com.letify.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.letify.app.ui.components.AccentSwitch
import com.letify.app.ui.components.NoFeedbackButton
import com.letify.app.ui.components.SettingsCard
import com.letify.app.ui.components.SettingsHeader
import com.letify.app.ui.components.SettingsRow
import com.letify.app.ui.components.SettingsRowDivider
import com.letify.app.ui.components.screenHPad
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.theme.Letify
import com.letify.app.ui.theme.LetifyColors

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val state = LocalAppState.current
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxSize().background(Letify.colors.bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 6.dp, bottom = 60.dp),
        ) {
            SettingsHeader(title = "Уведомления", onBack = onBack)

            Box(Modifier.height(6.dp))

            SettingsCard(
                modifier = Modifier.screenHPad(),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                SettingsRow(
                    icon = "sun-bold",
                    iconTile = LetifyColors.TileOrange,
                    title = "Утренняя сводка",
                    showChevron = false,
                    trailing = {
                        AccentSwitch(
                            checked = state.notifyMorning,
                            onCheckedChange = { state.notifyMorning = it },
                        )
                    },
                )
                SettingsRowDivider()
                SettingsRow(
                    icon = "calendar-bold",
                    iconTile = LetifyColors.TileViolet,
                    title = "Привычки и задачи",
                    showChevron = false,
                    trailing = {
                        AccentSwitch(
                            checked = state.notifyHabits,
                            onCheckedChange = { state.notifyHabits = it },
                        )
                    },
                )
                SettingsRowDivider()
                SettingsRow(
                    icon = "waterdrop-bold",
                    iconTile = LetifyColors.TileSky,
                    title = "Напоминания о воде",
                    showChevron = false,
                    trailing = {
                        AccentSwitch(
                            checked = state.notifyWater,
                            onCheckedChange = { state.notifyWater = it },
                        )
                    },
                )
            }
        }
    }
}
