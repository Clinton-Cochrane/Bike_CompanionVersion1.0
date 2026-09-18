package com.clintoncochrane.bikecompanion

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseConfigurationTest {

    @Test
    fun releaseBuild_usesExternalUploadSigningInputs() {
        val appBuild = File("build.gradle").readText()

        listOf(
            "BIKE_COMPANION_UPLOAD_STORE_FILE",
            "BIKE_COMPANION_UPLOAD_STORE_PASSWORD",
            "BIKE_COMPANION_UPLOAD_KEY_ALIAS",
            "BIKE_COMPANION_UPLOAD_KEY_PASSWORD",
        ).forEach { input ->
            assertTrue("Missing release input: $input", appBuild.contains(input))
        }
        assertTrue(appBuild.contains("signingConfig = signingConfigs.release"))
        assertTrue(appBuild.contains("name == \"preReleaseBuild\""))
        assertTrue(appBuild.contains("dependsOn(validateReleaseSigning)"))
    }

    @Test
    fun releaseVersion_comesFromExplicitProjectProperties() {
        val appBuild = File("build.gradle").readText()
        val gradleProperties = File("../gradle.properties").readText()

        assertTrue(appBuild.contains("BIKE_COMPANION_VERSION_CODE"))
        assertTrue(appBuild.contains("BIKE_COMPANION_VERSION_NAME"))
        assertTrue(
            Regex("""(?m)^BIKE_COMPANION_VERSION_CODE=[1-9]\d*$""")
                .containsMatchIn(gradleProperties),
        )
        assertTrue(
            Regex("""(?m)^BIKE_COMPANION_VERSION_NAME=\d+\.\d+\.\d+$""")
                .containsMatchIn(gradleProperties),
        )
        assertFalse(appBuild.contains("versionName = \""))
    }

    @Test
    fun repositoryIgnoresAndroidSigningMaterial() {
        val gitignore = File("../.gitignore").readText()

        assertTrue(gitignore.contains("*.jks"))
        assertTrue(gitignore.contains("*.keystore"))
        assertTrue(gitignore.contains("keystore.properties"))
    }
}
