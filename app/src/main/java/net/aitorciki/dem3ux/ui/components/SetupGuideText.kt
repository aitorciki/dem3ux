package net.aitorciki.dem3ux.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import net.aitorciki.dem3ux.R

@Composable
internal fun SetupGuideText(
    onOpenSetupGuideClick: () -> Unit,
    onOpenSetupClick: () -> Unit,
) {
    val supportedPrefix = stringResource(R.string.setup_guide_supported_prefix)
    val setupLink = stringResource(R.string.setup_guide_setup_link)
    val supportedSuffix = stringResource(R.string.setup_guide_supported_suffix)
    val manualPrefix = stringResource(R.string.setup_guide_manual_prefix)
    val integrationGuideLink = stringResource(R.string.setup_guide_integration_link)
    val manualSuffix = stringResource(R.string.setup_guide_manual_suffix)

    Text(
        text = stringResource(R.string.setup_guide_intro),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text =
            buildAnnotatedString {
                append(supportedPrefix)
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "setup",
                        styles =
                            TextLinkStyles(
                                style =
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                            ),
                        linkInteractionListener = { onOpenSetupClick() },
                    ),
                ) {
                    append(setupLink)
                }
                append(supportedSuffix)
            },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text =
            buildAnnotatedString {
                append(manualPrefix)
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "guide",
                        styles =
                            TextLinkStyles(
                                style =
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                            ),
                        linkInteractionListener = { onOpenSetupGuideClick() },
                    ),
                ) {
                    append(integrationGuideLink)
                }
                append(manualSuffix)
            },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.setup_guide_empty_playlist_note),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
