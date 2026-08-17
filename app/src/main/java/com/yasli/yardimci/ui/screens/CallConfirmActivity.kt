package com.yasli.yardimci.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import com.yasli.yardimci.ui.theme.YasliTheme

/**
 * Arama onayı ana ekran içinde overlay olarak (CallConfirmScreen) gösterilir.
 * Bu Activity v1'de kullanılmaz; ileride ayrı giriş noktası gerekirse saklanır.
 */
class CallConfirmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YasliTheme {
                Text("Arama onayı")
            }
        }
    }
}
