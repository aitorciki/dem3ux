package net.aitorciki.dem3ux.ui.screens.help

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.aitorciki.dem3ux.ui.components.SetupGuideText
import net.aitorciki.dem3ux.ui.preview.PreviewScreenFrame
import net.aitorciki.dem3ux.ui.theme.Dem3uxTheme

@Composable
internal fun HelpContent(
    onOpenSetupGuideClick: () -> Unit,
    onOpenSetupClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Setup guide",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                SetupGuideText(
                    onOpenSetupGuideClick = onOpenSetupGuideClick,
                    onOpenSetupClick = onOpenSetupClick,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Help")
@Composable
private fun HelpContentPreview() {
    Dem3uxTheme {
        PreviewScreenFrame {
            HelpContent(
                onOpenSetupGuideClick = {},
                onOpenSetupClick = {},
            )
        }
    }
}
