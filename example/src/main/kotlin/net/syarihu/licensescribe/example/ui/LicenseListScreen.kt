package net.syarihu.licensescribe.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.syarihu.licensescribe.LicenseInfo
import net.syarihu.licensescribe.example.AppLicenses

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseListScreen() {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Open Source Licenses") },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      items(AppLicenses.all) { license ->
        LicenseItem(license)
      }
    }
  }
}

@Composable
private fun LicenseItem(license: LicenseInfo) {
  val uriHandler = LocalUriHandler.current

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(
        horizontal = 16.dp,
        vertical = 8.dp,
      )
      .clickable {
        license.artifactUrl?.let { url ->
          uriHandler.openUri(url)
        }
      },
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
    ) {
      Text(
        text = license.artifactName,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = license.artifactId,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (license.copyrightHolders.isNotEmpty()) {
        Text(
          text = license.copyrightHolders.joinToString(", "),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = license.licenseName,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
      )
    }
  }
}

@Preview
@Composable
private fun LicenseListScreenPreview() {
  LicenseListScreen()
}
