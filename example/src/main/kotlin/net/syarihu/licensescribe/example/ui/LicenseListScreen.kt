package net.syarihu.licensescribe.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.syarihu.licensescribe.LicenseInfo
import net.syarihu.licensescribe.example.AppLicenses

private val CardBackgroundColor = Color(0xFFFFFFFF)
private val TextPrimaryColor = Color(0xFF212121)
private val TextSecondaryColor = Color(0xFF757575)
private val DefaultAccentColor = Color(0xFF6200EE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseListScreen() {
  val uriHandler = LocalUriHandler.current

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Open Source Licenses") },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(vertical = 8.dp),
    ) {
      items(AppLicenses.all, key = { it.artifactId }) { license ->
        LicenseItem(
          license = license,
          accentColor = DefaultAccentColor,
          onItemClick = {
            license.artifactUrl?.let { url ->
              uriHandler.openUri(url)
            }
          },
          onLicenseClick = {
            license.licenseUrl?.let { url ->
              uriHandler.openUri(url)
            }
          },
        )
      }
    }
  }
}

@Composable
private fun LicenseItem(
  license: LicenseInfo,
  accentColor: Color,
  onItemClick: () -> Unit,
  onLicenseClick: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .clickable(onClick = onItemClick),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    colors = CardDefaults.cardColors(containerColor = CardBackgroundColor),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
    ) {
      Text(
        text = license.artifactName,
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimaryColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      Text(
        text = license.artifactId,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondaryColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      if (license.copyrightHolders.isNotEmpty()) {
        Text(
          text = license.copyrightHolders.joinToString(", "),
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondaryColor,
        )
      }

      Text(
        text = license.licenseName,
        style = MaterialTheme.typography.bodySmall,
        color = accentColor,
        modifier = Modifier
          .padding(top = 8.dp)
          .clickable(
            enabled = license.licenseUrl != null,
            onClick = onLicenseClick,
          )
          .padding(horizontal = 4.dp, vertical = 2.dp),
      )
    }
  }
}

@Preview
@Composable
private fun LicenseListScreenPreview() {
  LicenseListScreen()
}
