package net.aitorciki.dem3ux.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp

@Composable
internal fun SetupGuideText(
    onOpenSetupGuideClick: () -> Unit,
    onOpenSetupClick: () -> Unit,
) {
    Text(
        text = "dem3ux is a bridge and must be configured in your emulator frontend:",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text =
            buildAnnotatedString {
                append("● If your frontend is supported, you can configure it directly from the ")
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
                    append("Setup")
                }
                append(" section.")
            },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text =
            buildAnnotatedString {
                append("● Otherwise, configure the frontend manually following the ")
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
                    append("Integration Guide")
                }
                append(".")
            },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Playlists seen by dem3ux will appear here when first accessed from your frontend.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
