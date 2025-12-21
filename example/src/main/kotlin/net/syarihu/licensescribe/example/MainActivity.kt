package net.syarihu.licensescribe.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import net.syarihu.licensescribe.example.ui.LicenseListScreen
import net.syarihu.licensescribe.example.ui.theme.LicenseScribeTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      LicenseScribeTheme {
        LicenseListScreen()
      }
    }
  }
}
