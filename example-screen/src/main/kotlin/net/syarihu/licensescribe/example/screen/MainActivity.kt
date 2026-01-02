package net.syarihu.licensescribe.example.screen

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.core.graphics.toColorInt

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    setContent {
      MaterialTheme {
        MainScreen(
          onClickDefault = {
            OpenSourceLicensesActivity.start(this)
          },
          onClickPurple = {
            OpenSourceLicensesActivity.start(
              context = this,
              themeColor = "#6200EE".toColorInt(),
              toolbarTitleColor = Color.WHITE,
            )
          },
          onClickTeal = {
            OpenSourceLicensesActivity.start(
              context = this,
              themeColor = "#03DAC5".toColorInt(),
              toolbarTitleColor = Color.BLACK,
            )
          },
        )
      }
    }
  }
}
