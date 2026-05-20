package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = EditorialLightPurple,
    onPrimary = EditorialDeepViolet,
    primaryContainer = EditorialDeepViolet,
    onPrimaryContainer = EditorialLavender,
    secondary = EditorialTextGray,
    onSecondary = Color.White,
    background = Color(0xFF121016),
    onBackground = Color(0xFFECE4ED),
    surface = Color(0xFF1D1B20),
    onSurface = Color(0xFFECE4ED),
    surfaceVariant = Color(0xFF262329),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = EditorialDeepViolet,
    onPrimary = Color.White,
    primaryContainer = EditorialLavender,
    onPrimaryContainer = EditorialDeepViolet,
    secondary = EditorialTextGray,
    onSecondary = Color.White,
    secondaryContainer = EditorialLightPurple,
    onSecondaryContainer = EditorialDeepViolet,
    background = EditorialBg,
    onBackground = EditorialTextDark,
    surface = Color.White,
    onSurface = EditorialTextDark,
    surfaceVariant = EditorialBottomBarBg,
    onSurfaceVariant = EditorialTextGray,
    outline = EditorialTextGray,
    outlineVariant = EditorialBorder,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disabling dynamic colors by default so our custom Editorial premium palette shines!
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
