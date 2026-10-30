package ru.aokazantsev.devatlas.ui.semantics

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

const val APP_ROOT_TAG = "app_root"

val DarkThemeKey = SemanticsPropertyKey<Boolean>("DarkTheme")
var SemanticsPropertyReceiver.darkTheme by DarkThemeKey

val WallpaperColorsKey = SemanticsPropertyKey<Boolean>("WallpaperColors")
var SemanticsPropertyReceiver.wallpaperColors by WallpaperColorsKey
