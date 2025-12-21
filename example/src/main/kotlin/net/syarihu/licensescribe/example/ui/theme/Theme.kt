package net.syarihu.licensescribe.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme()

@Composable
fun LicenseScribeTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = LightColorScheme,
    content = content,
  )
}
