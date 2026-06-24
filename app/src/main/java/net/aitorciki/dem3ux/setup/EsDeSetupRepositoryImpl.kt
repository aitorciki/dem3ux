package net.aitorciki.dem3ux.setup

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import net.aitorciki.dem3ux.bridge.PresetBridge

private val KEY_CUSTOM_SYSTEMS_URI = stringPreferencesKey("custom_systems_uri")

private const val PREFERENCES_NAME = "es_de_setup"

private val Context.esDeSetupDataStore by preferencesDataStore(
    name = PREFERENCES_NAME,
    produceMigrations = { context -> listOf(SharedPreferencesMigration(context, PREFERENCES_NAME)) },
)

class EsDeSetupRepositoryImpl(
    private val context: Context,
    private val logger: (String, Throwable?) -> Unit,
) : EsDeSetupRepository {
    override suspend fun persistCustomSystemsFolder(
        uri: Uri,
        grantFlags: Int,
    ) {
        val persistableFlags = grantFlags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        require(persistableFlags != 0) { "No persistable read or write permission returned" }

        context.contentResolver.takePersistableUriPermission(
            uri,
            persistableFlags,
        )
        context.esDeSetupDataStore.edit { preferences -> preferences[KEY_CUSTOM_SYSTEMS_URI] = uri.toString() }
    }

    override suspend fun persistedCustomSystemsFolder(): Uri? {
        val uri =
            context.esDeSetupDataStore.data
                .map { it[KEY_CUSTOM_SYSTEMS_URI] }
                .first() ?: return null
        val parsed = uri.toUri()
        val hasPersistedGrant =
            context.contentResolver.persistedUriPermissions.any { permission ->
                permission.uri == parsed && (permission.isReadPermission || permission.isWritePermission)
            }

        return parsed.takeIf { hasPersistedGrant }
    }

    override fun readFindRules(treeUri: Uri): String? =
        findRulesFile(treeUri)
            ?.uri
            ?.let { uri -> context.contentResolver.openInputStream(uri) }
            ?.bufferedReader()
            ?.use { reader -> reader.readText() }

    override fun saveFindRules(
        treeUri: Uri,
        content: String,
    ) {
        val file = findRulesFile(treeUri) ?: createRulesFile(treeUri)
        requireNotNull(context.contentResolver.openOutputStream(file.uri, "wt")) { "Could not open es_find_rules.xml" }
            .bufferedWriter()
            .use { writer -> writer.write(content) }
    }

    override fun installedPresetTarget(preset: PresetBridge): InstalledPresetTarget? {
        val packageManager = context.packageManager
        val component = preset.resolveTargetComponent { component -> packageManager.hasActivity(component) } ?: return null
        val icon =
            runCatching {
                packageManager.getActivityInfoCompat(component).loadIcon(packageManager).toBitmap(width = 48, height = 48)
            }.onFailure { error -> logger("Failed to load preset target icon", error) }.getOrNull()

        return InstalledPresetTarget(icon = icon)
    }

    override fun installedFrontend(): InstalledFrontend? {
        val packageManager = context.packageManager
        if (!packageManager.hasPackage(ES_DE_PACKAGE_NAME)) {
            return null
        }

        val icon =
            runCatching {
                packageManager.getApplicationIcon(ES_DE_PACKAGE_NAME).toBitmap(width = 48, height = 48)
            }.onFailure { error -> logger("Failed to load frontend icon", error) }.getOrNull()

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

    private companion object {
        const val ES_FIND_RULES_FILE = "es_find_rules.xml"
        const val ES_DE_PACKAGE_NAME = "org.es_de.frontend"
    }
}

// minSdk 26 precludes the API 33 ComponentInfoFlags/ApplicationInfoFlags overloads; keep the int-flags calls.
@Suppress("DEPRECATION")
private fun PackageManager.getActivityInfoCompat(component: android.content.ComponentName) = getActivityInfo(component, 0)

@Suppress("DEPRECATION")
private fun PackageManager.getApplicationInfoCompat(packageName: String) = getApplicationInfo(packageName, 0)
