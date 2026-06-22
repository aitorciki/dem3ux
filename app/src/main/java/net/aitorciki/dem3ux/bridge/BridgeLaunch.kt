package net.aitorciki.dem3ux.bridge

import android.content.ComponentName

data class BridgeLaunch(
    val inputPath: String,
    val targetComponents: List<ComponentName>,
    val targetAction: String? = null,
    val embeddedExtraReplacement: EmbeddedExtraPattern? = null,
    val requestedFolderAccess: Boolean = false,
)
