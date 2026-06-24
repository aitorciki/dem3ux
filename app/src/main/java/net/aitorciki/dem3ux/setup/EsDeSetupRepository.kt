package net.aitorciki.dem3ux.setup

import android.graphics.Bitmap
import android.net.Uri
import net.aitorciki.dem3ux.bridge.PresetBridge

interface EsDeSetupRepository {
    suspend fun persistCustomSystemsFolder(
        uri: Uri,
        grantFlags: Int,
    )

    suspend fun persistedCustomSystemsFolder(): Uri?

    suspend fun readFindRules(treeUri: Uri): String?

    suspend fun saveFindRules(
        treeUri: Uri,
        content: String,
    )

    fun installedPresetTarget(preset: PresetBridge): InstalledPresetTarget?

    fun installedFrontend(): InstalledFrontend?
}

data class InstalledPresetTarget(
    val icon: Bitmap?,
)

data class InstalledFrontend(
    val icon: Bitmap?,
)
