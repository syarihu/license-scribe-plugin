package net.syarihu.licensescribe.example.hilt

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.syarihu.licensescribe.LicenseInfo

@Composable
fun MainScreen(
  licenses: List<LicenseInfo>,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    Text(
      text = "Hilt DI Example",
      style = MaterialTheme.typography.headlineMedium,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "Total licenses: ${licenses.size}",
      style = MaterialTheme.typography.bodyLarge,
    )
    Spacer(modifier = Modifier.height(16.dp))
    LazyColumn {
      items(licenses) { license ->
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        ) {
          Text(
            text = license.artifactName,
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = license.licenseName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  MaterialTheme {
    MainScreen(
      licenses = listOf(
        LicenseInfo(
          artifactId = "com.example:sample:1.0.0",
          artifactName = "Sample Library",
          artifactUrl = null,
          copyrightHolders = emptyList(),
          licenseName = "Apache License 2.0",
          licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        ),
        LicenseInfo(
          artifactId = "org.sample:library:2.0.0",
          artifactName = "Sample Library 2",
          artifactUrl = null,
          copyrightHolders = emptyList(),
          licenseName = "MIT License",
          licenseUrl = "https://opensource.org/licenses/MIT",
        ),
      ),
    )
  }
}
