package com.letify.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.letify.app.ui.components.SettingsCard
import com.letify.app.ui.components.SettingsHeader
import com.letify.app.ui.components.SettingsRow
import com.letify.app.ui.components.SettingsRowDivider
import com.letify.app.ui.components.screenHPad
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.theme.Letify
import com.letify.app.ui.theme.LetifyColors

/**
 * Profile → Другое sub-screen. Hosts the secondary / power-user entries
 * that don't belong on the main Profile list — currently Логи and Привязки
 * (moved here from the Profile root since the binding isn't first-priority).
 * Telegram-style: a single SettingsCard with one or more rows.
 */
@Composable
fun OtherScreen(
    onBack: () -> Unit,
    onLogs: () -> Unit,
    onBindings: () -> Unit = {},
) {
    val state = LocalAppState.current
    Box(Modifier.fillMaxSize().background(Letify.colors.bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 6.dp),
        ) {
            SettingsHeader(title = "Другое", onBack = onBack)
            Box(Modifier.height(6.dp))
            SettingsCard(
                modifier = Modifier.screenHPad(),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                SettingsRow(
                    icon = "link-round-bold",
                    iconTile = LetifyColors.TelegramBlue,
                    title = "Привязки",
                    value = state.telegramUser?.let { "Привязан" } ?: "Не привязан",
                    onClick = onBindings,
                )
                SettingsRowDivider()
                SettingsRow(
                    icon = "notebook-bold",
                    iconTile = LetifyColors.TileBlue,
                    title = "Логи",
                    onClick = onLogs,
                )
            }
        }
    }
}
