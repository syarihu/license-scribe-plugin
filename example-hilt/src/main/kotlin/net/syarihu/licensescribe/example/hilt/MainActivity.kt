package net.syarihu.licensescribe.example.hilt

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import net.syarihu.licensescribe.LicenseProvider
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject
  lateinit var licenseProvider: LicenseProvider

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val licenses = licenseProvider.all
    Log.d(TAG, "LicenseProvider injected successfully!")
    Log.d(TAG, "Total licenses: ${licenses.size}")
    licenses.take(3).forEach { license ->
      Log.d(TAG, "  - ${license.artifactName} (${license.licenseName})")
    }

    setContent {
      MaterialTheme {
        Scaffold(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        ) { innerPadding ->
          MainScreen(
            licenses = licenses,
            modifier = Modifier.padding(innerPadding),
          )
        }
      }
    }
  }

  companion object {
    private const val TAG = "HiltExample"
  }
}
