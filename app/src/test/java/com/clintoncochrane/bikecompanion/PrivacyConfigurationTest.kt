package com.clintoncochrane.bikecompanion

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyConfigurationTest {

    @Test
    fun manifest_disablesBackupAndUnusedNetworkAccess() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains("android:allowBackup=\"false\""))
        assertTrue(manifest.contains("android:fullBackupContent=\"@xml/backup_rules\""))
        assertTrue(manifest.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\""))
        assertFalse(manifest.contains("android.permission.INTERNET"))
    }

    @Test
    fun backupRules_excludeEveryStorageDomain() {
        val legacyRules = File("src/main/res/xml/backup_rules.xml").readText()
        val modernRules = File("src/main/res/xml/data_extraction_rules.xml").readText()
        val domains = listOf(
            "root",
            "file",
            "database",
            "sharedpref",
            "external",
            "device_root",
            "device_file",
            "device_database",
            "device_sharedpref",
        )

        domains.forEach { domain ->
            assertTrue(legacyRules.contains("<exclude domain=\"$domain\" path=\".\""))
            assertTrue(modernRules.countMatches("<exclude domain=\"$domain\" path=\".\"") == 2)
        }
    }

    @Test
    fun manifest_exposesPrivacyPolicyToHealthConnect() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains(".ui.privacy.PrivacyPolicyActivity"))
        assertTrue(manifest.contains("androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"))
        assertTrue(manifest.contains("android.intent.action.VIEW_PERMISSION_USAGE"))
        assertTrue(manifest.contains("android.intent.category.HEALTH_PERMISSIONS"))
        assertTrue(manifest.contains("android.permission.START_VIEW_PERMISSION_USAGE"))
    }

    private fun String.countMatches(value: String): Int = windowed(value.length).count { it == value }
}
