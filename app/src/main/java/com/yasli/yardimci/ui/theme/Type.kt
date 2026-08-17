package com.yasli.yardimci.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Gözü az gören kullanıcı için büyük ölçekli tipografi
val YasliTypography = Typography(
    displayLarge = TextStyle(fontSize = 38.sp, fontWeight = FontWeight.Black),
    headlineMedium = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
)
