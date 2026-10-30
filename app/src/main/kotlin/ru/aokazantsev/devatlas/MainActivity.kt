package ru.aokazantsev.devatlas

import android.app.UiModeManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import ru.aokazantsev.devatlas.app.DevAtlasApp
import ru.aokazantsev.devatlas.settings.domain.model.ThemeMode

class MainActivity : ComponentActivity() {

    private var contentReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val application = application as DevAtlasApplication
        installSplashScreen().setKeepOnScreenCondition { application.holdsSystemSplash && !contentReady }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = application.graph
        setContent {
            DevAtlasApp(
                graph = graph,
                onThemeResolved = ::applyTheme,
                onContentReady = { contentReady = true },
            )
        }
    }

    private fun applyTheme(mode: ThemeMode, darkTheme: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(UiModeManager::class.java).setApplicationNightMode(mode.toNightMode())
        }
        val style = if (darkTheme) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    private fun ThemeMode.toNightMode(): Int = when (this) {
        ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
        ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
        ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
    }
}
