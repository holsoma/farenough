package io.github.holsoma.fairenough.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.LocalActivity
import androidx.core.view.WindowCompat

private val Light = lightColorScheme(primary = Color(0xFF006C67), onPrimary = Color.White, primaryContainer = Color(0xFFCBECE7), onPrimaryContainer = Color(0xFF003B38), background = Color(0xFFF6F8FA), surface = Color(0xFFF6F8FA), surfaceContainer = Color(0xFFECF0F2), onSurface = Color(0xFF18242B), onSurfaceVariant = Color(0xFF52616B), outline = Color(0xFF74858C), secondary = Color(0xFF775900), secondaryContainer = Color(0xFFFFE5A5))
private val Dark = darkColorScheme(primary = Color(0xFF78D4CA), onPrimary = Color(0xFF003A35), primaryContainer = Color(0xFF00504B), onPrimaryContainer = Color(0xFFB3F2E9), background = Color(0xFF10191D), surface = Color(0xFF10191D), surfaceContainer = Color(0xFF1D2A2F), onSurface = Color(0xFFE6EFF1), onSurfaceVariant = Color(0xFFB5C7CC), outline = Color(0xFF81979D), secondary = Color(0xFFE8C567), secondaryContainer = Color(0xFF54410A))

@Composable fun FairenoughTheme(appearance: String, content: @Composable () -> Unit) {
    val dark = appearance == "dark" || appearance == "system" && isSystemInDarkTheme()
    val activity = LocalActivity.current
    SideEffect {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = Typography(), content = content)
}
