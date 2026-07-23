package com.letify.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.letify.app.ui.LetifyApp
import com.letify.app.ui.icons.SolarIconLoader
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.state.rememberAppState
import com.letify.app.ui.theme.LetifyTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Icon prewarm strategy — the cure for the "icons fade in /
        // navbar indicator lags by 3s on cold launch" symptom.
        //
        // prewarmAll() spawns one worker thread that decodes every SVG
        // in assets/icons, navbar glyphs first. We then block this
        // (main) thread on a CountDownLatch with an 800ms ceiling until
        // those 5 navbar bitmaps are in the snapshot map. The very
        // first frame after setContent therefore already has cached
        // bitmaps for every tab — no AsyncImage state machine, no
        // 3-second icon fade-in.
        //
        // Why a Latch and not `runBlocking { loader.execute(...) }`:
        // Coil's execute dispatches onto Dispatchers.Main.immediate, so
        // calling it via runBlocking from Main deadlocks instantly —
        // the outer runBlocking parks Main, the inner coroutine cannot
        // schedule on Main, splash hangs forever. CountDownLatch is a
        // plain JDK primitive, unrelated to coroutine dispatch, so
        // Coil's worker can still post back to Main and resume itself.
        // LetifyApplication.onCreate also calls prewarmAll, so on a
        // warm process the latch is usually released before we even
        // get here and awaitNavbarReady returns immediately.
        SolarIconLoader.prewarmAll(applicationContext)
        SolarIconLoader.awaitNavbarReady()
        setContent {
            val appState = rememberAppState()
            // Reconcile the launcher icon with the saved choice (e.g. after an
            // app update that reset alias states). No-op if already correct.
            androidx.compose.runtime.LaunchedEffect(appState.appIcon) {
                com.letify.app.ui.applyAppIcon(
                    this@MainActivity,
                    com.letify.app.ui.AppIconVariant.fromKey(appState.appIcon),
                )
            }
            CompositionLocalProvider(LocalAppState provides appState) {
                LetifyTheme(mode = appState.themeMode, accent = appState.accent) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().background(com.letify.app.ui.theme.Letify.colors.bg)) {
                            LetifyApp()
                        }
                    }
                }
            }
        }
    }
}
