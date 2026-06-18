package net.aitorciki.dem3ux.setup

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import androidx.documentfile.provider.DocumentFile
import net.aitorciki.dem3ux.bridge.PresetBridge

class EsDeSetupRepository(
    private val context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun persistCustomSystemsFolder(
        uri: Uri,
        grantFlags: Int,
    ) {
        val persistableFlags = grantFlags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        require(persistableFlags != 0) { "No persistable read or write permission returned" }

        context.contentResolver.takePersistableUriPermission(
            uri,
            persistableFlags,
        )
        preferences.edit { putString(KEY_CUSTOM_SYSTEMS_URI, uri.toString()) }
    }

    fun persistedCustomSystemsFolder(): Uri? {
        val uri = preferences.getString(KEY_CUSTOM_SYSTEMS_URI, null)?.let(Uri::parse) ?: return null
        val hasPersistedGrant =
            context.contentResolver.persistedUriPermissions.any { permission ->
                permission.uri == uri && (permission.isReadPermission || permission.isWritePermission)
            }

        return uri.takeIf { hasPersistedGrant }
    }

    fun readFindRules(treeUri: Uri): String? =
        findRulesFile(treeUri)
            ?.uri
            ?.let { uri -> context.contentResolver.openInputStream(uri) }
            ?.bufferedReader()
            ?.use { reader -> reader.readText() }

    fun saveFindRules(
        treeUri: Uri,
        content: String,
    ) {
        val file = findRulesFile(treeUri) ?: createRulesFile(treeUri)
        requireNotNull(context.contentResolver.openOutputStream(file.uri, "wt")) { "Could not open es_find_rules.xml" }
            .bufferedWriter()
            .use { writer -> writer.write(content) }
    }

    fun installedPresetTarget(preset: PresetBridge): InstalledPresetTarget? {
        val packageManager = context.packageManager
        val component = preset.resolveTargetComponent { component -> packageManager.hasActivity(component) } ?: return null
        val icon =
            runCatching {
                packageManager.getActivityInfoCompat(component).loadIcon(packageManager).toBitmap(width = 48, height = 48)
            }.getOrNull()

        return InstalledPresetTarget(icon = icon)
    }

    fun installedFrontend(): InstalledFrontend? {
        val packageManager = context.packageManager
        if (!packageManager.hasPackage(ES_DE_PACKAGE_NAME)) {
            return null
        }

        val icon =
            runCatching {
                packageManager.getApplicationIcon(ES_DE_PACKAGE_NAME).toBitmap(width = 48, height = 48)
            }.getOrNull()

        return InstalledFrontend(icon = icon)
    }

    private fun findRulesFile(treeUri: Uri): DocumentFile? = customSystemsFolder(treeUri).findFile(ES_FIND_RULES_FILE)

    private fun createRulesFile(treeUri: Uri): DocumentFile =
        requireNotNull(customSystemsFolder(treeUri).createFile("text/xml", ES_FIND_RULES_FILE)) {
            "Could not create es_find_rules.xml"
        }

    private fun customSystemsFolder(treeUri: Uri): DocumentFile =
        requireNotNull(DocumentFile.fromTreeUri(context, treeUri)) { "Could not open ES-DE custom_systems folder" }

    private fun PackageManager.hasActivity(component: android.content.ComponentName): Boolean =
        runCatching {
            getActivityInfoCompat(component)
            true
        }.getOrDefault(false)

    private fun PackageManager.hasPackage(packageName: String): Boolean =
        runCatching {
            getApplicationInfoCompat(packageName)
            true
        }.getOrDefault(false)
}

data class InstalledPresetTarget(
    val icon: Bitmap?,
)

data class InstalledFrontend(
    val icon: Bitmap?,
)

private const val ES_FIND_RULES_FILE = "es_find_rules.xml"
private const val ES_DE_PACKAGE_NAME = "org.es_de.frontend"
private const val PREFERENCES_NAME = "es_de_setup"
private const val KEY_CUSTOM_SYSTEMS_URI = "custom_systems_uri"

@Suppress("DEPRECATION")
private fun PackageManager.getActivityInfoCompat(component: android.content.ComponentName) = getActivityInfo(component, 0)

@Suppress("DEPRECATION")
private fun PackageManager.getApplicationInfoCompat(packageName: String) = getApplicationInfo(packageName, 0)
