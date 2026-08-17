package com.yasli.yardimci.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Yesil,
    onPrimary = CardWhite,
    secondary = Mavi,
    tertiary = Turuncu,
    background = Paper,
    onBackground = Ink,
    surface = CardWhite,
    onSurface = Ink,
    error = Kirmizi,
    onError = CardWhite,
)

@Composable
fun YasliTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = YasliTypography,
        content = content
    )
}
