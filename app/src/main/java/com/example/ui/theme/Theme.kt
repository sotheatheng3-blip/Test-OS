package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val BentoColorScheme =
  lightColorScheme(
    primary = BentoPurpleAccent,
    secondary = BentoPurpleBg,
    tertiary = BentoPinkContainer,
    background = BentoBg,
    surface = BentoWhiteSurface,
    onBackground = BentoTextDark,
    onSurface = BentoTextDark,
    surfaceVariant = BentoGreySurface,
    onSurfaceVariant = BentoTextSecondary,
    primaryContainer = BentoPurpleBg,
    onPrimaryContainer = BentoTextDeep,
    secondaryContainer = BentoBlueContainer,
    onSecondaryContainer = BentoTextDeep
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Let Bento Theme use the bright vibrant light palette
  dynamicColor: Boolean = false, // Preserve original Bento palette colors
  content: @Composable () -> Unit,
) {
  val colorScheme = BentoColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
