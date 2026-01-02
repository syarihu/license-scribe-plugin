package net.syarihu.licensescribe.example.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen(
  onClickDefault: () -> Unit,
  onClickPurple: () -> Unit,
  onClickTeal: () -> Unit,
) {
  Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        text = stringResource(R.string.title_license_scribe_screen_example),
        style = MaterialTheme.typography.headlineMedium,
      )

      Spacer(modifier = Modifier.height(32.dp))

      Button(onClick = onClickDefault) {
        Text(text = stringResource(R.string.button_show_licenses_default))
      }

      Spacer(modifier = Modifier.height(16.dp))

      Button(onClick = onClickPurple) {
        Text(text = stringResource(R.string.button_show_licenses_purple))
      }

      Spacer(modifier = Modifier.height(16.dp))

      Button(onClick = onClickTeal) {
        Text(text = stringResource(R.string.button_show_licenses_teal))
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  MaterialTheme {
    MainScreen(
      onClickDefault = {},
      onClickPurple = {},
      onClickTeal = {},
    )
  }
}
